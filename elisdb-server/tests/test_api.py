"""End-to-end tests over the FastAPI app: auth, push/pull, validation,
photos, deletion, history and rate limiting."""

import hashlib
import json
import os
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
    assert health.json()["api_version"] == 2
    assert health.json()["instance_id"]

    now = client.client.get("/api/time")
    assert now.status_code == 200
    assert isinstance(now.json()["time"], int)


def test_instance_id_is_stable_and_latest_exposes_limits(server):
    client, db = server
    health = client.client.get("/health")
    latest = client.get("/api/latest")
    assert latest.status_code == 200
    assert latest.json()["instance_id"] == health.json()["instance_id"]
    assert latest.json()["instance_id"] == db.instance_id()
    assert latest.json()["max_push_records"] == 200

    # Opening the same database again (a restart) keeps the identity.
    from app.database import Database

    reopened = Database(db.db_path, fotos_dir=db.fotos_dir)
    reopened.init()
    assert reopened.instance_id() == db.instance_id()


def test_instance_id_changes_for_a_new_database(tmp_path):
    from app.database import Database

    first = Database(str(tmp_path / "first.db"), fotos_dir=str(tmp_path / "photos-1"))
    second = Database(str(tmp_path / "second.db"), fotos_dir=str(tmp_path / "photos-2"))
    first.init()
    second.init()

    assert first.instance_id()
    assert first.instance_id() == first.instance_id()
    assert first.instance_id() != second.instance_id()


def test_push_limit_is_advertised_and_enforced(tmp_path, monkeypatch):
    client, _db = _fresh_server(tmp_path, monkeypatch, ELISDB_MAX_PUSH_RECORDS=3)
    latest = client.get("/api/latest")
    assert latest.status_code == 200
    assert latest.json()["max_push_records"] == 3

    response = client.post(
        "/api/push",
        _body([_record(id=str(i), nome=f"C{i}") for i in range(4)]),
    )
    assert response.status_code == 422
    assert "3" in response.json()["detail"]
    assert client.get("/api/pull").json()["records"] == []


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

    too_long_tag = "x" * 101
    response = client.post("/api/push", _body([_record(id="2", nome="Bea", tag=too_long_tag)]))
    assert response.status_code == 422

    too_long_total = ";".join(f"T{i}" + "x" * 88 for i in range(6))
    response = client.post(
        "/api/push", _body([_record(id="3", nome="Caio", tag=too_long_total)])
    )
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
    payload = b"\xff\xd8\xff\xe0fake-image-bytes"
    # A photo can only be uploaded after a push references it.
    reference = [{"id": "f1", "mime": "image/jpeg", "ordem": 0}]
    client.post("/api/push", _body([_record(id="1", updated_at=1000, fotos=reference)]))
    assert client.put("/api/photos/f1", payload).status_code == 204
    got = client.get("/api/photos/f1")
    assert got.status_code == 200
    assert got.content == payload
    assert got.headers["x-content-type-options"] == "nosniff"
    assert got.headers["content-disposition"] == "attachment"
    assert client.get("/api/photos/missing").status_code == 404


def test_photo_upload_requires_a_reference(server):
    client, _db = server
    payload = b"\xff\xd8\xff\xe0orphan-bytes"
    response = client.put("/api/photos/not-referenced", payload)
    assert response.status_code == 404
    assert "not referenced" in response.json()["detail"]


def test_photo_quota_rejects_before_writing_the_file(tmp_path, monkeypatch):
    client, db = _fresh_server(
        tmp_path, monkeypatch, ELISDB_MAX_PHOTO_BYTES_TOTAL=10
    )
    reference = [
        {"id": "p1", "mime": "image/jpeg", "ordem": 0},
        {"id": "p2", "mime": "image/jpeg", "ordem": 1},
    ]
    client.post("/api/push", _body([_record(id="1", fotos=reference)]))

    assert client.put("/api/photos/p1", b"\xff\xd8\xff\xe0" + b"x" * 4).status_code == 204
    response = client.put("/api/photos/p2", b"\xff\xd8\xff\xe0" + b"y" * 4)
    assert response.status_code == 413
    assert not os.path.exists(db._foto("p2"))


