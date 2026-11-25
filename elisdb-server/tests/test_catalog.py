"""Catalog CRUD/sync and per-client service associations."""

import json

import pytest

from elisdb.models import (
    CatalogItemIn,
    ClienteIn,
    ServicoIn,
    normalize_catalog_name,
)


def _item(item_id: str, categoria: str = "roedores", nome: str = "Ratoeira", **kwargs):
    base = dict(
        id=item_id,
        categoria=categoria,
        nome=nome,
        ativo=True,
        updated_at=1000,
    )
    base.update(kwargs)
    return CatalogItemIn(**base)


def _client(servicos=None, **kwargs):
    base = dict(id="c1", nome="Cliente", updated_at=1000, servicos=servicos)
    base.update(kwargs)
    return ClienteIn(**base)


def test_normalized_catalog_names():
    assert normalize_catalog_name("  Ratoeira   Grande ") == "RATOEIRA GRANDE"
    assert normalize_catalog_name("Rátoeira") == normalize_catalog_name("RATOEIRA")


def test_catalog_push_is_lww_and_idempotent(server):
    client, db = server
    first = client.post(
        "/api/catalog/push?x=1",
        json.dumps({"items": [_item("i1", updated_at=1000).model_dump()]}),
    )
    assert first.status_code == 200
    assert first.json()[0]["status"] == "accepted"

    same = client.post(
        "/api/catalog/push?x=2",
        json.dumps({"items": [_item("i1", updated_at=1000).model_dump()]}),
    )
    assert same.json()[0]["status"] == "unchanged"

    stale = client.post(
        "/api/catalog/push?x=3",
        json.dumps({"items": [_item("i1", nome="Antiga", updated_at=900).model_dump()]}),
    )
    assert stale.json()[0]["status"] == "stale"

    newer = client.post(
        "/api/catalog/push?x=4",
        json.dumps({"items": [_item("i1", nome="Nova", updated_at=1100).model_dump()]}),
    )
    assert newer.json()[0]["status"] == "accepted"
    assert newer.json()[0]["item"]["nome"] == "NOVA"

    catalog = client.get("/api/catalog?x=1").json()
    assert catalog["complete"] is True
    assert [item["nome"] for item in catalog["items"]] == ["NOVA"]
    assert db.list_catalog()[0]["nome"] == "NOVA"


def test_catalog_category_is_immutable(server):
    client, _db = server
    client.post(
        "/api/catalog/push",
        json.dumps({"items": [_item("i1").model_dump()]}),
    )
    response = client.post(
        "/api/catalog/push",
        json.dumps(
            {
                "items": [
                    _item(
                        "i1",
                        categoria="insetos_voadores",
                        updated_at=1200,
                    ).model_dump()
                ]
            }
        ),
    )
    assert response.json()[0]["status"] == "invalid"
    assert "category" in response.json()[0]["reason"]


def test_duplicate_normalized_names_are_invalid_but_batch_continues(server):
    client, _db = server
    response = client.post(
        "/api/catalog/push",
        json.dumps(
            {
                "items": [
                    _item("i1", nome="Ratoeira").model_dump(),
                    _item("i2", nome="  RÁTOEIRA ", categoria="roedores").model_dump(),
                    _item("i3", nome="Mosca", categoria="insetos_voadores").model_dump(),
                ]
            }
        ),
    )
    results = {result["id"]: result for result in response.json()}
    assert results["i1"]["status"] == "accepted"
    assert results["i2"]["status"] == "invalid"
    assert results["i2"]["reason"] == "duplicate_name"
    assert results["i3"]["status"] == "accepted"

    # The same name in another category is allowed.
    other = client.post(
        "/api/catalog/push",
        json.dumps(
            {
                "items": [
                    _item("i4", nome="Ratoeira", categoria="insetos_voadores").model_dump()
                ]
            }
        ),
    )
    assert other.json()[0]["status"] == "accepted"


def test_catalog_deactivate_and_reactivate_are_logged(server):
    client, _db = server
    client.post(
        "/api/catalog/push?x=1",
        json.dumps({"items": [_item("i1", updated_at=1000).model_dump()]}),
    )
    client.post(
        "/api/catalog/push?x=2",
        json.dumps({"items": [_item("i1", ativo=False, updated_at=1100).model_dump()]}),
    )
    client.post(
        "/api/catalog/push?x=3",
        json.dumps({"items": [_item("i1", ativo=True, updated_at=1200).model_dump()]}),
    )
    history = client.get("/api/history").json()
    actions = [entry["action"] for entry in history["entries"]]
    assert actions[:3] == ["reactivate", "deactivate", "create"]
    assert history["entries"][0]["kind"] == "catalog"
    assert history["entries"][0]["item_nome"] == "RATOEIRA"
    assert history["entries"][0]["cliente_id"] is None


