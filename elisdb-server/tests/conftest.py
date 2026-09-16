"""Shared test fixtures: a temp-dir database, an issued key, and a signing client.

The ElisDB server is normally Docker-only; these tests exercise the real modules
directly. Importing ``app.main`` has module-level side effects (it builds the
default Database), so the environment is pointed at a throwaway temp directory
before that import happens.
"""

import base64
import os
import tempfile
import time

import pytest

# Point the module-level Database away from the repo before importing app.main.
_TMP = tempfile.mkdtemp(prefix="elisdb-tests-")
os.environ.setdefault("ELISDB_DB", os.path.join(_TMP, "import.db"))
os.environ.setdefault("ELISDB_PHOTOS", os.path.join(_TMP, "import-photos"))
# Keep the default limiter out of the way; rate-limit tests set their own.
os.environ.setdefault("ELISDB_RATE_LIMIT_REQUESTS", "100000")

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey  # noqa: E402
from cryptography.hazmat.primitives.serialization import (  # noqa: E402
    Encoding,
    NoEncryption,
    PrivateFormat,
    PublicFormat,
)
from fastapi.testclient import TestClient  # noqa: E402

from app import auth  # noqa: E402
from app.database import Database  # noqa: E402


def make_key() -> tuple[Ed25519PrivateKey, str, str]:
    """Generate an ed25519 keypair as (private, private_seed_b64, public_b64)."""
    private = Ed25519PrivateKey.generate()
    seed = private.private_bytes(Encoding.Raw, PrivateFormat.Raw, NoEncryption())
    public = private.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    return (
        private,
        base64.b64encode(seed).decode("ascii"),
        base64.b64encode(public).decode("ascii"),
    )


class SigningClient:
    """Wraps a TestClient and signs every request with a keypair."""

    def __init__(self, client: TestClient, private: Ed25519PrivateKey, public_b64: str):
        self.client = client
        self.private = private
        self.public_b64 = public_b64

    def request(self, method: str, path: str, body: bytes = b"", timestamp: int | None = None):
        if isinstance(body, str):
            body = body.encode("utf-8")
        ts = int(time.time()) if timestamp is None else timestamp
        message = auth.build_message(method, path, body, ts)
        signature = auth.sign(message, self.private)
        headers = {
            auth.TIMESTAMP_HEADER: str(ts),
            auth.SIGNATURE_HEADER: signature,
            auth.KEY_HEADER: self.public_b64,
            "Content-Type": "application/json",
        }
        return self.client.request(method, path, content=body, headers=headers)

    def get(self, path: str, **kwargs):
        return self.request("GET", path, b"", **kwargs)

    def post(self, path: str, body: bytes = b"", **kwargs):
        return self.request("POST", path, body, **kwargs)

    def put(self, path: str, body: bytes = b"", **kwargs):
        return self.request("PUT", path, body, **kwargs)

    def delete(self, path: str, **kwargs):
        return self.request("DELETE", path, b"", **kwargs)


@pytest.fixture
def server(tmp_path):
    """A fresh app + database + issued key, exposed through a SigningClient."""
    from app.main import create_app

    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    private, _seed, public = make_key()
    db.add_key("test-key", public, comment="tester")
    app = create_app(db)
    client = SigningClient(TestClient(app), private, public)
    return client, db


@pytest.fixture
def keypair():
    return make_key()
