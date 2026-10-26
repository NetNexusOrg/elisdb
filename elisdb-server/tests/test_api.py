"""End-to-end tests over the FastAPI app: auth, push/pull, validation,
photos, deletion, history and rate limiting."""

import hashlib
import json
import time


def _body(records) -> bytes:
    return json.dumps({"records": records}).encode("utf-8")


def _record(id="1", nome="Ana", updated_at=1000, fotos=None, **extra):
    client = {"id": id, "nome": nome, "updated_at": updated_at}
    client.update(extra)
    return {"client": client, "fotos": fotos or []}


def test_health_and_time_are_open(server):
    client, _db = server
    health = client.client.get("/health")
    assert health.status_code == 200
    assert health.json()["status"] == "ok"

    now = client.client.get("/api/time")
    assert now.status_code == 200
    assert isinstance(now.json()["time"], int)


def test_missing_signature_is_rejected(server):
    client, _db = server
    assert client.client.get("/api/latest").status_code == 401
    assert client.client.post("/api/push", json={"records": []}).status_code == 401


def test_push_then_pull_roundtrip(server):
    client, _db = server
    pushed = client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))
    assert pushed.status_code == 200
    assert pushed.json()[0]["status"] == "accepted"

    pulled = client.get("/api/pull")
    assert pulled.status_code == 200
    ids = [r["client"]["id"] for r in pulled.json()["records"]]
    assert ids == ["1"]


def test_invalid_record_does_not_abort_the_batch(server):
    client, _db = server
    response = client.post(
        "/api/push",
        _body([_record(id="bad", nome="   "), _record(id="good", nome="Bea", updated_at=1001)]),
    )
    assert response.status_code == 200
    statuses = {r["id"]: r["status"] for r in response.json()}
    assert statuses["bad"] == "error"
    assert statuses["good"] == "accepted"

    pulled = client.get("/api/pull")
    assert [r["client"]["id"] for r in pulled.json()["records"]] == ["good"]


def test_field_length_limit_rejects_request(server):
    client, _db = server
    too_long = "x" * 201
    response = client.post("/api/push", _body([_record(id="1", nome=too_long)]))
    assert response.status_code == 422


def test_far_future_timestamp_is_rejected_per_record(server):
    client, _db = server
    response = client.post("/api/push", _body([_record(id="1", updated_at=9_999_999_999_999)]))
    assert response.status_code == 200
    assert response.json()[0]["status"] == "error"


def test_pull_parameter_validation(server):
    client, _db = server
    assert client.get("/api/pull?limit=0").status_code == 422
    assert client.get("/api/pull?limit=1001").status_code == 422
    # since_id without since is an explicit 400 (not silently ignored).
    assert client.get("/api/pull?since_id=abc").status_code == 400


def test_keys_me_returns_identity_and_listing_is_forbidden(server):
    client, _db = server
    me = client.get("/api/keys/me")
    assert me.status_code == 200
    assert me.json() == {"key_id": "test-key", "username": "tester"}
    assert client.get("/api/keys").status_code == 403


def test_replayed_request_is_rejected(server):
    client, _db = server
    ts = int(time.time())
    first = client.request("GET", "/api/latest", timestamp=ts)
    second = client.request("GET", "/api/latest", timestamp=ts)
    assert first.status_code == 200
    assert second.status_code == 401


def test_photo_upload_and_download_roundtrip(server):
    client, _db = server
    payload = b"\x00\x01fake-image-bytes"
    assert client.put("/api/photos/f1", payload).status_code == 204
    # A photo only gets a database row once a push references it.
    reference = [{"id": "f1", "mime": "image/jpeg", "ordem": 0}]
    client.post("/api/push", _body([_record(id="1", updated_at=1000, fotos=reference)]))
    got = client.get("/api/photos/f1")
    assert got.status_code == 200
    assert got.content == payload
    assert client.get("/api/photos/missing").status_code == 404


def test_oversized_photo_is_rejected_with_413(server, monkeypatch):
    client, _db = server
    monkeypatch.setattr("app.main.MAX_PHOTO_BYTES", 100)
    response = client.put("/api/photos/big", b"x" * 200)
    assert response.status_code == 413


def test_delete_client_and_missing_returns_404(server):
    client, db = server
    client.post("/api/push", _body([_record(id="1", updated_at=1000)]))
    now = int(time.time())
    assert client.delete("/api/clients/1", timestamp=now).status_code == 204
    assert client.delete("/api/clients/1", timestamp=now + 1).status_code == 404
    assert db.get_client_with_photos("1") is None


