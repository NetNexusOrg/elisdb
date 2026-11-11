# Tasks — release 2.0.0

## 1. Servidor

- [x] 1.1 Adicionar `revoked_at` com migração idempotente.
- [x] 1.2 Ignorar chaves revogadas na autenticação.
- [x] 1.3 Adicionar `app.keys revoke <utilizador>` e atualizar a usage.
- [x] 1.4 Adicionar testes de revogação/filtragem/migração.
- [x] 1.5 Correr a suite completa do servidor (156 testes).

## 2. Operação

- [x] 2.1 Escrever o runbook `docs/upgrade-2.0.0.md`.
- [x] 2.2 Documentar backup verificado fora da VPS, ensaio de restauro e rollback.
- [x] 2.3 Documentar emissão de novas chaves e revogação das antigas.

## 3. App e fecho

- [x] 3.1 Subir `app.versionCode=20` / `app.versionName=2.0.0`.
- [x] 3.2 Atualizar `BuildConfigVersionTest.kt`.
- [x] 3.3 Adicionar a secção `[2.0.0]` ao `CHANGELOG.md`.
- [x] 3.4 Validar a change com `openspec validate --strict`.
- [ ] 3.5 Criar a tag `v2.0.0` (produção; requer confirmação explícita).
- [ ] 3.6 Commitar e arquivar (requer confirmação explícita).

> Commits, push, tag e deploy só com confirmação explícita do utilizador.
