"""Deployment defaults must not create accounts or expose the server naively."""

from pathlib import Path

import pytest

from app.provision import validate_server_url

COMPOSE = Path(__file__).resolve().parents[1] / "compose.yaml"
ENTRYPOINT = Path(__file__).resolve().parents[1] / "docker-entrypoint.sh"


def test_compose_does_not_ship_default_accounts_or_a_placeholder_url():
    text = COMPOSE.read_text(encoding="utf-8")

    assert "maria,joao" not in text
    assert "<ip-do-host>" not in text
    assert 'ELISDB_USERS: "${ELISDB_USERS:-}"' in text
    assert 'ELISDB_SERVER_URL: "${ELISDB_SERVER_URL:-}"' in text


def test_compose_binds_to_loopback_by_default():
    text = COMPOSE.read_text(encoding="utf-8")

    assert "${ELISDB_BIND_ADDRESS:-127.0.0.1}:8000:8000" in text
    assert "${ELISDB_BIND_ADDRESS:-0.0.0.0}" not in text


def test_entrypoint_provisions_before_starting_the_server():
    text = ENTRYPOINT.read_text(encoding="utf-8")

    assert text.index("python -m app.provision") < text.index("exec setpriv")


@pytest.mark.parametrize(
    "url",
    [
        "",
        "http://<ip-do-host>:8000",
        "ftp://db.example.com",
        "https://",
        "db.example.com:8000",
    ],
)
def test_invalid_provisioning_urls_are_rejected(url):
    with pytest.raises(ValueError):
        validate_server_url(url)


@pytest.mark.parametrize(
    "url",
    ["https://db.example.com", "http://192.168.1.10:8000", "http://127.0.0.1:8000"],
)
def test_valid_provisioning_urls_are_accepted(url):
    validate_server_url(url)