def test_periodic_gc_removes_an_orphan_without_a_push(tmp_path, monkeypatch):
    from fastapi.testclient import TestClient

    from app.database import Database
    from app.main import create_app

    monkeypatch.setenv("ELISDB_PHOTO_GC_INTERVAL_SECONDS", "1")
    db = Database(str(tmp_path / "gc.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    app = create_app(db)

    with TestClient(app):
        orphan = tmp_path / "photos" / "orphan.jpg"
        orphan.write_bytes(b"\xff\xd8\xff\xe0orphan")
        old = time.time() - 3600
        os.utime(orphan, (old, old))
        deadline = time.time() + 4
        while orphan.exists() and time.time() < deadline:
            time.sleep(0.1)

    assert not orphan.exists()


def test_photo_with_invalid_bytes_is_rejected(server):
    client, _db = server
    response = client.put("/api/photos/f1", b"not-an-image")
    assert response.status_code == 400
    assert response.json()["detail"] == "unsupported image content"


def test_push_with_invalid_photo_mime_is_rejected(server):
    client, _db = server
    response = client.post(
        "/api/push",
        _body(
            [
                _record(
                    id="1",
                    fotos=[{"id": "f1", "mime": "text/html", "ordem": 0}],
                )
            ]
        ),
    )
    assert response.status_code == 422


def test_oversized_photo_is_rejected_with_413(server, monkeypatch):
    client, _db = server
    monkeypatch.setattr("app.main.MAX_PHOTO_BYTES", 100)
    response = client.put("/api/photos/big", b"x" * 200)
    assert response.status_code == 413


def test_oversized_push_body_is_rejected_before_parsing(server, monkeypatch):
    client, _db = server
    monkeypatch.setattr("app.main.MAX_PHOTO_BYTES", 100)
    response = client.post("/api/push", b"x" * 200)
    assert response.status_code == 413


def test_oversized_chunked_push_body_is_rejected(server, monkeypatch):
    client, _db = server
    monkeypatch.setattr("app.main.MAX_PHOTO_BYTES", 100)

    def chunks():
        yield b"x" * 80
        yield b"x" * 80

    response = client.client.post("/api/push", content=chunks())
    assert response.status_code == 413, response.text


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
    assert latest.json()["api_version"] == 2


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


def test_push_reports_photos_missing_on_the_server(server):
    client, _db = server
    response = client.post(
        "/api/push",
        _body(
            [
                _record(
                    id="1",
                    fotos=[{"id": "never-uploaded", "mime": "image/jpeg", "ordem": 0}],
                )
            ]
        ),
    )

    assert response.status_code == 200
    result = response.json()[0]
    assert result["status"] == "accepted"
    assert result["missing_photos"] == ["never-uploaded"]
    assert result["fotos"] == []

    pulled = client.get("/api/pull").json()["records"]
    assert pulled[0]["photos_complete"] is True
    assert pulled[0]["fotos"] == [
        {
            "id": "never-uploaded",
            "mime": "image/jpeg",
            "ordem": 0,
            "hash": None,
            "available": False,
        }
    ]


def test_pull_carries_the_delivery_timestamp(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=1000)]))
    client.post("/api/push", _body([_record(id="2", nome="Bea", updated_at=1000)]))

    # A deletion newer than the record but far behind the server watermark must
    # still be delivered: the tombstone keeps its device timestamp (LWW) and
    # gets a later delivery value.
    now = int(time.time())
    deleted = client.delete("/api/clients/2?deleted_at=1500", timestamp=now)
    assert deleted.status_code == 204

    payload = client.get("/api/pull").json()
    client_row = payload["records"][0]["client"]
    assert client_row["updated_at"] == 1000
    assert client_row["delivery_ts"] >= 1000

    tombstone = payload["tombstones"][0]
    assert tombstone["id"] == "2"
    assert tombstone["deleted_at"] == 1500
    assert tombstone["delivery_ts"] > client_row["delivery_ts"]


