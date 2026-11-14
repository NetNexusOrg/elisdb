"""Consistent backup and restore for the ElisDB SQLite database and photos.

The database snapshot uses SQLite's online backup API, so it is safe while the
server is writing. The manifest records a SHA-256 for the database and for every
referenced photo; restore verifies all of them before replacing live data.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sqlite3
import sys
import tempfile
import time
from pathlib import Path

MANIFEST_NAME = "manifest.json"
DB_NAME = "elisdb.db"
PHOTOS_DIR = "photos"
AUTO_PREFIX = "auto-"


class BackupError(Exception):
    """Raised when a backup is incomplete, corrupt, or cannot be restored."""


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _integrity(conn: sqlite3.Connection) -> None:
    row = conn.execute("PRAGMA integrity_check").fetchone()
    if row is None or row[0] != "ok":
        raise BackupError(f"database integrity check failed: {row[0] if row else 'no result'}")


def create_backup(db_path: str, photos_dir: str, out_dir: str) -> Path:
    """Create a consistent backup directory and return it."""
    source = Path(db_path)
    if not source.is_file():
        raise BackupError(f"database not found: {db_path}")
    out = Path(out_dir)
    if out.exists() and any(out.iterdir()):
        raise BackupError(f"backup directory is not empty: {out_dir}")
    (out / PHOTOS_DIR).mkdir(parents=True, exist_ok=True)
    target_db = out / DB_NAME

    src = sqlite3.connect(str(source))
    dst = sqlite3.connect(str(target_db))
    try:
        src.backup(dst)
        dst.commit()
    finally:
        dst.close()
        src.close()

    conn = sqlite3.connect(str(target_db))
    conn.row_factory = sqlite3.Row
    try:
        _integrity(conn)
        schema_version = conn.execute("PRAGMA user_version").fetchone()[0]
        rows = conn.execute(
            "SELECT id, mime, caminho, size_bytes FROM fotos ORDER BY id"
        ).fetchall()
    finally:
        conn.close()

    photos = []
    for row in rows:
        path = Path(row["caminho"])
        if not path.is_file():
            raise BackupError(f"photo file missing for {row['id']}: {row['caminho']}")
        stored_name = f"{row['id']}{path.suffix}"
        shutil.copy2(path, out / PHOTOS_DIR / stored_name)
        photos.append(
            {
                "id": row["id"],
                "mime": row["mime"],
                "original_path": row["caminho"],
                "stored_name": stored_name,
                "size": path.stat().st_size,
                "sha256": _sha256_file(path),
            }
        )

    manifest = {
        "created_at": int(time.time()),
        "schema_version": schema_version,
        "db_sha256": _sha256_file(target_db),
        "db_size": target_db.stat().st_size,
        "photos": photos,
    }
    (out / MANIFEST_NAME).write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return out


def verify_backup(backup_dir: str) -> dict:
    """Verify a backup's checksums, integrity and photo coverage."""
    root = Path(backup_dir)
    manifest_path = root / MANIFEST_NAME
    db_path = root / DB_NAME
    if not manifest_path.is_file() or not db_path.is_file():
        raise BackupError("backup is missing manifest.json or elisdb.db")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise BackupError(f"invalid backup manifest: {exc}") from exc
    if manifest.get("db_sha256") != _sha256_file(db_path):
        raise BackupError("database checksum mismatch")
    if manifest.get("db_size") != db_path.stat().st_size:
        raise BackupError("database size mismatch")

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    try:
        _integrity(conn)
        db_photo_ids = {
            row["id"] for row in conn.execute("SELECT id FROM fotos").fetchall()
        }
    finally:
        conn.close()

    manifest_ids = set()
    for photo in manifest.get("photos", []):
        photo_id = photo.get("id")
        if not photo_id:
            raise BackupError("photo entry has no id")
        manifest_ids.add(photo_id)
        path = root / PHOTOS_DIR / photo.get("stored_name", "")
        if not path.is_file():
            raise BackupError(f"backup photo missing: {photo_id}")
        if path.stat().st_size != photo.get("size"):
            raise BackupError(f"backup photo size mismatch: {photo_id}")
        if _sha256_file(path) != photo.get("sha256"):
            raise BackupError(f"backup photo checksum mismatch: {photo_id}")
    if db_photo_ids != manifest_ids:
        raise BackupError("backup photo manifest does not match database rows")
    return manifest


def restore_backup(backup_dir: str, db_path: str, photos_dir: str) -> None:
    """Verify a backup and atomically replace the live database and photos.

    The previous files are kept until both replacements succeed; on failure the
    original database and photo directory are restored.
    """
    manifest = verify_backup(backup_dir)
    root = Path(backup_dir)
    db_target = Path(db_path)
    photos_target = Path(photos_dir)
    parent = db_target.parent
    parent.mkdir(parents=True, exist_ok=True)

    staging = Path(tempfile.mkdtemp(prefix=".elisdb-restore-", dir=str(parent)))
    staged_db = staging / DB_NAME
    staged_photos = staging / PHOTOS_DIR
    staged_photos.mkdir()
    try:
        shutil.copy2(root / DB_NAME, staged_db)
        conn = sqlite3.connect(str(staged_db))
        try:
            for photo in manifest.get("photos", []):
                source = root / PHOTOS_DIR / photo["stored_name"]
                destination = staged_photos / photo["stored_name"]
                shutil.copy2(source, destination)
                conn.execute(
                    "UPDATE fotos SET caminho = ? WHERE id = ?",
                    (str(photos_target / photo["stored_name"]), photo["id"]),
                )
            conn.commit()
            _integrity(conn)
        finally:
            conn.close()

        db_backup = staging / "old.db"
        photos_backup = staging / "old-photos"
        had_db = db_target.exists()
        had_photos = photos_target.exists()
        if had_db:
            os.replace(db_target, db_backup)
        if had_photos:
            os.replace(photos_target, photos_backup)
        try:
            os.replace(staged_db, db_target)
            os.replace(staged_photos, photos_target)
        except BaseException:
            if db_target.exists():
                db_target.unlink()
            if photos_target.exists():
                shutil.rmtree(photos_target)
            if had_db and db_backup.exists():
                os.replace(db_backup, db_target)
            if had_photos and photos_backup.exists():
                os.replace(photos_backup, photos_target)
            raise
        for suffix in ("-wal", "-shm"):
            sidecar = Path(str(db_target) + suffix)
            if sidecar.exists():
                sidecar.unlink()
    finally:
        shutil.rmtree(staging, ignore_errors=True)