def test_client_services_push_pull_and_history(server):
    client, _db = server
    client.post(
        "/api/catalog/push",
        json.dumps(
            {
                "items": [
                    _item("i1", nome="Ratoeira").model_dump(),
                    _item("i2", categoria="tratamentos", nome="Desinfecao").model_dump(),
                ]
            }
        ),
    )
    body = {
        "records": [
            {
                "client": _client(
                    servicos=[
                        ServicoIn(item_id="i1", quantidade=3).model_dump(),
                        ServicoIn(item_id="i2", quantidade=None).model_dump(),
                    ]
                ).model_dump(),
                "fotos": [],
            }
        ]
    }
    response = client.post("/api/push", json.dumps(body))
    assert response.status_code == 200
    assert response.json()[0]["status"] == "accepted"

    pulled = client.get("/api/pull?since=0").json()["records"][0]["client"]
    assert {s["item_id"]: s["quantidade"] for s in pulled["servicos"]} == {
        "i1": 3,
        "i2": None,
    }

    # The summary list must not carry services.
    listed = client.get("/api/clients").json()[0]
    assert listed.get("servicos") is None

    history = client.get("/api/clients/c1/history").json()
    assert history[-1]["details"]["services"] == [
        {
            "item_id": "i1",
            "nome": "RATOEIRA",
            "change": "added",
            "old": None,
            "new": 3,
        },
        {
            "item_id": "i2",
            "nome": "DESINFECAO",
            "change": "added",
            "old": None,
            "new": None,
        },
    ]


def test_client_services_null_preserves_empty_clears(server):
    client, _db = server
    client.post(
        "/api/catalog/push",
        json.dumps({"items": [_item("i1").model_dump()]}),
    )
    client.post(
        "/api/push",
        json.dumps(
            {
                "records": [
                    {
                        "client": _client(
                            servicos=[ServicoIn(item_id="i1", quantidade=4).model_dump()]
                        ).model_dump(),
                        "fotos": [],
                    }
                ]
            }
        ),
    )

    client.post(
        "/api/push",
        json.dumps(
            {
                "records": [
                    {
                        "client": _client(notas="sem servicos", updated_at=1100).model_dump(),
                        "fotos": [],
                    }
                ]
            }
        ),
    )
    pulled = client.get("/api/pull?since=0&x=1").json()["records"][0]["client"]
    assert [s["quantidade"] for s in pulled["servicos"]] == [4]

    client.post(
        "/api/push",
        json.dumps(
            {
                "records": [
                    {
                        "client": _client(servicos=[], updated_at=1200).model_dump(),
                        "fotos": [],
                    }
                ]
            }
        ),
    )
    pulled = client.get("/api/pull?since=0&x=2").json()["records"][0]["client"]
    assert pulled["servicos"] == []


def test_service_validation(server):
    client, _db = server
    client.post(
        "/api/catalog/push",
        json.dumps(
            {
                "items": [
                    _item("i1", nome="Ratoeira").model_dump(),
                    _item("i2", categoria="tratamentos", nome="Desinfecao").model_dump(),
                ]
            }
        ),
    )

    cases = [
        [ServicoIn(item_id="missing", quantidade=1)],
        [ServicoIn(item_id="i1", quantidade=0)],
        [ServicoIn(item_id="i1", quantidade=None)],
        [ServicoIn(item_id="i2", quantidade=2)],
    ]
    for index, services in enumerate(cases):
        response = client.post(
            f"/api/push?x={index}",
            json.dumps(
                {
                    "records": [
                        {
                            "client": _client(servicos=services).model_dump(),
                            "fotos": [],
                        }
                    ]
                }
            ),
        )
        assert response.status_code == 422 or response.json()[0]["status"] == "error"
        assert _db.list_catalog()

    from pydantic import ValidationError

    with pytest.raises(ValidationError):
        _client(
            servicos=[
                ServicoIn(item_id="i1", quantidade=1),
                ServicoIn(item_id="i1", quantidade=2),
            ]
        )


def test_deleting_client_cascades_services(server):
    client, db = server
    client.post(
        "/api/catalog/push",
        json.dumps({"items": [_item("i1").model_dump()]}),
    )
    client.post(
        "/api/push",
        json.dumps(
            {
                "records": [
                    {
                        "client": _client(
                            servicos=[ServicoIn(item_id="i1", quantidade=2).model_dump()]
                        ).model_dump(),
                        "fotos": [],
                    }
                ]
            }
        ),
    )
    assert client.delete("/api/clients/c1").status_code == 204
    conn = db._conn()
    try:
        assert conn.execute("SELECT COUNT(*) FROM cliente_servicos").fetchone()[0] == 0
    finally:
        conn.close()
