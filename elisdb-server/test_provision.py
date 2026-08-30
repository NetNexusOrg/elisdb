import json
import os

from app.database import Database
from app.provision import main, provision_users


def test_provision_users_issues_keys_for_new_users(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    configs = tmp_path / "configs"
    provisioned, revoked = provision_users(db, ["maria", "joao"], "http://host:8000", str(configs))

    assert provisioned == ["maria", "joao"]
    assert revoked == []
    keys = {k["comment"] for k in db.list_keys()}
    assert keys == {"maria", "joao"}
    for user in ("maria", "joao"):
        path = configs / f"{user}.config.json"
        assert path.exists()
        data = json.loads(path.read_text())
        assert data["server_url"] == "http://host:8000"
        assert data["private_seed"]


def test_provision_users_is_idempotent(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    configs = tmp_path / "configs"
    provision_users(db, ["maria", "joao"], "http://host:8000", str(configs))
    first = {k["public_key"] for k in db.list_keys()}

    provisioned, revoked = provision_users(db, ["maria", "joao"], "http://host:8000", str(configs))

    assert provisioned == []
    assert revoked == []
    assert {k["public_key"] for k in db.list_keys()} == first


def test_provision_users_skips_blank_and_existing(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    configs = tmp_path / "configs"
    db.add_key("existing", "PUBEXISTING", comment="maria")
    provisioned, revoked = provision_users(db, ["maria", " ", "novo"], "http://host:8000", str(configs))

    assert provisioned == ["novo"]
    assert revoked == []
    comments = {k["comment"] for k in db.list_keys()}
    assert comments == {"maria", "novo"}
    assert not (configs / "maria.config.json").exists()


def test_provision_revokes_removed_user_and_config(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    configs = tmp_path / "configs"
    provision_users(db, ["maria", "joao"], "http://host:8000", str(configs))
    assert (configs / "joao.config.json").exists()

    provisioned, revoked = provision_users(db, ["maria"], "http://host:8000", str(configs))

    assert provisioned == []
    assert revoked == ["joao"]
    comments = {k["comment"] for k in db.list_keys()}
    assert comments == {"maria"}
    assert not (configs / "joao.config.json").exists()


def test_provision_does_not_revoke_manual_keys(tmp_path):
    db = Database(str(tmp_path / "test.db"))
    configs = tmp_path / "configs"
    db.add_key("manual-pedro", "PUBPEDRO", comment="pedro")  # provisioned=False
    provisioned, revoked = provision_users(db, ["maria"], "http://host:8000", str(configs))

    assert provisioned == ["maria"]
    assert revoked == []
    comments = {k["comment"] for k in db.list_keys()}
    assert comments == {"maria", "pedro"}


def test_main_without_users_is_noop(tmp_path, monkeypatch):
    monkeypatch.setenv("ELISDB_DB", str(tmp_path / "test.db"))
    monkeypatch.delenv("ELISDB_USERS", raising=False)
    assert main() == 0
    assert Database(str(tmp_path / "test.db")).list_keys() == []


def test_main_without_server_url_fails(tmp_path, monkeypatch):
    monkeypatch.setenv("ELISDB_DB", str(tmp_path / "test.db"))
    monkeypatch.setenv("ELISDB_USERS", "maria")
    monkeypatch.delenv("ELISDB_SERVER_URL", raising=False)
    assert main() == 2


def test_main_provisions_users_from_env(tmp_path, monkeypatch):
    monkeypatch.setenv("ELISDB_DB", str(tmp_path / "test.db"))
    monkeypatch.setenv("ELISDB_USERS", "maria, joao")
    monkeypatch.setenv("ELISDB_SERVER_URL", "http://host:8000")
    monkeypatch.setenv("ELISDB_CONFIGS_DIR", str(tmp_path / "configs"))
    assert main() == 0
    keys = {k["comment"] for k in Database(str(tmp_path / "test.db")).list_keys()}
    assert keys == {"maria", "joao"}
    assert (tmp_path / "configs" / "maria.config.json").exists()
    assert (tmp_path / "configs" / "joao.config.json").exists()