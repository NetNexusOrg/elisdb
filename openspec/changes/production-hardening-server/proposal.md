# Proposal — production hardening do servidor

## Why

A auditoria de prontidão para produção encontrou riscos no servidor que não
podem ficar abertos fora de uma LAN de confiança:

- o cache de replay expirava 300 s após a receção, mas aceita timestamps até
  300 s no futuro — um pedido capturado podia ser reutilizado durante ~5 min
  depois de sair do cache (`app/auth.py`);
- o limite de corpo era o middleware mais externo e bufferizava até 20 MiB
  antes do rate limit e da autenticação, permitindo DoS não autenticado
  (`app/main.py`);
- SQLite e escrita de fotos corriam no event loop em `async` endpoints,
  bloqueando todos os pedidos durante um upload lento;
- a assunção de processo único (cache de replay e rate limit em memória) não
  era imposta;
- o deploy não fazia backup antes das migrações e o rsync podia copiar
  `*.db`, `*.config.json`, `photos/` e `data/` da máquina de desenvolvimento
  para o servidor.

## What Changes

- Expirar o cache de replay pelo timestamp assinado, não pela hora de receção.
- Tornar o rate limit o middleware mais externo, antes de qualquer leitura de
  corpo.
- Mover as chamadas síncronas a SQLite e a escrita de fotos para threadpool.
- Falhar no arranque quando `WEB_CONCURRENCY`/`UVICORN_WORKERS` > 1.
- Excluir dados e segredos locais do rsync e criar um backup pré-deploy quando
  já existe um contentor a correr, abortando o deploy se o backup falhar.
- Documentar a operação de backups agendados e a regra de instância única.

## Capabilities

### Modified Capabilities

- `server-auth`: a proteção anti-replay passa a cobrir todo o período em que o
  timestamp assinado continua válido.
- `server-rate-limiting`: o limite passa a ser aplicado antes de ler ou
  bufferizar o corpo do pedido.

### New Capabilities

Nenhuma.

## Impact

- `elisdb-server/app/auth.py`, `elisdb-server/app/main.py`,
  `elisdb-server/tests/test_auth.py`, `elisdb-server/scripts/deploy-server.sh`.
- Sem alterações ao protocolo, à base de dados, ao Android ou à versão.
- Nenhum segredo ou dado real é adicionado ao repositório.
