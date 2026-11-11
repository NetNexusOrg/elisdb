# Tasks — production hardening do servidor

## 1. Segurança P0

- [x] 1.1 Expirar o cache de replay pelo timestamp assinado e aceitar `timestamp` em `record_replay`/`consume_replay`.
- [x] 1.2 Passar `timestamp` nas duas chamadas de `consume_replay` em `require_auth`.
- [x] 1.3 Tornar o rate limit o middleware mais externo, antes do limite de corpo.
- [x] 1.4 Mover as consultas de chaves e a escrita de fotos para `run_in_threadpool`.
- [x] 1.5 Falhar no arranque com mais de um worker (`WEB_CONCURRENCY`/`UVICORN_WORKERS`).
- [x] 1.6 Adicionar teste de regressão para assinatura com timestamp futuro.
- [x] 1.7 Correr a suite completa do servidor (`python -m pytest -q`).

## 2. Operação de deploy

- [x] 2.1 Excluir `*.db`, `*.db-*`, `*.config.json`, `photos/`, `data/`, `.env*` e logs do rsync.
- [x] 2.2 Criar backup pré-deploy quando há contentor a correr e abortar se falhar.
- [x] 2.3 Validar o script com `bash -n`.
- [x] 2.4 Documentar backups agendados (cron/systemd) e retenção em `elisdb-server/docs/backup.md`.
- [x] 2.5 Documentar a regra de instância única no README/doc de deploy.

## 3. Fecho

- [x] 3.1 Validar a change com `openspec validate production-hardening-server --strict`.
- [ ] 3.2 Arquivar após revisão.

## Workflow

- Commit e push só depois de confirmação explícita do utilizador.