def test_latest_timestamp_reflects_pushes(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=4242)]))
    latest = client.get("/api/latest")
    assert latest.status_code == 200
    assert latest.json()["latest_updated_at"] == 4242


def test_latest_includes_live_client_count(server):
    client, _db = server
    base = int(time.time())
    first = client.get("/api/latest", timestamp=base)
    assert first.status_code == 200
    assert first.json()["client_count"] == 0

    client.post("/api/push", _body([_record(id="1"), _record(id="2", nome="Bia")]))
    two = client.get("/api/latest", timestamp=base + 1)
    assert two.json()["client_count"] == 2

    assert client.delete("/api/clients/1", timestamp=base + 2).status_code == 204
    # The tombstone left by the deletion is not a client.
    one = client.get("/api/latest", timestamp=base + 3)
    assert one.json()["client_count"] == 1


def test_client_history_records_the_actor(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=1000)]))
    history = client.get("/api/clients/1/history")
    assert history.status_code == 200
    entries = history.json()
    assert len(entries) == 1
    assert entries[0]["action"] == "create"
    assert entries[0]["username"] == "tester"


def test_rate_limit_returns_429(tmp_path, monkeypatch):
    from fastapi.testclient import TestClient

    from app.database import Database
    from app.main import create_app

    from conftest import SigningClient, make_key

    monkeypatch.setenv("ELISDB_RATE_LIMIT_REQUESTS", "2")
    monkeypatch.setenv("ELISDB_RATE_LIMIT_WINDOW_SECONDS", "60")

    db = Database(str(tmp_path / "rl.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    private, _seed, public = make_key()
    db.add_key("rl-key", public, comment="rl")
    app = create_app(db)
    client = SigningClient(TestClient(app), private, public)

    # Distinct timestamps keep identical requests from tripping the replay guard.
    now = int(time.time())
    assert client.get("/api/latest", timestamp=now).status_code == 200
    assert client.get("/api/latest", timestamp=now + 1).status_code == 200
    assert client.get("/api/latest", timestamp=now + 2).status_code == 429


def test_client_history_carries_the_recorded_changes(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))
    client.post("/api/push", _body([_record(id="1", nome="Ana Silva", updated_at=1001)]))

    entries = client.get("/api/clients/1/history").json()
    # The create carried no photos, so it has no change details.
    assert entries[0]["action"] == "create"
    assert entries[0]["details"] is None
    assert entries[-1]["action"] == "edit"
    assert entries[-1]["details"]["fields"] == [
        {"field": "nome", "old": "ANA", "new": "ANA SILVA", "truncated": False}
    ]


def test_app_release_without_a_package_is_a_404(server, monkeypatch, tmp_path):
    client, _db = server
    monkeypatch.setenv("ELISDB_APK_DIR", str(tmp_path / "empty"))

    assert client.get("/api/app/latest").status_code == 404
    assert client.get("/api/app/download").status_code == 404


def test_app_release_metadata_matches_the_package(server, monkeypatch, tmp_path):
    client, _db = server
    monkeypatch.setenv("ELISDB_APK_DIR", str(tmp_path))
    (tmp_path / "elisdb-1.6.0-11.apk").write_bytes(b"release-bytes")

    response = client.get("/api/app/latest")

    assert response.status_code == 200
    payload = response.json()
    assert payload["version_name"] == "1.6.0"
    assert payload["version_code"] == 11
    assert payload["size"] == len(b"release-bytes")
    assert payload["sha256"] == hashlib.sha256(b"release-bytes").hexdigest()
    assert payload["filename"] == "elisdb-1.6.0-11.apk"


def test_app_release_download_serves_the_published_package(server, monkeypatch, tmp_path):
    client, _db = server
    monkeypatch.setenv("ELISDB_APK_DIR", str(tmp_path))
    (tmp_path / "elisdb-1.6.0-11.apk").write_bytes(b"release-bytes")

    response = client.get("/api/app/download")

    assert response.status_code == 200
    assert response.content == b"release-bytes"
    assert response.headers["content-type"].startswith(
        "application/vnd.android.package-archive"
    )


def test_app_release_requires_a_signature(server, monkeypatch, tmp_path):
    client, _db = server
    monkeypatch.setenv("ELISDB_APK_DIR", str(tmp_path))
    (tmp_path / "elisdb-1.6.0-11.apk").write_bytes(b"x")

    assert client.client.get("/api/app/latest").status_code == 401
    assert client.client.get("/api/app/download").status_code == 401
