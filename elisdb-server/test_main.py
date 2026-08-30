import base64
import hashlib
import json
import logging
import os
import threading
import time

import pytest
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from fastapi.testclient import TestClient

from app import auth
from app.database import Database
from app.main import create_app

B64 = base64.b64encode(b"\xff\xd8fakejpeg").decode("ascii")


def photo_hash(data_b64: str) -> str:
    """SHA-256 hex of a photo's decoded content, as returned by the server."""
    return hashlib.sha256(base64.b64decode(data_b64)).hexdigest()


def upload_photo(fx: AuthFixture, foto_id: str, data_b64: str) -> None:
    resp = fx.request("PUT", f"/api/photos/{foto_id}", body=base64.b64decode(data_b64))
    assert resp.status_code == 204


def make_client(id: str, nome: str, updated_at: int, **kw) -> dict:
    base = {
        "id": id,
        "nome": nome,
        "contrato": None,
        "latitude": 1.0,
        "longitude": 2.0,
        "notas": None,
        "updated_at": updated_at,
    }
    base.update(kw)
    return base


class AuthFixture:
    """TestClient with a registered dev key and request-signing helpers."""

    def __init__(self, database: Database):
        self.database = database
        self.private_key = Ed25519PrivateKey.generate()
        public_key = base64.b64encode(
            self.private_key.public_key().public_bytes_raw()
        ).decode("ascii")
        database.add_key("dev", public_key)
        self.client = TestClient(create_app(database))
        self._last_ts = 0

    def headers(
        self,
        method: str,
        path: str,
        body: bytes,
        timestamp: int | None = None,
        signature_override: str | None = None,
        timestamp_override: str | None = None,
    ) -> dict[str, str]:
        if timestamp is None:
            # A correct client stamps each request with a strictly increasing
            # timestamp, so two requests never share a signature (which would
            # otherwise trip replay protection).
            ts = max(int(time.time()), self._last_ts + 1)
            self._last_ts = ts
        else:
            ts = timestamp
        message = auth.build_message(method, path, body, int(ts))
        sig = (
            auth.sign(message, self.private_key)
            if signature_override is None
            else signature_override
        )
        return {
            auth.TIMESTAMP_HEADER: (
                str(int(ts)) if timestamp_override is None else timestamp_override
            ),
            auth.SIGNATURE_HEADER: sig,
        }

    def request(
        self,
        method: str,
        path: str,
        body: bytes = b"",
        sign: bool = True,
        headers_override: dict[str, str] | None = None,
    ):
        # The server verifies the signature over request.url.path plus the raw
        # query string; sign the full path (including any query) here.
        headers = self.headers(method, path, body) if sign else {}
        if body:
            headers["Content-Type"] = "application/json"
        if headers_override:
            headers.update(headers_override)
        return self.client.request(method, path, content=body, headers=headers)


def push(fx: AuthFixture, records: list[dict]):
    """Upload photos via PUT, then push records with photo metadata only."""
    stripped = []
    for record in records:
        rec = {"client": record["client"], "fotos": []}
        for foto in record.get("fotos", []):
            if foto.get("dataB64") is not None:
                upload_photo(fx, foto["id"], foto["dataB64"])
            rec["fotos"].append(
                {"id": foto["id"], "mime": foto["mime"], "ordem": foto.get("ordem", 0)}
            )
        stripped.append(rec)
    body = json.dumps({"records": stripped}).encode("utf-8")
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 200
    return resp.json()


def pull(fx: AuthFixture, path: str = "/api/pull", **params) -> dict:
    """GET /api/pull returning the parsed envelope (records, tombstones, next_since)."""
    resp = fx.request("GET", path, **params)
    assert resp.status_code == 200
    return resp.json()


def pull_records(fx: AuthFixture, path: str = "/api/pull", **params) -> list:
    return pull(fx, path=path, **params)["records"]


@pytest.fixture()
def fx(tmp_path):
    return AuthFixture(Database(str(tmp_path / "test.db")))


@pytest.fixture()
def fx_named(tmp_path):
    """Fixture whose registered key carries a user name (comment)."""
    fixture = AuthFixture(Database(str(tmp_path / "named.db")))
    fixture.database.remove_key("dev")
    private_key = Ed25519PrivateKey.generate()
    public_key = base64.b64encode(
        private_key.public_key().public_bytes_raw()
    ).decode("ascii")
    fixture.database.add_key("named", public_key, comment="Maria")
    fixture.private_key = private_key
    return fixture


# ---------------------------------------------------------------------------
# key comments and server-issued keys
# ---------------------------------------------------------------------------

def build_legacy_db(tmp_path) -> str:
    """Create a pre-comment database with a keys table lacking the column."""
    import sqlite3
    db_path = str(tmp_path / "legacy.db")
    conn = sqlite3.connect(db_path)
    conn.execute(
        "CREATE TABLE keys ("
        " id TEXT PRIMARY KEY, public_key TEXT NOT NULL, created_at INTEGER NOT NULL)"
    )
    conn.execute(
        "INSERT INTO keys (id, public_key, created_at) VALUES ('legacy', 'LEGACYKEY', 1)"
    )
    conn.commit()
    conn.close()
    return db_path


def test_comment_column_migrates_an_existing_db(tmp_path):
    db = Database(build_legacy_db(tmp_path))
    keys = db.list_keys()
    assert keys == [{"id": "legacy", "public_key": "LEGACYKEY", "comment": None, "provisioned": 0, "created_at": 1}]


