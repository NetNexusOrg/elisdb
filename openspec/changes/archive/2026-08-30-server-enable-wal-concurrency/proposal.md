## Why

O servidor abre uma ligação SQLite nova por cada pedido (`app/database.py:connect`) e, em cada `connect()`, apenas ativa `PRAGMA foreign_keys = ON` — não ativa WAL nem `busy_timeout`, e re-executa o schema + 4 migrações. Num sistema offline-first em que vários dispositivos sincronizam em paralelo, isto produz:

- Erros "database is locked" (HTTP 500) sob escritas concorrentes, porque o modo DELETE default bloqueia a base inteira em cada write.
- Trabalho redundante: schema + migrações corridos em todos os pedidos em vez de uma vez no arranque.

Isto é uma falha de robustez sob a carga exata que o produto é desenhado para suportar.

## What Changes

- Configurar a ligação SQLite com `PRAGMA journal_mode=WAL` e `PRAGMA busy_timeout=<ms>` (ex.: 5000) no arranque/primeira ligação.
- Executar schema + migrações uma única vez no arranque do processo, não em cada `connect()`.
- Manter a opção de ligação por pedido (aceitável para o modo single-process do uvicorn) mas garantir que propriedades WAL/busy_timeout e migrações já estão aplicadas, evitando "database is locked" sob concorrência.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-sync`: nova requirement "Concurrent access safety" — o servidor SHALL configurar o SQLite para acesso concorrente (WAL + busy_timeout) e SHALL aplicar schema/migrações uma vez no arranque; pedidos concorrentes não devem produzir "database is locked".

## Impact
- Servidor: `app/database.py` (função `connect` / inicialização da DB), possivelmente `app/main.py` ou entrypoint para correr migrações no startup.
- Ficheiro de base (`elisdb.db`) passa a usar WAL (cria `-wal`/`-shm`); o `docker-entrypoint.sh`/compose já montam volume de dados, pelo que sobrevive a restart.
- Testes: `test_main.py` (adicionar teste de pushes concorrentes sem "database is locked").