def test_delete_rejects_negative_and_future_timestamps(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=1000)]))

    negative = client.delete("/api/clients/1?deleted_at=-1")
    assert negative.status_code == 400
    assert "negative" in negative.json()["detail"]

    future = client.delete("/api/clients/1?deleted_at=99999999999")
    assert future.status_code == 400
    assert "future" in future.json()["detail"]

    payload = client.get("/api/pull").json()
    assert [record["client"]["id"] for record in payload["records"]] == ["1"]


def test_delete_older_than_the_stored_edit_is_rejected(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=2000)]))

    response = client.delete("/api/clients/1?deleted_at=500")
    assert response.status_code == 409
    assert "not newer" in response.json()["detail"]

    # The client is still present and no tombstone was written.
    payload = client.get("/api/pull").json()
    assert [record["client"]["id"] for record in payload["records"]] == ["1"]
    assert payload["tombstones"] == []


def test_client_status_defaults_to_active(server):
    client, _db = server
    pushed = client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))
    assert pushed.json()[0]["status"] == "accepted"

    stored = client.get("/api/pull").json()["records"][0]["client"]
    assert stored["ativo"] is True


def test_inactive_status_roundtrips_and_appears_in_history(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))
    accepted = client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=2000, ativo=False)]),
    )
    assert accepted.json()[0]["status"] == "accepted"

    stored = client.get("/api/pull").json()["records"][0]["client"]
    assert stored["ativo"] is False

    entries = client.get("/api/clients/1/history").json()
    assert entries[-1]["action"] == "edit"
    assert entries[-1]["details"]["fields"] == [
        {"field": "ativo", "old": "Ativo", "new": "Inativo", "truncated": False}
    ]


def test_activation_appears_in_history(server):
    client, _db = server
    client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=1000, ativo=False)]),
    )
    accepted = client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=2000, ativo=True)]),
    )
    assert accepted.json()[0]["status"] == "accepted"

    entries = client.get("/api/clients/1/history").json()
    assert entries[-1]["details"]["fields"] == [
        {"field": "ativo", "old": "Inativo", "new": "Ativo", "truncated": False}
    ]


def test_tag_roundtrips_and_appears_in_history(server):
    client, _db = server
    pushed = client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=1000, tag="Premium")]),
    )
    assert pushed.json()[0]["status"] == "accepted"

    stored = client.get("/api/pull").json()["records"][0]["client"]
    assert stored["tag"] == "PREMIUM"

    accepted = client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=2000, tag="Standard")]),
    )
    assert accepted.json()[0]["status"] == "accepted"

    stored = client.get("/api/pull?limit=1000").json()["records"][0]["client"]
    assert stored["tag"] == "STANDARD"

    entries = client.get("/api/clients/1/history").json()
    assert entries[-1]["details"]["fields"] == [
        {"field": "tag", "old": "PREMIUM", "new": "STANDARD", "truncated": False}
    ]


def test_multiple_tags_are_normalized(server):
    client, _db = server
    pushed = client.post(
        "/api/push",
        _body([_record(id="1", nome="Ana", updated_at=1000, tag="premium; gold;PREMIUM")]),
    )
    assert pushed.json()[0]["status"] == "accepted"

    stored = client.get("/api/pull").json()["records"][0]["client"]
    assert stored["tag"] == "GOLD;PREMIUM"


def test_too_many_tags_are_rejected(server):
    client, _db = server
    tags = ";".join(f"TAG{i}" for i in range(11))
    response = client.post(
        "/api/push", _body([_record(id="1", nome="Ana", tag=tags)])
    )
    assert response.status_code == 422


def test_client_without_tag_roundtrips_without_one(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))

    stored = client.get("/api/pull").json()["records"][0]["client"]
    assert stored["tag"] is None


