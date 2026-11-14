#!/usr/bin/env python3
"""Small terminal menu to manage the ElisDB server (stdlib only).

Run from `elisdb-server/`:

    python start.py                 # numbered menu
    python start.py --status        # same snapshot as the Status option
    python start.py --list-users
    python start.py --add-user NOME [--server-url URL] [--yes]
    python start.py --revoke <key-id|nome> [--yes]

The TUI only orchestrates what an operator would type: `docker compose`
commands or a local uvicorn process. It never runs sudo, never deletes data and
never applies a configuration change without showing the diff and asking.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

from elisdb import status

ROOT = Path(__file__).resolve().parent
ENV_FILE = ROOT / ".env"
ENV_EXAMPLE = ROOT / ".env.example"
RUN_DIR = ROOT / ".run"
PID_FILE = RUN_DIR / "server.pid"

BOOL_KEYS = {"ELISDB_TRUST_PROXY"}
POSITIVE_INT_KEYS = {
    "ELISDB_MAX_PUSH_RECORDS",
    "ELISDB_MAX_PUSH_BYTES",
    "ELISDB_MAX_PHOTOS_PER_CLIENT",
    "ELISDB_MAX_PHOTO_BYTES",
    "ELISDB_MAX_PHOTO_BYTES_PER_KEY",
    "ELISDB_MAX_PHOTO_BYTES_TOTAL",
    "ELISDB_RATE_LIMIT_REQUESTS",
    "ELISDB_RATE_LIMIT_WINDOW_SECONDS",
    "ELISDB_PHOTO_GC_INTERVAL_SECONDS",
    "PUID",
    "PGID",
    "ELISDB_PORT",
}
NON_NEGATIVE_INT_KEYS = {
    "ELISDB_TOMBSTONE_RETENTION_DAYS",
    "ELISDB_AUDIT_RETENTION_DAYS",
}
LOG_LEVELS = {"DEBUG", "INFO", "WARNING", "ERROR"}
_KEY_LINE = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$")


# ---------------------------------------------------------------------------
# .env handling
# ---------------------------------------------------------------------------


def read_env(path: Path = ENV_FILE) -> dict[str, str]:
    """Parse KEY=VALUE lines, ignoring comments and blank lines."""
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        match = _KEY_LINE.match(stripped)
        if match:
            values[match.group(1)] = match.group(2).strip().strip('"').strip("'")
    return values


def validate_env(values: dict[str, str]) -> list[str]:
    """Return a list of human-readable problems (empty means valid)."""
    problems: list[str] = []
    for key in POSITIVE_INT_KEYS:
        if key in values and values[key] != "":
            try:
                if int(values[key]) <= 0:
                    problems.append(f"{key} must be a positive integer")
            except ValueError:
                problems.append(f"{key} must be a positive integer")
    for key in NON_NEGATIVE_INT_KEYS:
        if key in values and values[key] != "":
            try:
                if int(values[key]) < 0:
                    problems.append(f"{key} must be zero or a positive integer")
            except ValueError:
                problems.append(f"{key} must be zero or a positive integer")
    for key in BOOL_KEYS:
        if key in values and values[key].lower() not in {"true", "false"}:
            problems.append(f"{key} must be true or false")
    if values.get("ELISDB_LOG_LEVEL"):
        level = values["ELISDB_LOG_LEVEL"].upper()
        if level not in LOG_LEVELS:
            problems.append(
                "ELISDB_LOG_LEVEL must be one of " + ", ".join(sorted(LOG_LEVELS))
            )
    url = values.get("ELISDB_SERVER_URL", "")
    if url:
        if "<" in url or ">" in url:
            problems.append("ELISDB_SERVER_URL still contains a placeholder")
        elif not re.match(r"^https?://[^/\s]+", url):
            problems.append("ELISDB_SERVER_URL must be an absolute http(s) URL")
    return problems


def write_env(updates: dict[str, str], path: Path = ENV_FILE) -> Path | None:
    """Update keys in place (comments preserved), atomically and owner-only.

    Returns the path of the backup, or None when there was no previous file.
    Raises ValueError if validation fails.
    """
    current = read_env(path)
    candidate = {**current, **updates}
    problems = validate_env(candidate)
    if problems:
        raise ValueError("; ".join(problems))

    if path.is_file():
        backup: Path | None = path.with_name(f"{path.name}.bak-{int(time.time())}")
        backup.write_bytes(path.read_bytes())
        os.chmod(backup, 0o600)
    else:
        backup = None

    lines = path.read_text(encoding="utf-8").splitlines() if path.is_file() else []
    seen: set[str] = set()
    out: list[str] = []
    for line in lines:
        match = _KEY_LINE.match(line.strip())
        if match and match.group(1) in updates:
            key = match.group(1)
            seen.add(key)
            out.append(f"{key}={updates[key]}")
        else:
            out.append(line)
    for key, value in updates.items():
        if key not in seen:
            out.append(f"{key}={value}")

    tmp = path.with_name(f".{path.name}.tmp")
    tmp.write_text("\n".join(out) + "\n", encoding="utf-8")
    os.chmod(tmp, 0o600)
    os.replace(tmp, path)
    return backup


# ---------------------------------------------------------------------------
# process helpers
# ---------------------------------------------------------------------------


def _compose(*args: str) -> list[str]:
    return ["docker", "compose", *args]


def _run(
    cmd: list[str],
    *,
    check: bool = False,
    capture: bool = False,
    env: dict[str, str] | None = None,
) -> subprocess.CompletedProcess:
    return subprocess.run(
        cmd,
        cwd=ROOT,
        check=check,
        text=True,
        capture_output=capture,
        env=env,
    )


def _server_env() -> dict[str, str]:
    """Environment for a local server/CLI process, mirroring the container."""
    values = read_env()
    data = Path(values.get("ELISDB_DATA_DIR", "elisdb-data"))
    if not data.is_absolute():
        data = ROOT / data
    env = os.environ.copy()
    env.update(
        {
            "ELISDB_DB": str(data / "elisdb.db"),
            "ELISDB_PHOTOS": str(data / "photos"),
            "ELISDB_CONFIGS_DIR": str(ROOT / "configs"),
            "ELISDB_APK_DIR": str(data / "apk" / "latest"),
        }
    )
    if values.get("ELISDB_SERVER_URL"):
        env["ELISDB_SERVER_URL"] = values["ELISDB_SERVER_URL"]
    return env


def _wait_healthy(timeout: float = 15.0) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if status.health():
            return True
        time.sleep(0.5)
    return False


def _pid_is_server(pid: int) -> bool:
    """True only when `pid` is alive and looks like our uvicorn process."""
    proc = Path(f"/proc/{pid}/cmdline")
    if proc.exists():
        try:
            return b"uvicorn" in proc.read_bytes()
        except OSError:
            return False
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    return True  # non-Linux: pid reuse is possible, assume alive


def _local_pid() -> int | None:
    try:
        pid = int(PID_FILE.read_text(encoding="utf-8").strip())
    except (OSError, ValueError):
        return None
    if not _pid_is_server(pid):
        PID_FILE.unlink(missing_ok=True)
        return None
    return pid


def _current_mode() -> str:
    return status.active_mode(ROOT)


# ---------------------------------------------------------------------------
# status
# ---------------------------------------------------------------------------


def print_status() -> None:
    state = status.collect(ROOT)
    counts = state["counts"]
    sizes = state["sizes"]

    print(f"Modo ativo     {state['mode']}")
    print(
        f"Dados          {state['data_dir']} · dono {state['owner'] or '?'} · "
        f"{'gravável' if state['writable'] else 'NÃO gravável'}"
    )
    print(
        f"               BD {human_size(sizes['db'])} · fotos {human_size(sizes['photos'])} · "
        f"backups {human_size(sizes['backups'])} · APK {human_size(sizes['apk'])}"
    )
    print(
        f"Conteúdo       {counts['clients']} clientes · {counts['photos']} fotos · "
        f"{counts['tombstones']} tombstones"
    )
    print(f"Chaves         {counts['keys_active']} ativas / {counts['keys_total']} total")
    backups = state["backups"]
    newest = backups["newest_auto"]
    if newest is None:
        detail = "sem backups automáticos"
    else:
        age = backups["newest_auto_age_seconds"] or 0
        if age >= 48 * 3600:
            when = f"há {age // 86400} dias"
        elif age >= 3600:
            when = f"há {age // 3600} h"
        else:
            when = "há menos de 1 h"
        detail = f"{newest} ({when})"
    print(
        f"Backups        {backups['count']} automáticos em {backups['dir']} · {detail}"
    )
    health = state["health"]
    if health:
        print(f"Servidor       HTTP ok (versão {health.get('version', '?')}, porta {state['port']})")
    else:
        print(f"Servidor       sem resposta na porta {state['port']}")
    if state["mode"] == "stopped" and counts["keys_total"] == 0:
        print("Sugestão       não há chaves; usa Utilizadores -> Adicionar")


def human_size(value: int | None) -> str:
    if not value:
        return "0 B"
    size = float(value)
    for unit in ("B", "KB", "MB", "GB"):
        if size < 1024 or unit == "GB":
            return f"{size:.0f} {unit}"
        size /= 1024
    return f"{size:.0f} GB"


# ---------------------------------------------------------------------------
# start / stop
# ---------------------------------------------------------------------------


def _python() -> str:
    """Interpreter for local runs: ELISDB_PYTHON, .venv, then the current one."""
    override = os.environ.get("ELISDB_PYTHON")
    if override:
        return override
    candidates = (
        ROOT / ".venv" / "bin" / "python",
        ROOT / ".venv" / "Scripts" / "python.exe",
    )
    for candidate in candidates:
        if candidate.exists():
            return str(candidate)
    return sys.executable


def _install_hint(python: str) -> None:
    print(f"error: {python} não tem o uvicorn instalado.")
    print("       Cria um ambiente local (recomendado, com uv):")
    print("         uv venv .venv && uv pip install -r requirements.txt")
    print("       Ou com o venv da stdlib:")
    print("         python3 -m venv .venv && .venv/bin/pip install -r requirements.txt")
    print("       Depois volta a correr o menu.")
    print("       Em produção usa o modo Docker (Iniciar -> Docker).")


def start_local() -> int:
    mode = _current_mode()
    if mode in {"docker", "both"}:
        print("error: o contentor Docker está a correr; para-o antes de arrancar localmente.")
        return 1
    if mode == "local":
        print(f"O servidor local já está a correr (pid {_local_pid()}).")
        return 0

    data = Path(_server_env()["ELISDB_DB"]).parent
    configs = ROOT / "configs"
    # Local runs may create their own directories: the process runs as the host
    # user, so anything it creates is owned correctly (Docker cannot do this).
    for directory in (data, configs):
        directory.mkdir(parents=True, exist_ok=True)
    if not os.access(data, os.W_OK) or not os.access(configs, os.W_OK):
        uid, gid = os.getuid(), os.getgid()
        print(f"error: sem permissão de escrita; corre: sudo chown -R {uid}:{gid} {data} {configs}")
        return 1

    python = _python()
    probe = subprocess.run([python, "-c", "import uvicorn"], capture_output=True, text=True)
    if probe.returncode != 0:
        _install_hint(python)
        return 1

    RUN_DIR.mkdir(exist_ok=True)
    port = status.port()
    log = RUN_DIR / "server.log"
    with log.open("ab") as handle:
        process = subprocess.Popen(
            [
                python,
                "-m",
                "uvicorn",
                "elisdb.main:app",
                "--host",
                "127.0.0.1",
                "--port",
                str(port),
            ],
            cwd=ROOT,
            env=_server_env(),
            stdout=handle,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    PID_FILE.write_text(str(process.pid), encoding="utf-8")
    if _wait_healthy():
        print(f"Servidor local a correr (pid {process.pid}) em http://127.0.0.1:{port}")
        return 0
    print(f"error: sem resposta em 15 s; vê o log: {log}")
    return 1


def start_docker() -> int:
    mode = _current_mode()
    if mode in {"local", "both"}:
        print("error: há um servidor local a correr; para-o antes de usar Docker.")
        return 1
    if mode == "docker":
        print("O contentor já está a correr.")
        return 0

    uid, gid = os.getuid(), os.getgid()
    data = Path(read_env().get("ELISDB_DATA_DIR", "elisdb-data"))
    if not data.is_absolute():
        data = ROOT / data
    for directory in (data, ROOT / "configs"):
        if directory.exists() and directory.stat().st_uid != uid:
            print(
                f"error: {directory} pertence a outro uid; corrige com:\n"
                f"  sudo chown -R {uid}:{gid} {directory}"
            )
            return 1
        directory.mkdir(exist_ok=True)

    result = _run(_compose("up", "-d", "--build"))
    if result.returncode != 0:
        return result.returncode
    if _wait_healthy():
        print("Contentor a correr e saudável.")
        return 0
    print("error: contentor iniciado mas /health não responde; vê: docker compose logs")
    return 1


def stop_server() -> int:
    mode = _current_mode()
    if mode == "stopped":
        print("Nada a parar.")
        return 0
    if mode in {"docker", "both"}:
        _run(_compose("stop"))
    pid = _local_pid()
    if pid:
        try:
            os.kill(pid, signal.SIGTERM)
        except OSError:
            pass
        for _ in range(20):
            if _local_pid() is None:
                break
            time.sleep(0.25)
        PID_FILE.unlink(missing_ok=True)
    print("Servidor parado.")
    return 0


# ---------------------------------------------------------------------------
# users
# ---------------------------------------------------------------------------


def _keys_command(*args: str) -> list[str]:
    if _current_mode() in {"docker", "both"}:
        return _compose("exec", "-T", status.CONTAINER_NAME, "python", "-m", "elisdb.keys", *args)
    return [_python(), "-m", "elisdb.keys", *args]


def _keys_env() -> dict[str, str] | None:
    """Docker exec inherits the container env; local runs need ours."""
    return None if _current_mode() in {"docker", "both"} else _server_env()


def _keys_run(*args: str, capture: bool = False) -> subprocess.CompletedProcess:
    return _run(_keys_command(*args), capture=capture, env=_keys_env())


def _config_file_for(user: str) -> Path | None:
    """Most recent config file for `user`, ignoring archived ones in _revogados/."""
    candidates = [
        path
        for path in (ROOT / "configs").rglob(f"{user}.config.json")
        if "_revogados" not in path.parts
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda path: path.stat().st_mtime)


def list_users(as_json: bool = False) -> int:
    result = _keys_run("list", "--json", capture=True)
    if result.returncode != 0:
        return result.returncode
    try:
        keys = json.loads(result.stdout or "[]")
    except json.JSONDecodeError:
        print("error: resposta inesperada de `elisdb.keys list --json`")
        return 1
    if as_json:
        print(json.dumps(keys, indent=2))
        return 0

    by_user: dict[str, list[dict]] = {}
    for key in keys:
        by_user.setdefault(key.get("comment") or "(sem nome)", []).append(key)
    if not by_user:
        print("Nenhuma chave. Usa Utilizadores -> Adicionar.")
        return 0
    for user in sorted(by_user):
        entries = by_user[user]
        active = [k for k in entries if not k.get("revoked_at")]
        file_state = "ficheiro ok" if _config_file_for(user) else "sem ficheiro"
        print(f"{user}: {len(active)} ativa(s) / {len(entries)} · {file_state}")
        for key in entries:
            state = "ativa" if not key.get("revoked_at") else "revogada"
            created = time.strftime("%Y-%m-%d", time.gmtime(key.get("created_at") or 0))
            print(f"  {key['id']}  {state}  {created}")
    return 0


def _config_path_for(user: str) -> Path:
    return ROOT / "configs" / f"{user}.config.json"


def add_user(user: str, server_url: str | None = None) -> int:
    existing = _config_path_for(user)
    if existing.exists():
        suggestion = _config_path_for(f"{user}-2")
        print(f"error: {existing} já existe; escolhe outro nome (ex.: {suggestion.name})")
        return 1
    url = server_url or read_env().get("ELISDB_SERVER_URL", "")
    if not url:
        print("error: define ELISDB_SERVER_URL no .env ou passa --server-url")
        return 1
    args = ["issue", user, "--server-url", url]
    result = _keys_run(*args, capture=True)
    if result.stdout:
        print(result.stdout.strip())
    if result.stderr:
        print(result.stderr.strip(), file=sys.stderr)
    return result.returncode


def revoke(target: str, assume_yes: bool = False) -> int:
    result = _keys_run("list", "--json", capture=True)
    try:
        keys = json.loads(result.stdout or "[]")
    except json.JSONDecodeError:
        keys = []
    active = [k for k in keys if not k.get("revoked_at")]
    if not active:
        print("Nenhuma chave ativa.")
        return 0
    match = [k for k in active if k["id"] == target] or [
        k for k in active if (k.get("comment") or "") == target
    ]
    if not match:
        print(f"error: nenhuma chave ativa para {target!r}")
        return 1
    if len(match) == len(active) and len(active) == 1 and not assume_yes:
        print("error: é a única chave ativa; escreve 'revogar ultima' para confirmar.")
        return 1
    code = 0
    for key in match:
        if not assume_yes:
            answer = input(f"Revogar {key['id']} ({key.get('comment')})? (s/N) ").strip().lower()
            if answer not in {"s", "sim", "y", "yes"}:
                continue
        result = _keys_run("revoke", "--id", key["id"], capture=True)
        print((result.stdout or result.stderr).strip())
        code = code or result.returncode
    return code


# ---------------------------------------------------------------------------
# configuration
# ---------------------------------------------------------------------------


EDITABLE_GROUPS: list[tuple[str, list[str]]] = [
    ("Utilizadores e URL", ["ELISDB_SERVER_URL"]),
    ("Rede", ["ELISDB_BIND_ADDRESS", "ELISDB_TRUST_PROXY", "ELISDB_PORT"]),
    (
        "Limites e quotas",
        [
            "ELISDB_MAX_PUSH_RECORDS",
            "ELISDB_MAX_PUSH_BYTES",
            "ELISDB_MAX_PHOTOS_PER_CLIENT",
            "ELISDB_MAX_PHOTO_BYTES",
            "ELISDB_MAX_PHOTO_BYTES_PER_KEY",
            "ELISDB_MAX_PHOTO_BYTES_TOTAL",
            "ELISDB_RATE_LIMIT_REQUESTS",
            "ELISDB_RATE_LIMIT_WINDOW_SECONDS",
        ],
    ),
    (
        "Retenção e manutenção",
        [
            "ELISDB_TOMBSTONE_RETENTION_DAYS",
            "ELISDB_AUDIT_RETENTION_DAYS",
            "ELISDB_PHOTO_GC_INTERVAL_SECONDS",
        ],
    ),
    ("Caminhos e dono", ["ELISDB_DATA_DIR", "PUID", "PGID"]),
    ("Log", ["ELISDB_LOG_LEVEL"]),
]


def show_config() -> None:
    values = read_env()
    print(f"Ficheiro: {ENV_FILE} ({'existe' if ENV_FILE.is_file() else 'não existe; usa .env.example'})")
    for title, keys in EDITABLE_GROUPS:
        print(f"\n{title}")
        for key in keys:
            origin = ".env" if key in values else "default"
            print(f"  {key}={values.get(key, '')}  [{origin}]")


def edit_group(title: str, keys: list[str]) -> int:
    values = read_env()
    updates: dict[str, str] = {}
    for key in keys:
        current = values.get(key, "")
        answer = input(f"{key} [{current}]: ").strip()
        if answer and answer != current:
            updates[key] = answer
    if not updates:
        print("Sem alterações.")
        return 0
    previous_mode = _current_mode()
    try:
        backup = write_env(updates)
    except ValueError as exc:
        print(f"error: {exc}")
        return 1
    for key, value in updates.items():
        print(f"  {key}: {values.get(key, '')!r} -> {value!r}")
    if backup:
        print(f"Backup: {backup}")
    answer = input("Aplicar agora (reiniciar o modo ativo)? (s/N) ").strip().lower()
    if answer not in {"s", "sim", "y", "yes"}:
        print("Gravado. Reinicia quando quiseres.")
        return 0
    stop_server()
    code = start_docker() if previous_mode == "docker" else start_local()
    if code != 0:
        print(f"error: o reinício falhou; podes repor o .env a partir do backup.")
    return code


# ---------------------------------------------------------------------------
# menu
# ---------------------------------------------------------------------------


def menu() -> int:
    while True:
        print(
            "\nElisDB server\n"
            "  1) Status\n"
            "  2) Iniciar\n"
            "  3) Parar\n"
            "  4) Utilizadores\n"
            "  5) Configuração\n"
            "  0) Sair"
        )
        choice = input("> ").strip()
        if choice == "1":
            print()
            print_status()
        elif choice == "2":
            print("\n  1) Local\n  2) Docker")
            mode = input("> ").strip()
            if mode == "1":
                start_local()
            elif mode == "2":
                start_docker()
        elif choice == "3":
            stop_server()
        elif choice == "4":
            users_menu()
        elif choice == "5":
            config_menu()
        elif choice in {"0", "q", ""}:
            return 0


def users_menu() -> None:
    while True:
        print(
            "\nUtilizadores\n"
            "  1) Listar\n"
            "  2) Adicionar\n"
            "  3) Revogar\n"
            "  0) Voltar"
        )
        choice = input("> ").strip()
        if choice == "1":
            list_users()
        elif choice == "2":
            user = input("Nome do utilizador: ").strip()
            if user:
                add_user(user)
        elif choice == "3":
            target = input("ID da chave ou nome: ").strip()
            if target:
                revoke(target)
        elif choice in {"0", "q", ""}:
            return


def config_menu() -> None:
    while True:
        print("\nConfiguração")
        for index, (title, _) in enumerate(EDITABLE_GROUPS, start=1):
            print(f"  {index}) {title}")
        print("  7) Ver valores efetivos\n  0) Voltar")
        choice = input("> ").strip()
        if choice == "7":
            show_config()
        elif choice.isdigit() and 1 <= int(choice) <= len(EDITABLE_GROUPS):
            title, keys = EDITABLE_GROUPS[int(choice) - 1]
            edit_group(title, keys)
        elif choice in {"0", "q", ""}:
            return


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="ElisDB server manager (TUI)")
    parser.add_argument("--status", action="store_true", help="print the status snapshot")
    parser.add_argument("--list-users", action="store_true")
    parser.add_argument("--add-user", metavar="NOME")
    parser.add_argument("--revoke", metavar="ID|NOME")
    parser.add_argument("--server-url")
    parser.add_argument("--yes", action="store_true", help="skip confirmations")
    args = parser.parse_args(argv)

    if args.status:
        print_status()
        return 0
    if args.list_users:
        return list_users()
    if args.add_user:
        return add_user(args.add_user, args.server_url)
    if args.revoke:
        return revoke(args.revoke, assume_yes=args.yes)
    return menu()


if __name__ == "__main__":
    sys.exit(main())
