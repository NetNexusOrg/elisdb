"""End-to-end sync smoke check used as a release gate.

Runs against a disposable database and a freshly generated key: push a client
with one photo, pull it back, delete it, and require the tombstone. It never
touches production data and prints no private key material.
"""

from __future__ import annotations

import base64
import json
import os
import tempfile
import time

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import (
    Encoding,
    NoEncryption,
    PrivateFormat,
    PublicFormat,
)
from fastapi.testclient import TestClient

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def _keypair():
    private = Ed25519PrivateKey.generate()
    seed = private.private_bytes(Encoding.Raw, PrivateFormat.Raw, NoEncryption())
    public = private.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    return private, base64.b64encode(seed).decode("ascii"), base64.b64encode(public).decode("ascii")


def main() -> int:
    from elisdb import auth
    from elisdb.database import Database
    from elisdb.main import create_app

    tmp = tempfile.mkdtemp(prefix="elisdb-sync-smoke-")
    os.environ["ELISDB_DB"] = os.path.join(tmp, "elisdb.db")
    os.environ["ELISDB_PHOTOS"] = os.path.join(tmp, "photos")
    private, _seed, public = _keypair()
    db = Database(os.environ["ELISDB_DB"], fotos_dir=os.environ["ELISDB_PHOTOS"])
    db.init()
    db.add_key("smoke-key", public, comment="sync-smoke")
    client = TestClient(create_app(db))

    def request(method: str, path: str, body: bytes = b"") -> object:
        ts = int(time.time())
        message = auth.build_message(method, path, body, ts)
        signature = auth.sign(message, private)
        return client.request(
            method,
            path,
            content=body,
            headers={
                auth.TIMESTAMP_HEADER: str(ts),
                auth.SIGNATURE_HEADER: signature,
                auth.KEY_HEADER: public,
                "Content-Type": "application/json",
            },
        )

    photo = b"\xff\xd8\xff\xe0smoke-photo"
    push_body = json.dumps(
        {
            "records": [
                {
                    "client": {"id": "smoke", "nome": "Smoke", "updated_at": 1000},
                    "fotos": [{"id": "smoke-photo", "mime": "image/jpeg", "ordem": 0}],
                }
            ]
        }
    ).encode("utf-8")
    pushed = request("POST", "/api/push", push_body)
    if pushed.status_code != 200:
        raise SystemExit(f"push failed: {pushed.status_code} {pushed.text}")
    result = pushed.json()[0]
    if result["status"] != "accepted" or "smoke-photo" not in result["missing_photos"]:
        raise SystemExit(f"unexpected push result: {result}")

    uploaded = request("PUT", "/api/photos/smoke-photo", photo)
    if uploaded.status_code != 204:
        raise SystemExit(f"photo upload failed: {uploaded.status_code} {uploaded.text}")

    pulled = request("GET", "/api/pull")
    if pulled.status_code != 200:
        raise SystemExit(f"pull failed: {pulled.status_code} {pulled.text}")
    payload = pulled.json()
    if not any(record["client"]["id"] == "smoke" for record in payload["records"]):
        raise SystemExit("pulled dataset is missing the smoke client")
    if not any(photo["id"] == "smoke-photo" for record in payload["records"] for photo in record["fotos"]):
        raise SystemExit("pulled dataset is missing the smoke photo")

    downloaded = request("GET", "/api/photos/smoke-photo")
    if downloaded.status_code != 200 or downloaded.content != photo:
        raise SystemExit("photo download did not return the uploaded bytes")

    deleted = request("DELETE", f"/api/clients/smoke?deleted_at={int(time.time())}")
    if deleted.status_code != 204:
        raise SystemExit(f"delete failed: {deleted.status_code} {deleted.text}")
    # The server's anti-replay cache rejects an identical signed GET within the
    # same second; wait one second so this is a genuinely new request.
    time.sleep(1)
    after = request("GET", "/api/pull").json()
    if not any(tombstone["id"] == "smoke" for tombstone in after["tombstones"]):
        raise SystemExit("tombstone was not returned after delete")

    print("sync smoke ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