def _fresh_server(tmp_path, monkeypatch, **env):
    """Build an app with env overrides (read at create_app time)."""
    from fastapi.testclient import TestClient

    from app.database import Database
    from app.main import create_app

    from conftest import SigningClient, make_key

    for name, value in env.items():
        monkeypatch.setenv(name, str(value))

    db = Database(str(tmp_path / "fresh.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    private, _seed, public = make_key()
    db.add_key("fresh-key", public, comment="fresh")
    app = create_app(db)
    return SigningClient(TestClient(app), private, public), db


def test_push_record_limit_is_rejected_with_422(tmp_path, monkeypatch):
    client, _db = _fresh_server(tmp_path, monkeypatch, ELISDB_MAX_PUSH_RECORDS=1)

    response = client.post(
        "/api/push", _body([_record(id="1"), _record(id="2", nome="Bia")])
    )

    assert response.status_code == 422
    assert "too many records" in response.json()["detail"]
    # Validation happens before any write.
    assert client.get("/api/pull").json()["records"] == []


def test_push_photo_limit_is_rejected_with_422(tmp_path, monkeypatch):
    client, _db = _fresh_server(
        tmp_path, monkeypatch, ELISDB_MAX_PHOTOS_PER_CLIENT=1
    )
    fotos = [
        {"id": "f1", "mime": "image/jpeg", "ordem": 0},
        {"id": "f2", "mime": "image/jpeg", "ordem": 1},
    ]

    response = client.post("/api/push", _body([_record(id="1", fotos=fotos)]))

    assert response.status_code == 422
    assert "too many photos" in response.json()["detail"]
    assert client.get("/api/pull").json()["records"] == []


def test_rate_limit_uses_x_forwarded_for_when_trusted(tmp_path, monkeypatch):
    client, _db = _fresh_server(
        tmp_path,
        monkeypatch,
        ELISDB_RATE_LIMIT_REQUESTS=2,
        ELISDB_RATE_LIMIT_WINDOW_SECONDS=60,
        ELISDB_TRUST_PROXY="true",
    )
    now = int(time.time())
    first_client = {"X-Forwarded-For": "203.0.113.1, 10.0.0.2"}

    assert client.get("/api/latest", timestamp=now, headers=first_client).status_code == 200
    assert client.get("/api/latest", timestamp=now + 1, headers=first_client).status_code == 200
    assert client.get("/api/latest", timestamp=now + 2, headers=first_client).status_code == 429

    # A different forwarded client behind the same proxy has its own budget.
    second_client = {"X-Forwarded-For": "203.0.113.2, 10.0.0.2"}
    assert client.get("/api/latest", timestamp=now + 3, headers=second_client).status_code == 200


def test_x_forwarded_for_is_ignored_without_trust_proxy(tmp_path, monkeypatch):
    client, _db = _fresh_server(
        tmp_path,
        monkeypatch,
        ELISDB_RATE_LIMIT_REQUESTS=2,
        ELISDB_RATE_LIMIT_WINDOW_SECONDS=60,
        ELISDB_TRUST_PROXY="false",
    )
    now = int(time.time())

    # Rotating the header must not create new budgets when the proxy is not
    # trusted: every request is attributed to the same real peer.
    assert client.get(
        "/api/latest", timestamp=now, headers={"X-Forwarded-For": "203.0.113.1"}
    ).status_code == 200
    assert client.get(
        "/api/latest", timestamp=now + 1, headers={"X-Forwarded-For": "203.0.113.2"}
    ).status_code == 200
    assert client.get(
        "/api/latest", timestamp=now + 2, headers={"X-Forwarded-For": "203.0.113.3"}
    ).status_code == 429


def test_startup_applies_configured_retention(tmp_path, monkeypatch):
    from app.database import Database, connect
    from app.main import create_app
    from app.models import ClienteIn

    from conftest import make_key

    db = Database(str(tmp_path / "retention.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    db.save_client(
        ClienteIn(id="1", nome="Ana", updated_at=1000),
        [],
        {"id": "key-1", "comment": "tester"},
    )
    db.delete_client("1", deleted_at=1001)

    monkeypatch.setenv("ELISDB_TOMBSTONE_RETENTION_DAYS", "1")
    monkeypatch.setenv("ELISDB_AUDIT_RETENTION_DAYS", "1")
    _private, _seed, public = make_key()
    db.add_key("retention-key", public, comment="tester")

    create_app(db)  # startup runs the configured retention

    conn = connect(db.db_path)
    assert conn.execute("SELECT COUNT(*) FROM tombstones").fetchone()[0] == 0
    assert conn.execute("SELECT COUNT(*) FROM client_audit").fetchone()[0] == 0
    conn.close()
