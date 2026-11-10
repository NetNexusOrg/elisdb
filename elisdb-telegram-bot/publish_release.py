#!/usr/bin/env python3
"""Announce an ElisDB Android release in the Telegram group.

Sends the signed APK as a document and the release notes (the changelog section
already extracted by the CI release job) as Telegram HTML messages.

The bot token and the target chat come from the environment, never from a file:
``TELEGRAM_BOT_TOKEN`` and ``TELEGRAM_CHAT_ID``.

Usage::

    TELEGRAM_BOT_TOKEN=... TELEGRAM_CHAT_ID=... \\
        python3 publish_release.py --version 1.13.0 \\
        --notes-file release-notes.md --apk elisdb-1.13.0-20.apk
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

API_ROOT = "https://api.telegram.org"

# The public Bot API accepts uploads up to 50 MB; keep a margin for the
# multipart envelope and the caption.
MAX_UPLOAD_BYTES = 50 * 1024 * 1024
UPLOAD_SAFETY_MARGIN = 1024 * 1024

MAX_MESSAGE_CHARS = 4096
APK_MEDIA_TYPE = "application/vnd.android.package-archive"

_BOLD = re.compile(r"\*\*(.+?)\*\*")
_HEADING = re.compile(r"^#{2,6}\s+(.+)$")
_BULLET = re.compile(r"^\s*[-*]\s+")


class TelegramError(RuntimeError):
    """The Bot API rejected a call."""


def _escape(text: str) -> str:
    """Escape the three characters Telegram HTML treats as markup."""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _inline(text: str) -> str:
    """Escape a line and turn the changelog's ``**bold**`` into Telegram bold."""
    return _BOLD.sub(r"<b>\1</b>", _escape(text))


def format_notes(markdown: str) -> str:
    """Convert a changelog section to Telegram HTML.

    ``### Novidades`` becomes a bold line, ``- item`` becomes a bullet, blank
    lines around headings are dropped, and consecutive blank lines collapse.
    """
    lines: list[str] = []
    after_heading = False
    for raw in markdown.splitlines():
        line = raw.rstrip()
        if not line.strip():
            if lines and lines[-1] != "" and not after_heading:
                lines.append("")
            continue
        heading = _HEADING.match(line)
        if heading:
            if lines and lines[-1] != "":
                lines.append("")
            lines.append(f"<b>{_escape(heading.group(1).strip())}</b>")
            after_heading = True
            continue
        after_heading = False
        bullet = _BULLET.match(line)
        if bullet:
            lines.append("• " + _inline(line[bullet.end() :]))
            continue
        lines.append(_inline(line))
    while lines and lines[-1] == "":
        lines.pop()
    return "\n".join(lines)


def build_announcement(version: str, notes_markdown: str) -> str:
    """Full announcement text: version header followed by the formatted notes."""
    header = f"🚀 <b>ElisDB v{_escape(version)}</b>"
    body = format_notes(notes_markdown)
    return f"{header}\n\n{body}" if body else header


def split_message(text: str, limit: int = MAX_MESSAGE_CHARS) -> list[str]:
    """Split text into messages that fit ``limit``, breaking on line boundaries."""
    text = text.strip()
    if not text:
        return []
    chunks: list[str] = []
    current = ""
    for line in text.split("\n"):
        while len(line) > limit:
            if current:
                chunks.append(current)
                current = ""
            chunks.append(line[:limit])
            line = line[limit:]
        candidate = f"{current}\n{line}" if current else line
        if len(candidate) > limit:
            chunks.append(current)
            current = line
        else:
            current = candidate
    if current:
        chunks.append(current)
    return [chunk for chunk in chunks if chunk.strip()]


def build_multipart(
    fields: dict[str, str],
    file_field: str,
    filename: str,
    content: bytes,
    media_type: str = APK_MEDIA_TYPE,
) -> tuple[bytes, str]:
    """Build a ``multipart/form-data`` body and return it with its boundary."""
    boundary = f"----elisdb{os.urandom(16).hex()}"
    parts: list[bytes] = []
    for name, value in fields.items():
        parts.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n".encode()
        )
    parts.append(
        f'--{boundary}\r\nContent-Disposition: form-data; name="{file_field}"; '
        f'filename="{filename}"\r\nContent-Type: {media_type}\r\n\r\n'.encode()
    )
    parts.append(content)
    parts.append(f"\r\n--{boundary}--\r\n".encode())
    return b"".join(parts), boundary