def backup_name(now: float | None = None) -> str:
    """Return the directory name used for an automatic backup."""
    stamp = time.gmtime(time.time() if now is None else now)
    return f"{AUTO_PREFIX}{time.strftime('%Y%m%d-%H%M%S', stamp)}"


def list_backups(backup_dir: str | os.PathLike[str]) -> list[Path]:
    """Return every valid backup under ``backup_dir``, oldest first."""
    root = Path(backup_dir)
    if not root.is_dir():
        return []
    found = [child for child in root.iterdir() if (child / MANIFEST_NAME).is_file()]
    return sorted(found, key=lambda path: path.name)


def summarize_backups(backup_dir: str | os.PathLike[str]) -> dict[str, object]:
    """Describe the automatic backups for status screens."""
    automatic = [
        path
        for path in list_backups(backup_dir)
        if path.name.startswith(AUTO_PREFIX)
    ]
    newest = automatic[-1] if automatic else None
    age = None
    if newest is not None:
        age = max(int(time.time() - newest.stat().st_mtime), 0)
    return {
        "dir": str(backup_dir),
        "count": len(automatic),
        "newest_auto": newest.name if newest else None,
        "newest_auto_age_seconds": age,
    }


def prune_backups(
    backup_dir: str | os.PathLike[str], keep: int, *, prefix: str = AUTO_PREFIX
) -> list[Path]:
    """Delete the oldest ``prefix`` backups, keeping ``keep`` of them.

    ``keep <= 0`` disables pruning: a scheduled run never deletes every backup.
    """
    if keep < 0:
        raise BackupError("retention cannot be negative")
    if keep == 0:
        return []
    automatic = [
        path for path in list_backups(backup_dir) if path.name.startswith(prefix)
    ]
    removed: list[Path] = []
    for path in automatic[: max(len(automatic) - keep, 0)]:
        shutil.rmtree(path)
        removed.append(path)
    return removed


def estimate_backup_bytes(
    db_path: str | os.PathLike[str], photos_dir: str | os.PathLike[str]
) -> int:
    """Estimate how much space a full backup needs (database plus photos)."""
    database = Path(db_path)
    total = database.stat().st_size if database.is_file() else 0
    root = Path(photos_dir)
    if root.is_dir():
        for entry in root.iterdir():
            if entry.is_file():
                total += entry.stat().st_size
    return total


def free_space_bytes(path: str | os.PathLike[str]) -> int:
    """Return the free bytes available on the filesystem that holds ``path``."""
    return shutil.disk_usage(path).free


def _default_backup_dir() -> str:
    """Backup location used when no flag is given, matching the scheduler."""
    configured = os.environ.get("ELISDB_BACKUP_DIR")
    if configured:
        return configured
    db_path = os.environ.get("ELISDB_DB", "elisdb.db")
    return str(Path(db_path).resolve().parent / "backups")


def _main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="ElisDB backup/restore")
    sub = parser.add_subparsers(dest="command", required=True)
    create = sub.add_parser("create")
    create.add_argument("out_dir")
    create.add_argument("--db", default=os.environ.get("ELISDB_DB", "elisdb.db"))
    create.add_argument(
        "--photos", default=os.environ.get("ELISDB_PHOTOS", "photos")
    )
    verify = sub.add_parser("verify")
    verify.add_argument("backup_dir")
    restore = sub.add_parser("restore")
    restore.add_argument("backup_dir")
    restore.add_argument("--db", default=os.environ.get("ELISDB_DB", "elisdb.db"))
    restore.add_argument(
        "--photos", default=os.environ.get("ELISDB_PHOTOS", "photos")
    )
    listing = sub.add_parser("list")
    listing.add_argument("--dir", default=_default_backup_dir())
    prune = sub.add_parser("prune")
    prune.add_argument("--dir", default=_default_backup_dir())
    prune.add_argument("--keep", type=int, default=3)
    args = parser.parse_args(argv)
    try:
        if args.command == "create":
            create_backup(args.db, args.photos, args.out_dir)
            print(f"backup created: {args.out_dir}")
        elif args.command == "verify":
            manifest = verify_backup(args.backup_dir)
            print(f"backup ok: {len(manifest.get('photos', []))} photos")
        elif args.command == "restore":
            restore_backup(args.backup_dir, args.db, args.photos)
            print(f"backup restored: {args.backup_dir}")
        elif args.command == "list":
            for path in list_backups(args.dir):
                print(path.name)
        else:
            removed = prune_backups(args.dir, args.keep)
            for path in removed:
                print(f"backup removed: {path.name}")
            print(f"backup kept: {len(list_backups(args.dir))}")
    except BackupError as exc:
        print(f"backup error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(_main(sys.argv[1:]))
