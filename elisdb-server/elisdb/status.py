"""Read-only snapshot of the server state for the management TUI.

Everything here is observation: no migrations are run, the database is opened
with ``mode=ro`` and no file is created or modified. ``start.py`` renders the
result and the tests assert on it directly.
"""

from __future__ import annotations

import json
import os
import sqlite3
import subprocess
import urllib.error
import urllib.request
from pathlib import Path

from .backup import summarize_backups

CONTAINER_NAME = "elisdb-server"
PID_FILE = Path(".run") / "server.pid"
DEFAULT_PORT = 8000


def server_root() -> Path:
    """Repository directory that contains compose.yaml and elisdb/."""
    return Path(__file__).resolve().parent.parent


def _setting(name: str, default: str) -> str:
    value = os.environ.get(name, "").strip()
    return value or default


def data_dir(root: Path | None = None) -> Path:
    """Host data directory (the one bind-mounted at /app/data)."""
    root = root or server_root()
    raw = Path(_setting("ELISDB_DATA_DIR", "elisdb-data"))
    return raw if raw.is_absolute() else root / raw


def configs_dir(root: Path | None = None) -> Path:
    return (root or server_root()) / "configs"


def port() -> int:
    try:
        return int(_setting("ELISDB_PORT", str(DEFAULT_PORT)))
    except ValueError:
        return DEFAULT_PORT


def _docker_running() -> bool:
    try:
        out = subprocess.run(
            [
                "docker",
                "inspect",
                "--format",
                "{{.State.Running}}",
                CONTAINER_NAME,
            ],
            capture_output=True,
            text=True,
            timeout=10,
            check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return False
    return out.returncode == 0 and out.stdout.strip() == "true"


def _local_pid(root: Path) -> int | None:
    pid_file = root / PID_FILE
    try:
        pid = int(pid_file.read_text(encoding="utf-8").strip())
    except (OSError, ValueError):
        return None
    try:
        os.kill(pid, 0)
    except OSError:
        return None
    return pid


def active_mode(root: Path | None = None) -> str:
    """``docker``, ``local`` or ``stopped``; ``both`` is a broken state."""
    root = root or server_root()
    docker = _docker_running()
    local = _local_pid(root) is not None
    if docker and local:
        return "both"
    if docker:
        return "docker"
    if local:
        return "local"
    return "stopped"


def _dir_size(path: Path) -> int:
    total = 0
    if not path.exists():
        return 0
    for entry in os.scandir(path):
        try:
            if entry.is_file(follow_symlinks=False):
                total += entry.stat(follow_symlinks=False).st_size
            elif entry.is_dir(follow_symlinks=False):
                total += _dir_size(Path(entry.path))
        except OSError:
            continue
    return total


def _counts(db_path: Path) -> dict[str, int]:
    empty = {"clients": 0, "photos": 0, "tombstones": 0, "keys_active": 0, "keys_total": 0}
    if not db_path.is_file():
        return empty
    try:
        conn = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
    except sqlite3.Error:
        return empty
    try:
        def count(table: str) -> int:
            try:
                return conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            except sqlite3.Error:
                return 0

        active = 0
        total = 0
        try:
            total = conn.execute("SELECT COUNT(*) FROM keys").fetchone()[0]
            active = conn.execute(
                "SELECT COUNT(*) FROM keys WHERE revoked_at IS NULL"
            ).fetchone()[0]
        except sqlite3.Error:
            pass
        return {
            "clients": count("clientes"),
            "photos": count("fotos"),
            "tombstones": count("tombstones"),
            "keys_active": active,
            "keys_total": total,
        }
    finally:
        conn.close()


def health() -> dict | None:
    try:
        with urllib.request.urlopen(
            f"http://127.0.0.1:{port()}/health", timeout=2
        ) as response:
            return json.loads(response.read().decode("utf-8"))
    except (OSError, ValueError, urllib.error.URLError):
        return None


def collect(root: Path | None = None) -> dict:
    """Return the full status snapshot rendered by the TUI."""
    root = root or server_root()
    data = data_dir(root)
    db_path = data / "elisdb.db"
    try:
        stat = data.stat()
        owner = f"{stat.st_uid}:{stat.st_gid}"
    except OSError:
        owner = None
    backup_root = Path(os.environ.get("ELISDB_BACKUP_DIR") or (data / "backups"))
    snapshot = {
        "root": str(root),
        "mode": active_mode(root),
        "data_dir": str(data),
        "configs_dir": str(configs_dir(root)),
        "owner": owner,
        "writable": os.access(data, os.W_OK),
        "port": port(),
        "sizes": {
            "db": db_path.stat().st_size if db_path.is_file() else 0,
            "photos": _dir_size(data / "photos"),
            "backups": _dir_size(data / "backups"),
            "apk": _dir_size(data / "apk"),
        },
        "counts": _counts(db_path),
        "backups": summarize_backups(backup_root),
        "health": health(),
    }
    return snapshot