def test_add_key_with_and_without_comment(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    db.add_key("k1", "PUB1")
    db.add_key("k2", "PUB2", comment="Maria")
    by_id = {k["id"]: k for k in db.list_keys()}
    assert by_id["k1"]["comment"] == ""
    assert by_id["k2"]["comment"] == "Maria"


def test_issue_registers_public_key_with_user_comment(fx):
    from app.keys import issue
    seed, public, key_id = issue("Joao")
    fx.database.add_key(key_id, public, comment="Joao")
    by_id = {k["id"]: k for k in fx.database.list_keys()}
    assert by_id[key_id]["public_key"] == public
    assert by_id[key_id]["comment"] == "Joao"
    assert seed  # 32-byte seed base64, non-empty


def test_issued_key_authenticates_against_allowlist(fx):
    from app.keys import generate
    seed_b64, public_b64 = generate()
    fx.database.add_key("issued-1", public_b64, comment="Maria")
    body = b"{}"
    ts = int(time.time())
    message = auth.build_message("GET", "/api/pull", body, ts)
    private = Ed25519PrivateKey.from_private_bytes(base64.b64decode(seed_b64))
    sig = auth.sign(message, private)
    headers = {auth.TIMESTAMP_HEADER: str(ts), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("GET", "/api/pull", body=body, headers_override=headers)
    assert resp.status_code == 200


def test_get_api_keys_is_operator_only(fx):
    # Key listing is no longer exposed to client devices.
    resp = fx.request("GET", "/api/keys")
    assert resp.status_code == 403


def test_keys_me_still_returns_own_identity(fx):
    resp = fx.request("GET", "/api/keys/me")
    assert resp.status_code == 200
    assert resp.json()["key_id"] == "dev"


def test_external_public_key_add_still_works(fx):
    fx.database.add_key("ext", "EXTPUB")
    keys = {k["id"]: k for k in fx.database.list_keys()}
    assert keys["ext"]["public_key"] == "EXTPUB"
    assert keys["ext"]["comment"] == ""


# ---------------------------------------------------------------------------
# db-server behaviour tests (now signed)
# ---------------------------------------------------------------------------

def test_push_persists_client_and_photos(fx):
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    assert results == [
        {"id": "c1", "status": "accepted", "existing_id": None, "client": None, "fotos": []}
    ]
    pull = pull_records(fx)
    assert len(pull) == 1
    assert pull[0]["client"]["nome"] == "MARIA"
    assert len(pull[0]["fotos"]) == 1
    assert pull[0]["fotos"][0]["hash"] == photo_hash(B64)


def test_push_without_coordinates_accepted_and_persisted(fx):
    results = push(
        fx,
        [
            {
                "client": {
                    "id": "c1",
                    "nome": "Sem Local",
                    "contrato": None,
                    "latitude": None,
                    "longitude": None,
                    "notas": None,
                    "updated_at": 100,
                },
                "fotos": [],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert len(pull) == 1
    assert pull[0]["client"]["latitude"] is None
    assert pull[0]["client"]["longitude"] is None


def test_push_omitting_coordinates_defaults_to_null(fx):
    results = push(
        fx,
        [
            {
                "client": {
                    "id": "c1",
                    "nome": "Sem Local",
                    "updated_at": 100,
                },
                "fotos": [],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert pull[0]["client"]["latitude"] is None
    assert pull[0]["client"]["longitude"] is None


def test_push_with_coordinates_still_works(fx):
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "Com Local", 100),
                "fotos": [],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert pull[0]["client"]["latitude"] == 1.0
    assert pull[0]["client"]["longitude"] == 2.0


def test_push_referenced_photo_keeps_stored_file(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "Maria Editada", 200),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": None, "ordem": 0}],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert len(pull[0]["fotos"]) == 1
    assert pull[0]["fotos"][0]["id"] == "f1"
    assert pull[0]["fotos"][0]["hash"] == photo_hash(B64)


def test_push_reference_to_unknown_photo_is_ignored(fx):
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f9", "mime": "image/jpeg", "dataB64": None, "ordem": 0}],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert pull[0]["fotos"] == []


def test_push_mixed_data_and_references(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [
                    {"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0},
                    {"id": "f2", "mime": "image/jpeg", "dataB64": "YWJj", "ordem": 1},
                ],
            }
        ],
    )
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "Maria Editada", 200),
                "fotos": [
                    {"id": "f1", "mime": "image/jpeg", "dataB64": None, "ordem": 0},
                    {"id": "f2", "mime": "image/jpeg", "dataB64": "eHl6", "ordem": 1},
                ],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    fotos = {f["id"]: f["hash"] for f in pull[0]["fotos"]}
    assert fotos["f1"] == photo_hash(B64)
    assert fotos["f2"] == photo_hash("eHl6")


def test_push_photo_id_with_traversal_is_rejected(fx):
    body = json.dumps(
        {
            "records": [
                {
                    "client": make_client("c1", "Maria", 100),
                    "fotos": [
                        {"id": "../../../../tmp/escape", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}
                    ],
                }
            ]
        }
    ).encode()
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 422
    # Nothing was persisted and no file was written outside the photos dir.
    assert fx.request("GET", "/api/clients").json() == []


def test_push_photo_id_with_backslash_is_rejected(fx):
    body = json.dumps(
        {
            "records": [
                {
                    "client": make_client("c1", "Maria", 100),
                    "fotos": [{"id": "..\\..\\escape", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
                }
            ]
        }
    ).encode()
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 422


def test_push_normal_photo_id_still_works(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "foto-123.jpg", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    pull = pull_records(fx)
    assert len(pull[0]["fotos"]) == 1
    assert pull[0]["fotos"][0]["id"] == "foto-123.jpg"


def test_photo_id_from_another_client_is_rejected(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    body = json.dumps(
        {
            "records": [
                {
                    "client": make_client("c2", "Joao", 200),
                    "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
                }
            ]
        }
    ).encode()
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 400
    pull = pull_records(fx)
    by_id = {c["client"]["id"]: c for c in pull}
    assert by_id["c1"]["fotos"][0]["id"] == "f1"
    assert by_id["c1"]["fotos"][0]["hash"] == photo_hash(B64)
    assert "c2" not in by_id


def test_duplicate_create_rejected_case_and_whitespace_insensitive(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    results = push(fx, [{"client": make_client("c2", "  MARIA  ", 200), "fotos": []}])
    assert results[0]["status"] == "duplicate"
    assert results[0]["existing_id"] == "c1"
    assert results[0]["client"]["id"] == "c1"
    assert results[0]["client"]["nome"] == "MARIA"


def test_push_lowercase_name_stored_and_returned_uppercased(fx):
    results = push(
        fx,
        [
            {
                "client": make_client("c1", "maRiA de sousa", 100),
                "fotos": [],
            }
        ],
    )
    assert results[0]["status"] == "accepted"
    pull = pull_records(fx)
    assert len(pull) == 1
    assert pull[0]["client"]["nome"] == "MARIA DE SOUSA"


def test_duplicate_still_case_insensitive_with_uppercased_storage(fx):
    push(fx, [{"client": make_client("c1", "maRiA", 100), "fotos": []}])
    results = push(fx, [{"client": make_client("c2", "maria", 200), "fotos": []}])
    assert results[0]["status"] == "duplicate"
    assert results[0]["existing_id"] == "c1"
    assert results[0]["client"]["nome"] == "MARIA"


def test_duplicate_response_carries_canonical_with_photos(fx):
    B64_1 = base64.b64encode(b"canonical-photo").decode("ascii")
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100, notas="canonical"),
                "fotos": [
                    {"id": "f1", "mime": "image/jpeg", "dataB64": B64_1, "ordem": 0},
                    {"id": "f2", "mime": "image/png", "dataB64": B64_1, "ordem": 1},
                ],
            }
        ],
    )
    results = push(
        fx,
        [
            {
                "client": make_client("c2", "  MARIA  ", 200, notas="novo"),
                "fotos": [{"id": "f3", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    assert results[0]["status"] == "duplicate"
    assert results[0]["existing_id"] == "c1"
    canonical = results[0]["client"]
    assert canonical["id"] == "c1"
    assert canonical["nome"] == "MARIA"
    assert canonical["notas"] == "canonical"
    fotos = results[0]["fotos"]
    assert [f["id"] for f in fotos] == ["f1", "f2"]
    assert fotos[0]["hash"] == photo_hash(B64_1)
    # The pushed duplicate is not persisted; the canonical record is intact.
    pull = pull_records(fx)
    assert [c["client"]["id"] for c in pull] == ["c1"]
    stored_canonical = pull[0]
    assert stored_canonical["client"]["notas"] == "canonical"
    assert len(stored_canonical["fotos"]) == 2


def test_duplicate_does_not_persist_rejected_record(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    push(
        fx,
        [
            {
                "client": make_client("c2", "MARIA", 200),
                "fotos": [{"id": "f2", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    pull = pull_records(fx)
    assert [c["client"]["id"] for c in pull] == ["c1"]
    assert [f["id"] for f in pull[0]["fotos"]] == ["f1"]


def test_stale_update_loses(fx):
    push(fx, [{"client": make_client("c1", "Maria", 500), "fotos": []}])
    results = push(
        fx,
        [{"client": make_client("c1", "Maria", 400, notas="antiga"), "fotos": []}],
    )
    assert results[0]["status"] == "stale"
    stored = fx.request("GET", "/api/clients").json()
    assert stored[0]["notas"] is None


def test_newer_update_wins(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    results = push(
        fx,
        [{"client": make_client("c1", "Maria", 200, notas="nova"), "fotos": []}],
    )
    assert results[0]["status"] == "accepted"
    stored = fx.request("GET", "/api/clients").json()
    assert stored[0]["notas"] == "nova"


def test_same_record_pushed_twice_is_stored_once(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert len(fx.request("GET", "/api/clients").json()) == 1


def test_rename_into_duplicate_is_rejected(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx, [{"client": make_client("c2", "Joao", 100), "fotos": []}])
    results = push(fx, [{"client": make_client("c2", "MARIA", 300), "fotos": []}])
    assert results[0]["status"] == "duplicate"
    assert results[0]["existing_id"] == "c1"


def test_get_clients_returns_metadata_only(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    resp = fx.request("GET", "/api/clients")
    assert resp.status_code == 200
    assert "fotos" not in resp.json()[0]
    assert resp.json()[0]["nome"] == "MARIA"


def test_pull_returns_all_clients_ordered_by_name(fx):
    for i, nome in [(1, "zeta"), (2, "alfa")]:
        push(fx, [{"client": make_client(f"c{i}", nome, 100), "fotos": []}])
    pull = pull_records(fx)
    assert [c["client"]["nome"] for c in pull] == ["ALFA", "ZETA"]


def test_pull_without_limit_remains_a_flat_list(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    pull = pull_records(fx)
    assert isinstance(pull, list)


def test_pull_since_returns_only_newer_records(fx):
    push(fx, [{"client": make_client("old", "Antiga", 100), "fotos": []}])
    push(fx, [{"client": make_client("new", "Nova", 300), "fotos": []}])
    pull = pull_records(fx, path="/api/pull?since=200")
    assert [c["client"]["nome"] for c in pull] == ["NOVA"]


def test_pull_since_exclusive_on_equality(fx):
    push(fx, [{"client": make_client("c1", "Mesma", 200), "fotos": []}])
    pull = pull_records(fx, path="/api/pull?since=200")
    assert pull == []


def test_pull_limit_returns_page_and_next_since(fx):
    for i in range(3):
        push(fx, [{"client": make_client(f"c{i}", f"C{i}", 100 + i), "fotos": []}])
    body = fx.request("GET", "/api/pull?limit=2").json()
    assert len(body["records"]) == 2
    assert body["next_since"] == 101


def test_pull_limit_with_equal_timestamps_does_not_skip(fx):
    push(fx, [{"client": make_client("a", "A", 100), "fotos": []}])
    push(fx, [{"client": make_client("b", "B", 200), "fotos": []}])
    push(fx, [{"client": make_client("c", "C", 200), "fotos": []}])
    push(fx, [{"client": make_client("d", "D", 300), "fotos": []}])
    page1 = fx.request("GET", "/api/pull?limit=2").json()
    assert [r["client"]["id"] for r in page1["records"]] == ["a", "b"]
    assert page1["next_since"] == 200
    assert page1["next_since_id"] == "b"
    page2 = fx.request("GET", "/api/pull?limit=2&since=200&since_id=b").json()
    assert [r["client"]["id"] for r in page2["records"]] == ["c", "d"]
    assert page2["next_since"] is None


def test_pull_limit_with_no_next_page_returns_none(fx):
    push(fx, [{"client": make_client("c1", "Só um", 100), "fotos": []}])
    body = fx.request("GET", "/api/pull?limit=5").json()
    assert len(body["records"]) == 1
    assert body["next_since"] is None


def test_delete_client_cascades_to_photos(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    assert fx.request("DELETE", "/api/clients/c1").status_code == 204
    assert pull_records(fx) == []
    assert fx.request("GET", "/api/clients").json() == []


def test_delete_unknown_client_returns_404(fx):
    assert fx.request("DELETE", "/api/clients/nao-existe").status_code == 404


def test_delete_writes_tombstone_and_pull_returns_it(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1?deleted_at=500").status_code == 204
    body = pull(fx)
    assert body["records"] == []
    assert body["tombstones"] == [{"id": "c1", "deleted_at": 500}]


def test_delete_without_timestamp_uses_server_time(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1").status_code == 204
    body = pull(fx)
    assert len(body["tombstones"]) == 1
    assert body["tombstones"][0]["id"] == "c1"


def test_push_of_tombstoned_old_record_is_stale(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1?deleted_at=500").status_code == 204
    results = push(fx, [{"client": make_client("c1", "Maria Editada", 400), "fotos": []}])
    assert results[0]["status"] == "stale"
    body = pull(fx)
    assert body["records"] == []
    assert body["tombstones"] == [{"id": "c1", "deleted_at": 500}]


def test_push_of_newer_record_resurrects_client(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1?deleted_at=500").status_code == 204
    results = push(fx, [{"client": make_client("c1", "Maria", 700), "fotos": []}])
    assert results[0]["status"] == "accepted"
    body = pull(fx)
    assert [r["client"]["id"] for r in body["records"]] == ["c1"]
    assert body["tombstones"] == []


def test_pull_incremental_filters_tombstones_by_since(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx, [{"client": make_client("c2", "Joao", 200), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1?deleted_at=500").status_code == 204
    body = pull(fx, path="/api/pull?since=400")
    assert body["records"] == []
    assert body["tombstones"] == [{"id": "c1", "deleted_at": 500}]
    older = pull(fx, path="/api/pull?since=600")
    assert older["tombstones"] == []


def test_create_records_audit_with_actor(fx_named):
    push(fx_named, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    history = fx_named.request("GET", "/api/clients/c1/history").json()
    assert len(history) == 1
    entry = history[0]
    assert entry["action"] == "create"
    assert entry["username"] == "Maria"
    assert entry["key_id"] == "named"
    assert entry["ts"] == 100


def test_update_records_audit_with_actor(fx_named):
    push(fx_named, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx_named, [{"client": make_client("c1", "Maria Editada", 200), "fotos": []}])
    history = fx_named.request("GET", "/api/clients/c1/history").json()
    assert [e["action"] for e in history] == ["create", "edit"]
    assert history[1]["username"] == "Maria"
    assert history[1]["ts"] == 200


def test_unnamed_key_records_null_username(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    history = fx.request("GET", "/api/clients/c1/history").json()
    assert len(history) == 1
    assert history[0]["username"] is None
    assert history[0]["key_id"] == "dev"


def test_refused_stale_does_not_record_audit(fx):
    push(fx, [{"client": make_client("c1", "Maria", 200), "fotos": []}])
    results = push(fx, [{"client": make_client("c1", "Maria Antiga", 100), "fotos": []}])
    assert results[0]["status"] == "stale"
    history = fx.request("GET", "/api/clients/c1/history").json()
    assert len(history) == 1  # only the accepted create


def test_refused_duplicate_does_not_record_audit(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    results = push(fx, [{"client": make_client("c2", "maria", 200), "fotos": []}])
    assert results[0]["status"] == "duplicate"
    history = fx.request("GET", "/api/clients/c2/history").json()
    assert history == []


def test_history_ordered_by_timestamp(fx_named):
    push(fx_named, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx_named, [{"client": make_client("c1", "Maria A", 200), "fotos": []}])
    push(fx_named, [{"client": make_client("c1", "Maria B", 300), "fotos": []}])
    history = fx_named.request("GET", "/api/clients/c1/history").json()
    assert [e["ts"] for e in history] == [100, 200, 300]
    assert [e["action"] for e in history] == ["create", "edit", "edit"]


def test_history_unknown_client_returns_empty(fx):
    history = fx.request("GET", "/api/clients/nao-existe/history").json()
    assert history == []


def test_push_blank_name_returns_400(fx):
    body = json.dumps({"records": [{"client": make_client("c1", "   ", 100), "fotos": []}]}).encode()
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 400


def test_data_survives_server_restart(tmp_path):
    db_path = str(tmp_path / "persist.db")
    db1 = Database(db_path)
    fx1 = AuthFixture(db1)
    push(fx1, [{"client": make_client("c1", "Maria", 100), "fotos": []}])

    db2 = Database(db_path)
    fx2 = AuthFixture(db2)
    pull = pull_records(fx2)
    assert len(pull) == 1
    assert pull[0]["client"]["nome"] == "MARIA"


# ---------------------------------------------------------------------------
# photo storage on disk
# ---------------------------------------------------------------------------

def test_push_writes_photo_file_to_disk_and_returns_base64(fx, tmp_path):
    data_b64 = B64
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": data_b64, "ordem": 0}],
            }
        ],
    )
    assert (tmp_path / "photos" / "f1").read_bytes() == base64.b64decode(data_b64)
    pull = pull_records(fx)
    assert pull[0]["fotos"][0]["hash"] == photo_hash(data_b64)
    assert pull[0]["fotos"][0]["mime"] == "image/jpeg"


def test_photo_download_returns_bytes_with_media_type(fx):
    upload_photo(fx, "f1", B64)
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": [{"id": "f1", "mime": "image/jpeg", "ordem": 0}]}])

    resp = fx.request("GET", "/api/photos/f1")

    assert resp.status_code == 200
    assert resp.headers["content-type"] == "image/jpeg"
    assert resp.content == base64.b64decode(B64)


def test_photo_upload_persists_bytes(fx, tmp_path):
    resp = fx.request("PUT", "/api/photos/f1", body=base64.b64decode(B64))
    assert resp.status_code == 204
    assert (tmp_path / "photos" / "f1").read_bytes() == base64.b64decode(B64)


def test_photo_download_unknown_returns_404(fx):
    assert fx.request("GET", "/api/photos/nao-existe").status_code == 404


def test_photo_endpoints_require_auth(fx):
    assert fx.client.get("/api/photos/f1").status_code == 401
    assert fx.client.put("/api/photos/f1", content=b"x").status_code == 401


def test_photo_file_survives_server_restart(tmp_path):
    db_path = str(tmp_path / "persist.db")
    db1 = Database(db_path)
    fx1 = AuthFixture(db1)
    push(
        fx1,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/png", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    # New Database against the same db path reuses the default photos/ dir.
    db2 = Database(db_path)
    fx2 = AuthFixture(db2)
    pull = pull_records(fx2)
    assert len(pull) == 1
    assert pull[0]["fotos"][0]["hash"] == photo_hash(B64)


def test_delete_client_removes_photo_files(fx, tmp_path):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    photo_path = tmp_path / "photos" / "f1"
    assert photo_path.exists()
    assert fx.request("DELETE", "/api/clients/c1").status_code == 204
    assert not photo_path.exists()


def test_configured_photo_directory_is_honored(tmp_path):
    custom = tmp_path / "custom-photos"
    db = Database(str(tmp_path / "db.db"), fotos_dir=str(custom))
    fx = AuthFixture(db)
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    assert (custom / "f1").read_bytes() == base64.b64decode(B64)


def test_default_photo_directory_relative_to_db(tmp_path):
    db = Database(str(tmp_path / "db.db"))
    assert db.fotos_dir == str(tmp_path / "photos")


# ---------------------------------------------------------------------------
# authentication tests
# ---------------------------------------------------------------------------

def test_signed_request_accepted(fx):
    body = json.dumps({"records": [{"client": make_client("c1", "Maria", 100), "fotos": []}]}).encode()
    resp = fx.request("POST", "/api/push", body=body)
    assert resp.status_code == 200
    assert resp.json()[0]["status"] == "accepted"


def test_missing_signature_rejected(fx):
    body = b"{}"
    resp = fx.request("POST", "/api/push", body=body, sign=False)
    assert resp.status_code == 401


def test_unknown_key_rejected(fx):
    other = Ed25519PrivateKey.generate()
    body = b"{}"
    timestamp = int(time.time())
    message = auth.build_message("POST", "/api/push", body, timestamp)
    sig = auth.sign(message, other)
    headers = {auth.TIMESTAMP_HEADER: str(timestamp), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("POST", "/api/push", body=body, headers_override=headers)
    assert resp.status_code == 401


def test_tampered_body_rejected(fx):
    good_body = json.dumps({"records": [{"client": make_client("c1", "Maria", 100), "fotos": []}]}).encode()
    timestamp = int(time.time())
    # signature over the real body, but send a different body
    good_message = auth.build_message("POST", "/api/push", good_body, timestamp)
    sig = auth.sign(good_message, fx.private_key)
    tampered_body = json.dumps({"records": [{"client": make_client("c1", "HACK", 100), "fotos": []}]}).encode()
    headers = {auth.TIMESTAMP_HEADER: str(timestamp), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("POST", "/api/push", body=tampered_body, headers_override=headers)
    assert resp.status_code == 401


def test_signature_not_covering_query_is_rejected(fx):
    timestamp = int(time.time())
    # Signature over /api/pull WITHOUT the query string, sent to a query URL.
    message = auth.build_message("GET", "/api/pull", b"", timestamp)
    sig = auth.sign(message, fx.private_key)
    headers = {auth.TIMESTAMP_HEADER: str(timestamp), auth.SIGNATURE_HEADER: sig}
    resp = fx.client.request("GET", "/api/pull?since=200", headers=headers)
    assert resp.status_code == 401


def test_tampered_query_rejected(fx):
    body = b""
    timestamp = int(time.time())
    # Signature over /api/pull?since=200, but the request carries since=999.
    message = auth.build_message("GET", "/api/pull?since=200", body, timestamp)
    sig = auth.sign(message, fx.private_key)
    headers = {auth.TIMESTAMP_HEADER: str(timestamp), auth.SIGNATURE_HEADER: sig}
    resp = fx.client.request("GET", "/api/pull?since=999", headers=headers)
    assert resp.status_code == 401


def test_stale_timestamp_rejected(fx):
    body = b"{}"
    old_ts = int(time.time()) - 100000
    message = auth.build_message("POST", "/api/push", body, old_ts)
    sig = auth.sign(message, fx.private_key)
    headers = {auth.TIMESTAMP_HEADER: str(old_ts), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("POST", "/api/push", body=body, headers_override=headers)
    assert resp.status_code == 401


def test_future_timestamp_rejected(fx):
    body = b"{}"
    fut_ts = int(time.time()) + 100000
    message = auth.build_message("POST", "/api/push", body, fut_ts)
    sig = auth.sign(message, fx.private_key)
    headers = {auth.TIMESTAMP_HEADER: str(fut_ts), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("POST", "/api/push", body=body, headers_override=headers)
    assert resp.status_code == 401


def test_removed_key_no_longer_accepted(fx):
    assert fx.request("GET", "/api/pull").status_code == 200
    fx.database.remove_key("dev")
    assert fx.request("GET", "/api/pull").status_code == 401


def test_health_endpoint_open(fx):
    assert fx.client.get("/health").status_code == 200
    assert fx.client.get("/health").json() == {"status": "ok"}


def test_time_endpoint_open_and_returns_unix_timestamp(fx):
    import time as _time

    resp = fx.client.get("/api/time")
    assert resp.status_code == 200
    body = resp.json()
    assert "time" in body
    assert isinstance(body["time"], int)
    assert abs(body["time"] - int(_time.time())) <= 5


def test_latest_endpoint_requires_auth(fx):
    assert fx.client.get("/api/latest").status_code == 401


def test_latest_returns_max_client_updated_at(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    push(fx, [{"client": make_client("c2", "Joao", 500), "fotos": []}])
    body = fx.request("GET", "/api/latest").json()
    assert body == {"latest_updated_at": 500}


def test_latest_includes_tombstones_deleted_at(fx):
    push(fx, [{"client": make_client("c1", "Maria", 100), "fotos": []}])
    assert fx.request("DELETE", "/api/clients/c1?deleted_at=900").status_code == 204
    body = fx.request("GET", "/api/latest").json()
    assert body == {"latest_updated_at": 900}


def test_latest_zero_when_no_data(fx):
    body = fx.request("GET", "/api/latest").json()
    assert body == {"latest_updated_at": 0}


def test_keys_add_list_remove(fx):
    assert [k["id"] for k in fx.database.list_keys()] == ["dev"]
    fx.database.add_key("k2", "BBBB")
    assert "k2" in [k["id"] for k in fx.database.list_keys()]
    assert fx.database.remove_key("k2") is True
    assert [k["id"] for k in fx.database.list_keys()] == ["dev"]


def test_pull_requires_auth(fx):
    assert fx.client.get("/api/pull").status_code == 401


def test_fresh_server_authenticates_no_requests(tmp_path):
    """A brand-new server DB trusts nobody: no built-in/default key exists."""
    db = Database(str(tmp_path / "fresh.db"))
    client = TestClient(create_app(db))

    body = b"{}"
    ts = int(time.time())
    message = auth.build_message("GET", "/api/pull", body, ts)
    ephemeral = Ed25519PrivateKey.generate()
    sig = auth.sign(message, ephemeral)
    headers = {auth.TIMESTAMP_HEADER: str(ts), auth.SIGNATURE_HEADER: sig}
    resp = client.request("GET", "/api/pull", content=body, headers=headers)
    assert resp.status_code == 401


def test_only_added_keys_authenticate(fx):
    """Only keys placed on the allowlist via add_key are accepted."""
    from app.keys import generate

    seed_b64, public_b64 = generate()
    db = fx.database
    assert fx.request("GET", "/api/pull").status_code == 200  # dev key already added
    db.add_key("manual", public_b64)

    # Sign with the manual pair -> accepted now that it is allowlisted.
    private = Ed25519PrivateKey.from_private_bytes(base64.b64decode(seed_b64))
    body = b"{}"
    ts = int(time.time())
    message = auth.build_message("GET", "/api/pull", body, ts)
    sig = auth.sign(message, private)
    headers = {auth.TIMESTAMP_HEADER: str(ts), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("GET", "/api/pull", body=body, headers_override=headers)
    assert resp.status_code == 200


def test_me_returns_username_for_named_key(fx):
    from app.keys import generate

    seed_b64, public_b64 = generate()
    fx.database.add_key("user-ana", public_b64, comment="Ana")
    private = Ed25519PrivateKey.from_private_bytes(base64.b64decode(seed_b64))
    body = b""
    ts = int(time.time())
    message = auth.build_message("GET", "/api/keys/me", body, ts)
    sig = auth.sign(message, private)
    headers = {auth.TIMESTAMP_HEADER: str(ts), auth.SIGNATURE_HEADER: sig}
    resp = fx.request("GET", "/api/keys/me", body=body, headers_override=headers)
    assert resp.status_code == 200
    assert resp.json() == {"key_id": "user-ana", "username": "Ana"}


def test_me_returns_null_username_for_unnamed_key(fx):
    resp = fx.request("GET", "/api/keys/me")
    assert resp.status_code == 200
    assert resp.json() == {"key_id": "dev", "username": None}


def test_issue_refuses_blank_user_name(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "issue.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    assert keys_main(["issue", ""]) == 2
    db = Database(db_path)
    assert db.list_keys() == []


def test_issue_writes_config_file(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "issue.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.chdir(tmp_path)
    assert keys_main(["issue", "Maria", "--server-url", "https://server.example"]) == 0
    config_path = tmp_path / "Maria.config.json"
    config = json.loads(config_path.read_text())
    assert config["server_url"] == "https://server.example"
    assert base64.b64decode(config["private_seed"])
    assert len(base64.b64decode(config["private_seed"])) == 32
    by_id = {k["id"]: k for k in Database(db_path).list_keys()}
    assert any(k["comment"] == "Maria" for k in by_id.values())


def test_issue_refuses_missing_server_url(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "issue2.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.delenv("ELISDB_SERVER_URL", raising=False)
    assert keys_main(["issue", "Maria"]) == 2
    assert Database(db_path).list_keys() == []
    assert not (tmp_path / "Maria.config.json").exists()


def test_issue_writes_config_to_output_flag(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "issue3.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.chdir(tmp_path)
    out = tmp_path / "custom.json"
    assert (
        keys_main(["issue", "Joao", "--server-url", "http://host:8000", "--output", str(out)])
        == 0
    )
    config = json.loads(out.read_text())
    assert config["server_url"] == "http://host:8000"
    assert config["private_seed"]


def test_issue_registers_named_key(tmp_path, monkeypatch):
    import app.keys as keys_mod

    db_path = str(tmp_path / "issue2.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    seed, public, key_id = keys_mod.issue("Joao")
    keys_mod._database().add_key(key_id, public, comment="Joao")
    by_id = {k["id"]: k for k in keys_mod._database().list_keys()}
    assert by_id[key_id]["public_key"] == public
    assert by_id[key_id]["comment"] == "Joao"


def test_removed_commands_are_refused_without_touching_the_allowlist(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "removed.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    for command in ["generate", "add", "remove", "list"]:
        assert keys_main([command]) == 2
    assert Database(db_path).list_keys() == []


# ---------------------------------------------------------------------------
# interactive keys CLI
# ---------------------------------------------------------------------------

def test_interactive_menu_dispatches_and_exits(tmp_path, monkeypatch, capsys):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.setattr("builtins.input", lambda prompt="": "4")
    assert keys_main([]) == 0
    out = capsys.readouterr().out
    assert "Criar autenticação" in out
    assert "Listar autenticações" in out
    assert "Remover autenticação" in out


def test_interactive_invalid_option_reprompts(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    answers = iter(["x", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert Database(db_path).list_keys() == []


def test_interactive_create_prompts_url_then_user_and_writes_config(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.delenv("ELISDB_SERVER_URL", raising=False)
    monkeypatch.chdir(tmp_path)
    answers = iter(["1", "https://server.example", "Maria", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    config = json.loads((tmp_path / "Maria.config.json").read_text())
    assert config["server_url"] == "https://server.example"
    assert base64.b64decode(config["private_seed"])
    by_id = {k["id"]: k for k in Database(db_path).list_keys()}
    assert any(k["comment"] == "Maria" for k in by_id.values())


def test_interactive_create_uses_env_url_default(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.setenv("ELISDB_SERVER_URL", "https://env.example")
    monkeypatch.chdir(tmp_path)
    answers = iter(["1", "", "Ana", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    config = json.loads((tmp_path / "Ana.config.json").read_text())
    assert config["server_url"] == "https://env.example"


def test_interactive_create_refuses_blank_server_url(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.delenv("ELISDB_SERVER_URL", raising=False)
    monkeypatch.chdir(tmp_path)
    answers = iter(["1", "", "", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert Database(db_path).list_keys() == []
    assert not (tmp_path / "Ana.config.json").exists()


def test_interactive_create_refuses_blank_user_name(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    monkeypatch.setenv("ELISDB_SERVER_URL", "https://server.example")
    monkeypatch.chdir(tmp_path)
    answers = iter(["1", "https://server.example", "", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert Database(db_path).list_keys() == []
    assert not (tmp_path / ".config.json").exists()


def test_interactive_list_empty_vs_with_keys(tmp_path, monkeypatch, capsys):
    from app.keys import main as keys_main
    from app.keys import issue as issue_key

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    answers = iter(["2", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert "nenhuma chave confiada" in capsys.readouterr().out

    seed, public, key_id = issue_key("Joao")
    Database(db_path).add_key(key_id, public, comment="Joao")
    answers = iter(["2", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    out = capsys.readouterr().out
    assert "1 chave(s) confiada(s)" in out
    assert key_id in out
    assert "Joao" in out
    assert public in out


def test_interactive_remove_requires_confirmation(tmp_path, monkeypatch):
    from app.keys import main as keys_main
    from app.keys import issue as issue_key

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    db = Database(db_path)
    seed, public, key_id = issue_key("Joao")
    db.add_key(key_id, public, comment="Joao")

    answers = iter(["3", key_id, "n", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert [k["id"] for k in db.list_keys()] == [key_id]


def test_interactive_remove_deletes_after_confirmation(tmp_path, monkeypatch):
    from app.keys import main as keys_main
    from app.keys import issue as issue_key

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    db = Database(db_path)
    seed, public, key_id = issue_key("Joao")
    db.add_key(key_id, public, comment="Joao")

    answers = iter(["3", key_id, "s", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert db.list_keys() == []


def test_interactive_remove_unknown_id_is_refused(tmp_path, monkeypatch):
    from app.keys import main as keys_main

    db_path = str(tmp_path / "interactive.db")
    monkeypatch.setenv("ELISDB_DB", db_path)
    answers = iter(["3", "nao-existe", "4"])
    monkeypatch.setattr("builtins.input", lambda prompt="": next(answers))
    assert keys_main([]) == 0
    assert Database(db_path).list_keys() == []


# ---------------------------------------------------------------------------
# high / medium / low priority improvements
# ---------------------------------------------------------------------------


def _signed_headers(private_key, method, path, body: bytes = b"") -> dict:
    ts = int(time.time())
    message = auth.build_message(method, path, body, ts)
    sig = auth.sign(message, private_key)
    return {
        auth.TIMESTAMP_HEADER: str(ts),
        auth.SIGNATURE_HEADER: sig,
    }


def test_auth_identity_isolated_between_concurrent_devices(tmp_path):
    """Concurrent pushes by different devices must record the correct actor."""
    db = Database(str(tmp_path / "concurrent.db"))
    priv_a = Ed25519PrivateKey.generate()
    pub_a = base64.b64encode(priv_a.public_key().public_bytes_raw()).decode()
    priv_b = Ed25519PrivateKey.generate()
    pub_b = base64.b64encode(priv_b.public_key().public_bytes_raw()).decode()
    db.add_key("devA", pub_a)
    db.add_key("devB", pub_b)
    app = create_app(db)
    client = TestClient(app)
    errors = []

    def worker(priv, key_id, label, idx):
        for i in range(10):
            cid = f"{label}-{idx}-{i}"
            body = json.dumps(
                {"records": [{"client": {"id": cid, "nome": cid, "updated_at": 100}, "fotos": []}]}
            ).encode()
            headers = _signed_headers(priv, "POST", "/api/push", body)
            headers["Content-Type"] = "application/json"
            r = client.post("/api/push", content=body, headers=headers)
            if r.status_code != 200:
                errors.append((cid, r.status_code))
                continue
            hist = db.client_history(cid)
            if not hist or hist[-1]["key_id"] != key_id:
                errors.append((cid, [h["key_id"] for h in hist]))

    threads = [threading.Thread(target=worker, args=(priv_a, "devA", "A", k)) for k in range(2)]
    threads += [threading.Thread(target=worker, args=(priv_b, "devB", "B", k)) for k in range(2)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    assert not errors, errors


def test_photo_file_removed_when_edited_out(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    # Edit removing the photo: its file must be deleted from disk.
    push(fx, [{"client": make_client("c1", "Maria", 200), "fotos": []}])
    assert not os.path.exists(os.path.join(fx.database.fotos_dir, "f1"))


def test_gc_removes_orphan_photo_file(fx):
    push(
        fx,
        [
            {
                "client": make_client("c1", "Maria", 100),
                "fotos": [{"id": "f1", "mime": "image/jpeg", "dataB64": B64, "ordem": 0}],
            }
        ],
    )
    # Simulate an orphan file from a PUT that was never referenced by a push.
    orphan = os.path.join(fx.database.fotos_dir, "orphan1")
    with open(orphan, "wb") as fh:
        fh.write(b"orphan")
    removed = fx.database.gc_orphan_photos()
    assert removed == 1
    assert not os.path.exists(orphan)
    # The real photo file is kept.
    assert os.path.exists(os.path.join(fx.database.fotos_dir, "f1"))


def test_photo_upload_too_large_rejected(fx, monkeypatch):
    monkeypatch.setattr("app.main.MAX_PHOTO_BYTES", 1024)
    big = b"x" * 2048
    headers = _signed_headers(fx.private_key, "PUT", f"/api/photos/big1", big)
    headers["content-length"] = str(len(big))
    resp = fx.client.put(f"/api/photos/big1", content=big, headers=headers)
    assert resp.status_code == 413
    # A within-limit upload still works.
    small = b"x" * 100
    headers2 = _signed_headers(fx.private_key, "PUT", f"/api/photos/small1", small)
    headers2["content-length"] = str(len(small))
    resp2 = fx.client.put(f"/api/photos/small1", content=small, headers=headers2)
    assert resp2.status_code == 204


def test_replay_within_window_rejected(fx):
    body = json.dumps({"records": []}).encode()
    headers = _signed_headers(fx.private_key, "POST", "/api/push", body)
    headers["Content-Type"] = "application/json"
    assert fx.client.post("/api/push", content=body, headers=headers).status_code == 200
    # Same signature replayed within the window is rejected.
    assert fx.client.post("/api/push", content=body, headers=headers).status_code == 401


def test_key_header_short_circuits_verification(fx):
    pub = base64.b64encode(fx.private_key.public_key().public_bytes_raw()).decode()
    body = json.dumps({"records": []}).encode()
    headers = _signed_headers(fx.private_key, "POST", "/api/push", body)
    headers[auth.KEY_HEADER] = pub
    headers["Content-Type"] = "application/json"
    assert fx.client.post("/api/push", content=body, headers=headers).status_code == 200


def test_key_header_wrong_key_rejected(fx):
    other = Ed25519PrivateKey.generate()
    wrong_pub = base64.b64encode(other.public_key().public_bytes_raw()).decode()
    body = json.dumps({"records": []}).encode()
    headers = _signed_headers(fx.private_key, "POST", "/api/push", body)
    headers[auth.KEY_HEADER] = wrong_pub
    headers["Content-Type"] = "application/json"
    assert fx.client.post("/api/push", content=body, headers=headers).status_code == 401


def test_concurrent_pushes_do_not_lock(tmp_path):
    db = Database(str(tmp_path / "wal.db"))
    priv = Ed25519PrivateKey.generate()
    pub = base64.b64encode(priv.public_key().public_bytes_raw()).decode()
    db.add_key("dev", pub)
    app = create_app(db)
    client = TestClient(app)
    errors = []

    def worker(idx):
        for i in range(15):
            cid = f"c-{idx}-{i}"
            body = json.dumps(
                {"records": [{"client": {"id": cid, "nome": cid, "updated_at": 100}, "fotos": []}]}
            ).encode()
            headers = _signed_headers(priv, "POST", "/api/push", body)
            headers["Content-Type"] = "application/json"
            r = client.post("/api/push", content=body, headers=headers)
            if r.status_code != 200:
                errors.append((cid, r.status_code))

    threads = [threading.Thread(target=worker, args=(k,)) for k in range(4)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    assert not errors, errors


def test_logging_is_configured():
    create_app(Database(":memory:"))
    assert logging.getLogger("elisdb.server").getEffectiveLevel() >= logging.INFO