# Tasks

## 1. Servidor

- [x] 1.1 Devolver 503 em `sqlite3.OperationalError` com handler global.
- [x] 1.2 Adicionar `pids_limit`, `mem_limit` e rotação de logs ao `compose.yaml`.
- [x] 1.3 Validar o YAML e correr a suite `pytest` (153 testes).
- [ ] 1.4 Podar `push_idempotency` por idade/tamanho na manutenção existente.
- [ ] 1.5 Separar o limite de body de push do limite de foto.
- [ ] 1.6 Aplicar limite máximo a `pull` e `history` sem `limit`.
- [ ] 1.7 Servir fotos em streaming (`FileResponse`) com `ETag`/`Cache-Control`.
- [ ] 1.8 Adicionar `/ready` com escrita na BD e verificação do diretório de fotos.

## 2. App Android

- [x] 2.1 Apagar snapshots locais em `resetSyncState()`.
- [x] 2.2 Correr testes unitários e compilar os testes instrumentados.
- [ ] 2.3 Decidir a política de HTTP cleartext (manter apenas para LAN/loopback).
- [ ] 2.4 Decidir `applicationId`/`REQUEST_INSTALL_PACKAGES` caso se mire a Play Store.
- [ ] 2.5 Migrar de `security-crypto` para Keystore/Tink.

## 3. Fecho

- [x] 3.1 Validar a change com `openspec validate --strict`.
- [ ] 3.2 Arquivar após revisão e commit aprovado pelo utilizador.

> Commits e push apenas com confirmação explícita do utilizador.
