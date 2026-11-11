# Design

## Context

Esta fase continua o hardening iniciado nas changes
`production-hardening-server` e `production-hardening-android`. O âmbito é
operacional: tratamento de contenção SQLite, limites do contentor e retenção de
snapshots locais. Não altera o contrato da API nem o esquema da base de dados.

## Decisions

### 503 em vez de 500 para contenção SQLite

`busy_timeout=5000` faz a escrita esperar, mas sob contenção prolongada o
SQLite levanta `OperationalError`. Um handler global devolve 503 com
`{"detail": "database busy, retry later"}` e regista um warning, permitindo ao
cliente re-tentar sem interpretar como falha definitiva.

### Limites do contentor

`pids_limit: 256` e `mem_limit: 1g` limitam o impacto de pedidos anómalos ou
fugas; a rotação json-file (`10m` × 3) impede crescimento sem limite dos logs.
O `read_only` não é ativado porque o entrypoint precisa de escrever no volume e
em diretórios temporários.

### Limpeza de snapshots

Os snapshots contêm a base Room e as fotos completas. Pertencem ao servidor
anterior; ao trocar ou remover a configuração, `resetSyncState()` passa a
chamá-los a apagar. A operação corre em `Dispatchers.IO`.

## Risks / Trade-offs

- [Limite de memória de 1 GB] Pode ser baixo para um volume grande de fotos em
  transferências concorrentes; ajustar por ambiente se necessário.
- [503] Clientes antigos tratam qualquer erro como falha; o cliente Android
  atual re-tenta em falhas transitórias.
- [Limpeza de snapshots] Elimina a possibilidade de recuperação após trocar de
  servidor; os snapshots deixam de fazer sentido quando a configuração muda.

## Decisões finais (fase 3)

- **1.5:** o limite do body passa a depender do endpoint: `/api/push` usa
  `ELISDB_MAX_PUSH_BYTES` (64 MiB por omissão) e os restantes usam o limite de
  foto. Assim um push grande deixa de colidir com o limite de 20 MiB.
- **1.6:** `pull` aceita no máximo 1000 registos por pedido e `history` devolve
  no máximo 200 entradas quando o cliente não pede uma página.
- **1.7:** as fotos passam a trazer `ETag` e `Cache-Control`; o streaming com
  `FileResponse` fica adiado porque obriga a expor o caminho do ficheiro na
  camada de base de dados.
- **2.3:** o HTTP cleartext mantém-se para LAN/loopback; o HTTPS é terminado no
  nginx-proxy-manager, como já documentado.
- **2.4:** a app não será publicada na Play Store; não há trabalho de
  `applicationId`/política de instalação.
- **2.5:** a migração de `security-crypto` fica adiada. Migrar o armazenamento
  encriptado sem perder credenciais exige uma change própria e testes de
  upgrade, pelo que não se mistura com este hardening.
