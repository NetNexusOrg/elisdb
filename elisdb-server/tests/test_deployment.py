"""Deployment defaults must not create accounts or expose the server naively."""

from pathlib import Path

import pytest

from elisdb.keys import validate_server_url

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ROOT / "compose.yaml"
DOCKERFILE = ROOT / "docker" / "Dockerfile"
ENTRYPOINT = ROOT / "docker" / "entrypoint.sh"


def test_compose_does_not_ship_default_accounts_or_a_placeholder_url():
    text = COMPOSE.read_text(encoding="utf-8")

    assert "maria,joao" not in text
    assert "<ip-do-host>" not in text
    assert "ELISDB_USERS" not in text
    assert 'ELISDB_SERVER_URL: "${ELISDB_SERVER_URL:-}"' in text


def test_startup_never_provisions_accounts():
    compose = COMPOSE.read_text(encoding="utf-8")
    entrypoint = ENTRYPOINT.read_text(encoding="utf-8")
    dockerfile = DOCKERFILE.read_text(encoding="utf-8")

    assert "provision" not in compose
    assert "provision" not in entrypoint
    assert "setpriv" not in entrypoint
    assert 'exec "$@"' in entrypoint
    assert "elisdb.provision" not in dockerfile


def test_compose_binds_to_loopback_by_default():
    text = COMPOSE.read_text(encoding="utf-8")

    assert "${ELISDB_BIND_ADDRESS:-127.0.0.1}:8000:8000" in text
    assert "${ELISDB_BIND_ADDRESS:-0.0.0.0}" not in text


def test_compose_uses_host_owned_bind_mounts_and_an_unprivileged_user():
    text = COMPOSE.read_text(encoding="utf-8")

    assert '"${ELISDB_DATA_DIR:-./elisdb-data}:/app/data"' in text
    assert '"./configs:/app/data/configs"' in text
    assert 'user: "${PUID:-1000}:${PGID:-1000}"' in text
    # No named volume: data must be reachable from the host.
    assert "elisdb-data:/app/data" not in text


def test_image_contains_no_runtime_data():
    text = DOCKERFILE.read_text(encoding="utf-8")

    assert "COPY . ." not in text
    assert "COPY elisdb ./elisdb" in text
    assert "PYTHONDONTWRITEBYTECODE" in text


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
def test_invalid_server_urls_are_rejected(url):
    with pytest.raises(ValueError):
        validate_server_url(url)


@pytest.mark.parametrize(
    "url",
    ["https://db.example.com", "http://192.168.1.10:8000", "http://127.0.0.1:8000"],
)
def test_valid_server_urls_are_accepted(url):
    validate_server_url(url)
