"""Tests for P3 idempotent push, paginated history and delete audit."""

import json
import time


def _body(records) -> bytes:
    return json.dumps({"records": records}).encode("utf-8")


def _record(id="1", nome="Ana", updated_at=1000, **extra):
    client = {"id": id, "nome": nome, "updated_at": updated_at}
    client.update(extra)
    return {"client": client, "fotos": []}


def test_push_idempotency_key_replays_without_duplicate_audit(server):
    client, _db = server
    body = _body([_record(id="1", updated_at=1000)])
    headers = {"Idempotency-Key": "batch-1"}
    now = int(time.time())

    first = client.post("/api/push", body, headers=headers, timestamp=now)
    second = client.post("/api/push", body, headers=headers, timestamp=now + 1)

    assert first.status_code == 200
    assert second.status_code == 200
    assert second.json() == first.json()
    history = client.get("/api/clients/1/history").json()
    assert len(history) == 1

    changed = _body([_record(id="1", nome="Bea", updated_at=1001)])
    conflict = client.post("/api/push", changed, headers=headers)
    assert conflict.status_code == 409


def test_history_pages_by_cursor_and_keeps_legacy_full_list(server):
    client, _db = server
    for index in range(3):
        response = client.post(
            "/api/push",
            _body([_record(id="1", nome=f"Ana {index}", updated_at=1000 + index)]),
        )
        assert response.status_code == 200

    full = client.get("/api/clients/1/history")
    assert full.status_code == 200
    assert len(full.json()) == 3

    first = client.get("/api/clients/1/history?limit=1")
    assert first.status_code == 200
    payload = first.json()
    assert len(payload["entries"]) == 1
    assert payload["next_after_id"] is not None

    second = client.get(
        f"/api/clients/1/history?limit=1&after_id={payload['next_after_id']}"
    )
    assert second.status_code == 200
    assert len(second.json()["entries"]) == 1

    bad = client.get("/api/clients/1/history?limit=201")
    assert bad.status_code == 422


def test_delete_is_audited_with_the_actor_and_deleted_record(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", nome="Ana", updated_at=1000)]))
    now = int(time.time())
    response = client.delete(f"/api/clients/1?deleted_at={now}")
    assert response.status_code == 204

    history = client.get("/api/clients/1/history").json()
    delete_entry = [entry for entry in history if entry["action"] == "delete"]
    assert len(delete_entry) == 1
    assert delete_entry[0]["username"] == "tester"
    assert delete_entry[0]["details"]["deleted_client"]["nome"] == "ANA"


def test_rejected_delete_writes_no_audit_entry(server):
    client, _db = server
    client.post("/api/push", _body([_record(id="1", updated_at=2000)]))

    response = client.delete("/api/clients/1?deleted_at=500")
    assert response.status_code == 409
    history = client.get("/api/clients/1/history").json()
    assert all(entry["action"] != "delete" for entry in history)
