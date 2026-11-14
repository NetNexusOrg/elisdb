"""Unit tests for the thin management TUI helpers (no Docker, no server)."""

import os
from pathlib import Path

import pytest

import start


def test_read_env_ignores_comments_and_quotes(tmp_path):
    env = tmp_path / ".env"
    env.write_text(
        "# comentário\n"
        "\n"
        "ELISDB_DATA_DIR=./elisdb-data\n"
        'ELISDB_SERVER_URL="https://db.example.com"\n'
        "PUID=1001\n",
        encoding="utf-8",
    )

    values = start.read_env(env)

    assert values == {
        "ELISDB_DATA_DIR": "./elisdb-data",
        "ELISDB_SERVER_URL": "https://db.example.com",
        "PUID": "1001",
    }


def test_write_env_updates_in_place_and_preserves_comments(tmp_path):
    env = tmp_path / ".env"
    env.write_text(
        "# cabeçalho\n"
        "ELISDB_SERVER_URL=https://old.example.com\n"
        "PUID=1000\n",
        encoding="utf-8",
    )
    os.chmod(env, 0o600)

    backup = start.write_env({"PUID": "1001", "PGID": "1001"}, env)

    text = env.read_text(encoding="utf-8")
    assert "# cabeçalho" in text
    assert "ELISDB_SERVER_URL=https://old.example.com" in text
    assert "PUID=1001" in text
    assert "PGID=1001" in text
    assert os.stat(env).st_mode & 0o777 == 0o600
    assert backup is not None and backup.is_file()
    assert "PUID=1000" in backup.read_text(encoding="utf-8")


def test_write_env_without_existing_file_has_no_backup(tmp_path):
    env = tmp_path / ".env"

    backup = start.write_env({"ELISDB_LOG_LEVEL": "DEBUG"}, env)

    assert backup is None
    assert start.read_env(env) == {"ELISDB_LOG_LEVEL": "DEBUG"}


@pytest.mark.parametrize(
    "update",
    [
        {"ELISDB_MAX_PUSH_RECORDS": "0"},
        {"ELISDB_MAX_PUSH_RECORDS": "muitos"},
        {"ELISDB_RATE_LIMIT_WINDOW_SECONDS": "-1"},
        {"ELISDB_TOMBSTONE_RETENTION_DAYS": "-3"},
        {"ELISDB_TRUST_PROXY": "talvez"},
        {"ELISDB_LOG_LEVEL": "VERBOSE"},
        {"ELISDB_SERVER_URL": "ftp://db.example.com"},
        {"ELISDB_SERVER_URL": "https://db.example.com/<ip>"},
    ],
)
def test_write_env_refuses_invalid_values(tmp_path, update):
    env = tmp_path / ".env"

    with pytest.raises(ValueError):
        start.write_env(update, env)

    assert not env.exists()


def test_validate_env_accepts_defaults():
    assert start.validate_env({}) == []
    assert start.validate_env(
        {
            "ELISDB_SERVER_URL": "https://db.example.com",
            "PUID": "1001",
            "PGID": "1001",
            "ELISDB_MAX_PUSH_RECORDS": "200",
            "ELISDB_TOMBSTONE_RETENTION_DAYS": "0",
            "ELISDB_TRUST_PROXY": "true",
            "ELISDB_LOG_LEVEL": "INFO",
        }
    ) == []


def test_human_size():
    assert start.human_size(0) == "0 B"
    assert start.human_size(2048).endswith("KB")
    assert start.human_size(5 * 1024 * 1024).endswith("MB")


def test_config_path_for_uses_configs_dir():
    path = start._config_path_for("maria")

    assert path == Path(start.ROOT) / "configs" / "maria.config.json"
