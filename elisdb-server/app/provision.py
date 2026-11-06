"""Provision trusted keys automatically at container startup.

Reads ``ELISDB_USERS`` (comma-separated user names), ``ELISDB_SERVER_URL`` and
``ELISDB_CONFIGS_DIR`` (default ``<dirname(ELISDB_DB)>/configs``) from the
environment and issues a key for every listed user that does not already hold a
trusted key, writing each configuration file ready for import into the app.
Provisioning is idempotent: users that already have a key are skipped.
"""

import os
import sys
from urllib.parse import urlparse

from .database import Database
from .keys import issue_and_register, safe_config_path


def validate_server_url(server_url: str) -> None:
    """Reject a missing, placeholder, or non-HTTP server URL.

    Provisioning would otherwise issue keys pointing at an address the phones
    cannot reach (or an operator forgot to edit), and the failure would only
    appear later on the device.
    """
    if not server_url:
        raise ValueError("ELISDB_SERVER_URL is required to provision users")
    if "<" in server_url or ">" in server_url:
        raise ValueError(
            "ELISDB_SERVER_URL still contains a placeholder; set the real URL"
        )
    parsed = urlparse(server_url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise ValueError(
            "ELISDB_SERVER_URL must be an absolute http:// or https:// URL"
        )


def provision_users(
    db: Database,
    users: list[str],
    server_url: str,
    configs_dir: str,
) -> tuple[list[str], list[str]]:
    """Issue a key for each listed user that lacks one and revoke removed users.

    Skips blank names and users that already hold a trusted key (matched by the
    allowlist comment). Provisioned keys are marked so they can be revoked when
    their user leaves `ELISDB_USERS`; manually issued keys are never touched.

    Returns (provisioned, revoked): the users newly provisioned and the users
    whose auto-provisioned key was revoked because they left the list.
    """
    existing = {k.get("comment") for k in db.list_keys()}
    provisioned: list[str] = []
    revoked: list[str] = []
    os.makedirs(configs_dir, exist_ok=True)

    desired: set[str] = set()
    for user in users:
        user = user.strip()
        if not user:
            continue
        try:
            output = safe_config_path(configs_dir, user)
        except ValueError as exc:
            print(f"error: skipping user {user!r}: {exc}", file=sys.stderr)
            continue
        desired.add(user)
        if user in existing:
            continue
        issue_and_register(db, user, server_url, output=output, provisioned=True)
        provisioned.append(user)

    for key in db.list_keys():
        comment = key.get("comment") or ""
        if not key.get("provisioned"):
            continue
        if comment in desired:
            continue
        db.remove_key(key["id"])
        try:
            config_path = safe_config_path(configs_dir, comment)
        except ValueError as exc:
            print(
                f"warning: not deleting config for {comment!r}: {exc}",
                file=sys.stderr,
            )
            config_path = None
        if config_path is not None:
            try:
                os.remove(config_path)
            except FileNotFoundError:
                pass
        revoked.append(comment)

    return provisioned, revoked


def main() -> int:
    users = [u for u in os.environ.get("ELISDB_USERS", "").split(",") if u.strip()]
    server_url = os.environ.get("ELISDB_SERVER_URL", "").strip()
    if users:
        try:
            validate_server_url(server_url)
        except ValueError as exc:
            print(f"error: {exc}", file=sys.stderr)
            return 2

    db_dir = os.path.dirname(os.path.abspath(os.environ.get("ELISDB_DB", "elisdb.db")))
    configs_dir = os.environ.get("ELISDB_CONFIGS_DIR") or os.path.join(db_dir, "configs")
    os.makedirs(configs_dir, exist_ok=True)

    db = Database(os.environ.get("ELISDB_DB", "elisdb.db"))
    provisioned, revoked = provision_users(db, users, server_url, configs_dir)
    for user in provisioned:
        print(f"provisioned key for '{user}'")
    for user in revoked:
        print(f"revoked key for '{user}'")
    return 0


if __name__ == "__main__":
    sys.exit(main())