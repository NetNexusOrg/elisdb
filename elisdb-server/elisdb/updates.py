"""Discovery of the Android package published for in-app updates.

The operator publishes a build by copying ``elisdb-<versionName>-<versionCode>.apk``
into the release directory (``/app/data/apk/latest`` by default). The newest
version code wins, other file names in that directory are ignored, and removing
the package retires the release.
"""

from __future__ import annotations

import hashlib
import logging
import os
import re
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger("elisdb.server.updates")

APK_MEDIA_TYPE = "application/vnd.android.package-archive"

# Generous ceiling: a release APK is a few megabytes; anything bigger is a
# mistake (or an attempt to exhaust the server) and is not offered.
MAX_APK_BYTES = 200 * 1024 * 1024

_NAME = re.compile(r"^elisdb-(?P<name>[0-9][0-9A-Za-z.+-]*)-(?P<code>[0-9]+)\.apk$")

# sha256 is recomputed only when the file changes.
_digests: dict[tuple[str, int, int], str] = {}


@dataclass(frozen=True)
class Release:
    """A published package: identity, size and checksum."""

    filename: str
    version_name: str
    version_code: int
    size: int
    sha256: str


def apk_dir() -> Path:
    """Directory holding the published package (env override or next to the DB)."""
    configured = os.environ.get("ELISDB_APK_DIR")
    if configured:
        return Path(configured)
    return Path(os.environ.get("ELISDB_DB", "elisdb.db")).parent / "apk" / "latest"


def parse_name(filename: str) -> tuple[str, int] | None:
    """Splits ``elisdb-<versionName>-<versionCode>.apk``; None when it does not match."""
    match = _NAME.match(filename)
    if match is None:
        return None
    return match.group("name"), int(match.group("code"))


def _sha256(path: Path, size: int) -> str:
    key = (str(path), path.stat().st_mtime_ns, size)
    cached = _digests.get(key)
    if cached is not None:
        return cached
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    value = digest.hexdigest()
    _digests[key] = value
    return value


def find_release(directory: Path | None = None) -> Release | None:
    """Newest published package, or None when nothing valid is published."""
    directory = directory or apk_dir()
    try:
        entries = list(directory.iterdir())
    except OSError:
        return None

    best: tuple[int, str] | None = None  # (versionCode, versionName)
    unexpected = False
    for entry in entries:
        if not entry.is_file():
            continue
        parsed = parse_name(entry.name)
        if parsed is None:
            unexpected = unexpected or entry.suffix == ".apk"
            continue
        name, code = parsed
        if best is None or (code, name) > best:
            best = (code, name)

    if unexpected:
        logger.warning(
            "ignoring .apk files not named elisdb-<versionName>-<versionCode>.apk in %s",
            directory,
        )
    if best is None:
        return None

    version_code, version_name = best
    path = directory / f"elisdb-{version_name}-{version_code}.apk"
    try:
        size = path.stat().st_size
    except OSError:
        return None
    if size > MAX_APK_BYTES:
        logger.warning(
            "ignoring %s: %d bytes exceeds the %d byte limit",
            path.name,
            size,
            MAX_APK_BYTES,
        )
        return None
    return Release(
        filename=path.name,
        version_name=version_name,
        version_code=version_code,
        size=size,
        sha256=_sha256(path, size),
    )
