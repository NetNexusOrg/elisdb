"""P3 schema migration checks for audit actions and push idempotency."""

import sqlite3

from elisdb.database import Database


def test_audit_action_migration_preserves_rows_and_accepts_delete(tmp_path):
    path = str(tmp_path / "legacy.db")
    conn = sqlite3.connect(path)
    conn.execute(
        """
        CREATE TABLE client_audit (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cliente_id TEXT NOT NULL,
            key_id TEXT NOT NULL,
            username TEXT,
            action TEXT NOT NULL CHECK(action IN ('create','edit')),
            ts INTEGER NOT NULL,
            details TEXT
        )
        """
    )
    conn.execute(
        "INSERT INTO client_audit (cliente_id, key_id, username, action, ts) "
        "VALUES ('1', 'k1', 'Maria', 'edit', 1000)"
    )
    conn.commit()
    conn.close()

    db = Database(path, fotos_dir=str(tmp_path / "photos"))
    db.init()
    conn = db._conn()
    try:
        sql = conn.execute(
            "SELECT sql FROM sqlite_master WHERE type='table' AND name='client_audit'"
        ).fetchone()["sql"]
        assert "'delete'" in sql
        assert conn.execute("SELECT COUNT(*) FROM client_audit").fetchone()[0] == 1
        conn.execute(
            "INSERT INTO client_audit (cliente_id, key_id, username, action, ts) "
            "VALUES ('1', 'k1', 'Maria', 'delete', 1001)"
        )
        conn.commit()
    finally:
        conn.close()


def test_fresh_schema_has_push_idempotency_table(tmp_path):
    db = Database(str(tmp_path / "fresh.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    conn = db._conn()
    try:
        names = {
            row["name"]
            for row in conn.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ).fetchall()
        }
    finally:
        conn.close()
    assert "push_idempotency" in names
