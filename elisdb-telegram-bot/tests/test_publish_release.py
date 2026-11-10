"""Tests for the release announcement script (stdlib only, no network)."""

from __future__ import annotations

import sys

import pytest

import publish_release


def test_format_notes_converts_headings_bullets_and_bold() -> None:
    notes = "### Novidades\n\n- **Servidor offline.** Mostra Indisponível.\n- Tags na lista.\n"

    formatted = publish_release.format_notes(notes)

    assert formatted == (
        "<b>Novidades</b>\n"
        "• <b>Servidor offline.</b> Mostra Indisponível.\n"
        "• Tags na lista."
    )


def test_format_notes_escapes_telegram_html() -> None:
    formatted = publish_release.format_notes("- a < b & c > d")

    assert formatted == "• a &lt; b &amp; c &gt; d"


def test_format_notes_collapses_blank_lines_and_trims() -> None:
    formatted = publish_release.format_notes("\n\n### Correções\n\n\n- uma\n\n\n")

    assert formatted == "<b>Correções</b>\n• uma"


def test_build_announcement_has_version_header_and_sections() -> None:
    announcement = publish_release.build_announcement(
        "1.13.0", "### Novidades\n- nova\n\n### Correções\n- corrigida"
    )

    assert announcement.startswith("🚀 <b>ElisDB v1.13.0</b>\n\n")
    assert "<b>Novidades</b>\n• nova\n\n<b>Correções</b>\n• corrigida" in announcement


def test_build_announcement_without_notes_is_just_the_header() -> None:
    assert publish_release.build_announcement("1.13.0", "  \n\n") == "🚀 <b>ElisDB v1.13.0</b>"


def test_split_message_respects_the_limit() -> None:
    text = "\n".join(f"linha {index}" for index in range(500))

    chunks = publish_release.split_message(text, limit=100)

    assert len(chunks) > 1
    assert all(len(chunk) <= 100 for chunk in chunks)
    assert "\n".join(chunks) == text


def test_split_message_hard_splits_a_line_longer_than_the_limit() -> None:
    chunks = publish_release.split_message("x" * 250, limit=100)

    assert [len(chunk) for chunk in chunks] == [100, 100, 50]


def test_split_message_ignores_empty_text() -> None:
    assert publish_release.split_message("   \n\n ") == []


def test_build_multipart_carries_fields_and_file() -> None:
    body, boundary = publish_release.build_multipart(
        {"chat_id": "-100", "caption": "ElisDB"}, "document", "elisdb-1.0.0-1.apk", b"apk-bytes"
    )

    assert body.startswith(f"--{boundary}\r\n".encode())
    assert b'name="chat_id"' in body
    assert b'filename="elisdb-1.0.0-1.apk"' in body
    assert b"application/vnd.android.package-archive" in body
    assert body.endswith(f"--{boundary}--\r\n".encode())
    assert b"apk-bytes" in body


def _recording_api(calls: list[tuple[str, dict[str, str]]]):
    def fake(token: str, method: str, fields: dict[str, str], **kwargs: object) -> object:
        calls.append((method, fields))
        return {"message_id": len(calls)}

    return fake


def test_publish_sends_document_then_announcement(monkeypatch, tmp_path) -> None:
    calls: list[tuple[str, dict[str, str]]] = []
    monkeypatch.setattr(publish_release, "api_call", _recording_api(calls))
    apk = tmp_path / "elisdb-1.13.0-20.apk"
    apk.write_bytes(b"x" * 1024)

    exit_code = publish_release.publish(
        token="token",
        chat_id="-100",
        version="1.13.0",
        notes="### Novidades\n- nova",
        apk=apk,
    )

    assert exit_code == 0
    assert calls[0][0] == "sendDocument"
    assert calls[0][1]["chat_id"] == "-100"
    assert "ElisDB v1.13.0" in calls[0][1]["caption"]
    assert [method for method, _ in calls[1:]] == ["sendMessage"]
    assert calls[1][1]["parse_mode"] == "HTML"


def test_publish_skips_apk_over_the_limit(monkeypatch, tmp_path, capsys) -> None:
    calls: list[tuple[str, dict[str, str]]] = []
    monkeypatch.setattr(publish_release, "api_call", _recording_api(calls))
    apk = tmp_path / "elisdb-1.13.0-20.apk"
    apk.write_bytes(b"x" * 4096)
    monkeypatch.setattr(publish_release, "MAX_UPLOAD_BYTES", 1024)
    monkeypatch.setattr(publish_release, "UPLOAD_SAFETY_MARGIN", 0)

    exit_code = publish_release.publish(
        token="token",
        chat_id="-100",
        version="1.13.0",
        notes="### Novidades\n- nova",
        apk=apk,
    )

    assert exit_code == 0
    assert [method for method, _ in calls] == ["sendMessage"]
    assert "over the Telegram upload limit" in capsys.readouterr().err


def test_publish_dry_run_does_not_call_the_api(monkeypatch, tmp_path, capsys) -> None:
    def fail(*args: object, **kwargs: object) -> object:
        raise AssertionError("api_call must not run in dry-run")

    monkeypatch.setattr(publish_release, "api_call", fail)
    apk = tmp_path / "elisdb-1.13.0-20.apk"
    apk.write_bytes(b"x" * 1024)

    exit_code = publish_release.publish(
        token="",
        chat_id="-100",
        version="1.13.0",
        notes="### Novidades\n- nova",
        apk=apk,
        dry_run=True,
    )

    output = capsys.readouterr().out
    assert exit_code == 0
    assert "[dry-run] sendDocument elisdb-1.13.0-20.apk" in output
    assert "🚀 <b>ElisDB v1.13.0</b>" in output


def test_main_skips_without_credentials(monkeypatch, tmp_path, capsys) -> None:
    monkeypatch.delenv("TELEGRAM_BOT_TOKEN", raising=False)
    monkeypatch.delenv("TELEGRAM_CHAT_ID", raising=False)
    notes = tmp_path / "notes.md"
    notes.write_text("### Novidades\n- nova", encoding="utf-8")

    exit_code = publish_release.main(["--version", "1.13.0", "--notes-file", str(notes)])

    assert exit_code == 0
    assert "skipping announcement" in capsys.readouterr().err


def test_main_fails_when_notes_are_missing(monkeypatch, tmp_path, capsys) -> None:
    monkeypatch.setenv("TELEGRAM_BOT_TOKEN", "token")
    monkeypatch.setenv("TELEGRAM_CHAT_ID", "-100")

    exit_code = publish_release.main(
        ["--version", "1.13.0", "--notes-file", str(tmp_path / "missing.md")]
    )

    assert exit_code == 1
    assert "notes file not found" in capsys.readouterr().err


def test_telegram_error_is_reported_as_an_action_error(monkeypatch, tmp_path, capsys) -> None:
    def failing(*args: object, **kwargs: object) -> object:
        raise publish_release.TelegramError("sendMessage failed: chat not found")

    monkeypatch.setattr(publish_release, "api_call", failing)
    monkeypatch.setenv("TELEGRAM_BOT_TOKEN", "token")
    monkeypatch.setenv("TELEGRAM_CHAT_ID", "-100")
    notes = tmp_path / "notes.md"
    notes.write_text("### Novidades\n- nova", encoding="utf-8")

    exit_code = publish_release.main(["--version", "1.13.0", "--notes-file", str(notes)])

    assert exit_code == 1
    assert "::error::sendMessage failed: chat not found" in capsys.readouterr().err


if __name__ == "__main__":
    sys.exit(pytest.main([__file__]))