def api_call(
    token: str,
    method: str,
    fields: dict[str, str],
    *,
    file_field: str | None = None,
    filename: str | None = None,
    content: bytes | None = None,
    media_type: str = APK_MEDIA_TYPE,
    timeout: int = 30,
) -> object:
    """POST to a Bot API method and return its ``result``."""
    url = f"{API_ROOT}/bot{token}/{method}"
    if content is None:
        body = urllib.parse.urlencode(fields).encode()
        headers = {"Content-Type": "application/x-www-form-urlencoded"}
    else:
        if file_field is None or filename is None:
            raise ValueError("file_field and filename are required when uploading")
        body, boundary = build_multipart(fields, file_field, filename, content, media_type)
        headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}

    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", "replace")
        raise TelegramError(f"{method} failed with HTTP {error.code}: {detail}") from error
    except urllib.error.URLError as error:
        raise TelegramError(f"{method} failed: {error.reason}") from error

    if not payload.get("ok"):
        raise TelegramError(f"{method} failed: {payload.get('description')}")
    return payload.get("result")


def _megabytes(size: int) -> str:
    return f"{size / (1024 * 1024):.0f} MB"


def publish(
    *,
    token: str,
    chat_id: str,
    version: str,
    notes: str,
    apk: Path | None,
    dry_run: bool = False,
) -> int:
    """Send the announcement and, when it fits, the APK. Returns an exit code."""
    announcement = build_announcement(version, notes)
    chunks = split_message(announcement)

    apk_size = apk.stat().st_size if apk and apk.is_file() else 0
    fits = apk is not None and apk_size > 0 and apk_size <= MAX_UPLOAD_BYTES - UPLOAD_SAFETY_MARGIN

    if dry_run:
        print(f"[dry-run] chat_id={chat_id} version={version}")
        if apk is None or not apk.is_file():
            print("[dry-run] no APK: announcement only")
        elif fits:
            caption = f"ElisDB v{version} — APK assinado ({_megabytes(apk_size)})"
            print(f"[dry-run] sendDocument {apk.name} ({_megabytes(apk_size)}) caption={caption!r}")
        else:
            print(
                f"[dry-run] APK {apk.name} is {_megabytes(apk_size)}, over the upload limit: "
                "announcement only"
            )
        for index, chunk in enumerate(chunks, start=1):
            print(f"[dry-run] sendMessage {index}/{len(chunks)} ({len(chunk)} chars)")
        print("---")
        print(announcement)
        return 0

    if fits:
        caption = f"ElisDB v{version} — APK assinado ({_megabytes(apk_size)})"
        api_call(
            token,
            "sendDocument",
            {"chat_id": chat_id, "caption": caption},
            file_field="document",
            filename=apk.name,
            content=apk.read_bytes(),
            timeout=300,
        )
        print(f"sent {apk.name} ({_megabytes(apk_size)})")
    elif apk is not None and apk.is_file():
        print(
            f"::warning::APK {apk.name} is {_megabytes(apk_size)}, over the Telegram upload "
            "limit; sending the announcement without it",
            file=sys.stderr,
        )
    else:
        print(f"::warning::no APK at {apk}; sending the announcement only", file=sys.stderr)

    for chunk in chunks:
        api_call(
            token,
            "sendMessage",
            {"chat_id": chat_id, "text": chunk, "parse_mode": "HTML"},
        )
    print(f"sent announcement in {len(chunks)} message(s)")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True, help="release version, without the leading v")
    parser.add_argument("--notes-file", required=True, help="Markdown file with the release notes")
    parser.add_argument("--apk", help="signed APK to attach")
    parser.add_argument("--dry-run", action="store_true", help="print what would be sent")
    args = parser.parse_args(argv)

    notes_path = Path(args.notes_file)
    if not notes_path.is_file():
        print(f"::error::notes file not found: {notes_path}", file=sys.stderr)
        return 1
    notes = notes_path.read_text(encoding="utf-8")

    apk = Path(args.apk) if args.apk else None
    if apk is not None and not apk.is_file():
        print(f"::error::APK not found: {apk}", file=sys.stderr)
        return 1

    token = os.environ.get("TELEGRAM_BOT_TOKEN", "")
    chat_id = os.environ.get("TELEGRAM_CHAT_ID", "")
    if not args.dry_run and (not token or not chat_id):
        print(
            "::warning::TELEGRAM_BOT_TOKEN/TELEGRAM_CHAT_ID not set; skipping announcement",
            file=sys.stderr,
        )
        return 0

    try:
        return publish(
            token=token,
            chat_id=chat_id,
            version=args.version,
            notes=notes,
            apk=apk,
            dry_run=args.dry_run,
        )
    except TelegramError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
