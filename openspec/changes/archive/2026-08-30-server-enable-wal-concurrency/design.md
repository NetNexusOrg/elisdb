## Context

`app/database.py:connect` (`app/database.py:132-144`) abre uma ligação SQLite e ativa apenas `PRAGMA foreign_keys = ON`; re-executa `SCHEMA` + 4 migrações (`PRAGMA table_info`/ALTER) em cada chamada. Em concorrência (vários dispositivos a sincronizar), o modo DELETE default bloqueia a base em writes e produz "database is locked" (HTTP 500). O uvicorn corre tipicamente single-process com threadpool, mas pedidos de dispositivos diferentes sobrepõem-se.

## Goals / Non-Goals

**Goals:**
- Ativar WAL + `busy_timeout` nas ligações.
- Correr schema + migrações uma vez no arranque.
- Eliminar "database is locked" sob concorrência.

**Non-Goals:**
- Não mudar para um motor de DB diferente (mantém-se sqlite3).
- Não introduzir pool de ligações multi-processo (o servidor é single-process).

## Decisions

**D1: Aplicar PRAGMAs de concorrência na ligação**
Em `connect` (ou num inicializador chamado uma vez), executar `PRAGMA journal_mode=WAL` e `PRAGMA busy_timeout=5000` (ou similar). WAL permite leitores concorrentes com um writer e reduz locking; `busy_timeout` faz a ligação esperar em vez de falhar de imediato.

**D2: Migrações uma vez no startup**
Extrair a execução de `SCHEMA` + migrações para uma função `init_db()` corrida uma única vez no arranque do processo (em `app/main.py` via lifespan/startup, ou no `docker-entrypoint.sh`/entrypoint Python). `connect` passa a assumir que o schema já existe (mantém `PRAGMA foreign_keys=ON` por ligação, que é seguro e barato).

**D3: Manter ligação por pedido (ou partilhada)**
O modo single-process tolera uma ligação por pedido desde que WAL + busy_timeout + migrações já aplicadas. Não é necessário pool.

## Risks / Trade-offs
- [WAL cria ficheiros `-wal`/`-shm`] → o volume de dados (compose) já é persistente; sobrevive a restart. O `compose.yaml` monta o diretório de dados.
- [busy_timeout muito alto pode segurar pedidos] → 5000ms é razoável para esta carga; ajustável.
- [init_db no startup adia o primeiro pedido] → custo único e pequeno.

## Migration Plan
- Sem migração de dados de utilizador. A base existente é convertida para WAL automaticamente no primeiro `PRAGMA journal_mode=WAL`. Recomendar backup antes do deploy.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
