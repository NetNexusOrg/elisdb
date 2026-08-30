## 1. WAL e busy_timeout

- [x] 1.1 Em `app/database.py`, ativar `PRAGMA journal_mode=WAL` e `PRAGMA busy_timeout=5000` na ligação (ou num inicializador único)
- [x] 1.2 Manter `PRAGMA foreign_keys=ON` por ligação

## 2. Migrações no arranque

- [x] 2.1 Extrair execução de `SCHEMA` + migrações para `init_db()` corrida uma vez no startup do processo (em `app/main.py` lifespan/startup)
- [x] 2.2 `connect` deixa de re-executar schema/migrações (assume DB já inicializada)

## 3. Testes e verificação

- [x] 3.1 Em `test_main.py`, adicionar teste de pushes concorrentes (vários dispositivos) sem "database is locked"
- [x] 3.2 Correr `pytest`; confirmar que a base passa a WAL (`elisdb.db-wal`/`elisdb.db-shm` aparecem)
