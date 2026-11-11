# Tasks

## 1. Servidor

- [x] 1.1 Devolver 503 em `sqlite3.OperationalError` com handler global.
- [x] 1.2 Adicionar `pids_limit`, `mem_limit` e rotação de logs ao `compose.yaml`.
- [x] 1.3 Validar o YAML e correr a suite `pytest` (153 testes).
- [x] 1.4 Podar `push_idempotency` por idade/tamanho na manutenção existente.
- [x] 1.5 Separar o limite de body de push (`ELISDB_MAX_PUSH_BYTES`, 64 MiB) do limite de foto.
- [x] 1.6 Aplicar limite máximo a `pull` (1000 registos) e `history` (200 entradas) sem `limit`.
- [x] 1.7 Adicionar `ETag`/`Cache-Control` às fotos (streaming `FileResponse` adiado).
- [x] 1.8 Adicionar `/ready` com verificação da BD e do diretório de fotos.

## 2. App Android

- [x] 2.1 Apagar snapshots locais em `resetSyncState()`.
- [x] 2.2 Correr testes unitários e compilar os testes instrumentados.
- [x] 2.3 Manter HTTP cleartext para LAN/loopback; o HTTPS fica a cargo do nginx-proxy-manager.
- [x] 2.4 Não publicar na Play Store; `applicationId` e `REQUEST_INSTALL_PACKAGES` mantêm-se.
- [x] 2.5 Adiar a migração de `security-crypto` (risco de perder credenciais); decisão registada no design.

## 3. Fecho

- [x] 3.1 Validar a change com `openspec validate --strict`.
- [x] 3.2 Arquivar após revisão e commit aprovado pelo utilizador.

> Commits e push apenas com confirmação explícita do utilizador.
