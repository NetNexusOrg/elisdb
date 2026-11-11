# Design — production hardening do servidor

## Context

O servidor é um único processo Uvicorn com SQLite em WAL, autenticação ed25519
e cache de replay/rate limit em memória. A auditoria de produção encontrou
falhas de segurança e operação nesse modelo. Esta change corrige apenas o que é
necessário para expor o servidor com segurança; não muda o protocolo nem a base
de dados.

## Decisions

### Expiração do replay pelo timestamp assinado

O cache guarda a assinatura com expiração `timestamp + janela + 1`. O `+1`
cobre a comparação inclusiva (`<=`) da frescura. Assim, uma assinatura com
timestamp no futuro continua bloqueada enquanto for aceite, em vez de expirar
300 s após a receção.

### Rate limit antes do corpo

O middleware de limite de corpo passa a ser adicionado antes do middleware de
rate limit, deixando o rate limit como o mais externo. Pedidos acima do limite
recebem 429 sem que o corpo seja lido ou copiado.

### I/O fora do event loop

As consultas de chaves e a escrita de fotos passam por
`starlette.concurrency.run_in_threadpool`. O event loop deixa de bloquear
durante `busy_timeout` do SQLite ou uma escrita de 20 MiB.

### Instância única imposta

`create_app` falha se `WEB_CONCURRENCY` ou `UVICORN_WORKERS` for > 1. O
Dockerfile continua a usar um único processo Uvicorn.

### Deploy sem dados locais e com backup prévio

O rsync exclui bases, credenciais, fotos, `data/` e `.env*`. Se já existe um
contentor a correr, o deploy cria um backup em `/app/data/backups/` e aborta se
falhar. O primeiro deploy não tem nada para copiar e segue sem backup.

## Risks / Trade-offs

- O cache de replay continua em memória e sem limite superior; esta change não
  o redesenha. O rate limit limita a taxa que o pode fazer crescer.
- O backup pré-deploy ocupa espaço no volume; a retenção deve ser operada
  externamente (cron/systemd), documentada como tarefa operacional.
- A regra de instância única é validada por variáveis de ambiente; um operador
  que arranque Uvicorn com `--workers` sem essas variáveis continua a
  contornar a proteção. O Dockerfile documenta e usa um único worker.
