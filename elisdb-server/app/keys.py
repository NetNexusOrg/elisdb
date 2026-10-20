"""Manage server-issued authentication keys.

Usage:
    python -m app.keys issue USER_NAME [--server-url URL] [--output PATH]

``issue`` generates an ed25519 keypair, registers the public key in the
allowlist with the user name as comment, and writes a configuration file
(JSON) with the server URL, the user name and the private seed for import
into the app.
The server URL is required and comes from ``--server-url`` or the
``ELISDB_SERVER_URL`` environment variable.

Without arguments the CLI opens an interactive menu to create (issue),
list and remove trusted authentications, driven by numbered options.
"""

import base64
import json
import os
import sys
import time
import uuid

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import (
    Encoding,
    PrivateFormat,
    PublicFormat,
    NoEncryption,
)

from .database import Database


def _database() -> Database:
    return Database(os.environ.get("ELISDB_DB", "elisdb.db"))


def generate() -> tuple[str, str]:
    """Generate an ed25519 keypair.

    Returns (private_seed_b64, public_key_b64). The private seed is the
    32-byte raw ed25519 key the app expects as ``ELISDB_PRIVATE_KEY``
    (``BuildConfig.SIGNER_PRIVATE_KEY_B64``, decoded by ``Signer.fromBase64``).
    Used internally by ``issue``, which registers the public key in the
    allowlist.
    """
    private = Ed25519PrivateKey.generate()
    seed = private.private_bytes(Encoding.Raw, PrivateFormat.Raw, NoEncryption())
    public = private.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    return base64.b64encode(seed).decode("ascii"), base64.b64encode(public).decode("ascii")


def issue(user_name: str) -> tuple[str, str, str]:
    """Server-issue a keypair for a named user.

    Generates a keypair, registers the public key with the user name as the
    allowlist comment, and returns (private_seed_b64, public_key_b64, key_id).
    The operator hands the private seed to the app for import.
    """
    seed_b64, public_b64 = generate()
    key_id = f"issue-{uuid.uuid4().hex[:12]}"
    return seed_b64, public_b64, key_id


def _usage() -> None:
    print(
        "usage: python -m app.keys issue <user-name> "
        "[--server-url <url>] [--output <path>]",
        file=sys.stderr,
    )


def issue_and_register(
    db: Database, user: str, server_url: str, output: str | None, provisioned: bool = False
) -> int:
    """Run the server-issue flow (shared by the argument and interactive modes)."""
    seed_b64, public_b64, key_id = issue(user)
    output_path = output or f"{user}.config.json"
    config = {"server_url": server_url, "private_seed": seed_b64, "user_name": user}
    # The config carries the private seed, so write it owner-only (0600) and
    # re-chmod in case the file already existed with looser permissions.
    fd = os.open(output_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2)
        f.write("\n")
    os.chmod(output_path, 0o600)
    db.add_key(key_id, public_b64, comment=user, provisioned=provisioned)
    print(f"issued key for '{user}' (id {key_id})")
    print(f"configuration written to: {output_path}")
    print("private seed (import into app): " + seed_b64)
    print("registered public key:          " + public_b64)
    return 0


def _run_args(args: list[str], db: Database) -> int:
    """Handle argument-based invocation; only ``issue`` is accepted."""
    if not args:
        _usage()
        return 2
    command = args[0]
    if command != "issue":
        print(f"unknown command: {command}", file=sys.stderr)
        return 2

    server_url = None
    output = None
    user_args: list[str] = []
    i = 1
    while i < len(args):
        arg = args[i]
        if arg == "--server-url":
            if i + 1 >= len(args):
                print("error: --server-url requires a value", file=sys.stderr)
                return 2
            i += 1
            server_url = args[i].strip()
        elif arg == "--output":
            if i + 1 >= len(args):
                print("error: --output requires a value", file=sys.stderr)
                return 2
            i += 1
            output = args[i].strip()
        elif arg.startswith("--"):
            print(f"unknown option: {arg}", file=sys.stderr)
            return 2
        else:
            user_args.append(arg)
        i += 1

    if not user_args:
        _usage()
        return 2
    user = user_args[0].strip()
    if not user:
        print("error: a user name is required to issue a key", file=sys.stderr)
        return 2

    server_url = server_url or os.environ.get("ELISDB_SERVER_URL", "").strip()
    if not server_url:
        print(
            "error: a server URL is required "
            "(--server-url <url> or ELISDB_SERVER_URL)",
            file=sys.stderr,
        )
        return 2

    return issue_and_register(db, user, server_url, output)


# ---------------------------------------------------------------------------
# interactive mode
# ---------------------------------------------------------------------------


def _prompt(prompt: str, default: str = "") -> str:
    """Read a line with input(), trimmed, returning ``default`` on empty input."""
    full_prompt = f"{prompt} [default: {default}]" if default else prompt
    value = input(full_prompt + " ").strip()
    if value == "" and default:
        return default
    return value


def _print_key(row: dict) -> None:
    created = time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime(row.get("created_at") or 0))
    print(f"{row['id']} | {row.get('comment') or ''} | {row['public_key']} | {created}")


def _list_keys(db: Database) -> int:
    keys = db.list_keys()
    if not keys:
        print("nenhuma chave confiada")
        return 0
    print(f"{len(keys)} chave(s) confiada(s):")
    for k in keys:
        _print_key(k)
    return 0


def _create_interactive(db: Database) -> int:
    env_url = os.environ.get("ELISDB_SERVER_URL", "").strip()
    server_url = _prompt("URL do servidor", default=env_url)
    if not server_url:
        print(
            "error: a server URL is required "
            "(--server-url <url> or ELISDB_SERVER_URL)",
            file=sys.stderr,
        )
        return 2
    user = _prompt("Nome do utilizador")
    if not user:
        print("error: a user name is required to issue a key", file=sys.stderr)
        return 2
    return issue_and_register(db, user, server_url, output=None)


def _remove_interactive(db: Database) -> int:
    _list_keys(db)
    key_id = _prompt("Id da chave a remover").strip()
    if not key_id:
        print("error: a key id is required to remove a key", file=sys.stderr)
        return 2
    target = next((k for k in db.list_keys() if k["id"] == key_id), None)
    if target is None:
        print(f"error: no key with id '{key_id}'", file=sys.stderr)
        return 2
    print("\nChave a remover:")
    _print_key(target)
    confirm = _prompt("Remover esta chave? (s/N)")
    if confirm.lower() not in ("s", "sim", "y", "yes"):
        print("operação cancelada")
        return 0
    db.remove_key(key_id)
    print(f"key '{key_id}' removed")
    return 0


def _run_interactive(db: Database) -> int:
    """Present a numbered menu until the operator exits."""
    options = {
        "1": _create_interactive,
        "2": _list_keys,
        "3": _remove_interactive,
    }
    while True:
        print("\nGestão de autenticações")
        print("1) Criar autenticação")
        print("2) Listar autenticações")
        print("3) Remover autenticação")
        print("4) Sair")
        choice = input("Escolha uma opção: ").strip()
        if choice == "4":
            return 0
        action = options.get(choice)
        if action is None:
            print("opção inválida — escolha 1, 2, 3 ou 4")
            continue
        action(db)


def main(argv: list[str] | None = None) -> int:
    args = list(argv if argv is not None else sys.argv[1:])
    db = _database()
    if not args:
        return _run_interactive(db)
    return _run_args(args, db)


if __name__ == "__main__":
    sys.exit(main())