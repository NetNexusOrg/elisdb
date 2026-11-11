# Proposal — release 2.0.0

## Porquê

A próxima release é a **2.0.0** e o servidor da `vps01` vai ser atualizado para a
versão mais recente. Não há requisito de compatibilidade com a app ou o servidor
antigos: as credenciais são substituídas e os utilizadores reinstalam a app.

O requisito crítico é **não perder a base de dados nem as fotos existentes na
VPS**. A release também precisa de revogar as chaves antigas sem partir as
referências de auditoria (`client_audit.key_id`).

## O que muda

- Servidor: coluna `keys.revoked_at` (migração idempotente), autenticação ignora
  chaves revogadas, comando `python -m app.keys revoke <utilizador>`.
- Operação: runbook `docs/upgrade-2.0.0.md` com backup verificado fora da VPS,
  ensaio de restauro, upgrade sem tocar no volume e rollback.
- App: `versionCode=20`, `versionName=2.0.0`, secção `[2.0.0]` no `CHANGELOG`.
- Sem trabalho de compatibilidade retroativa.

## Capabilities

Nenhuma capability alterada (`skip_specs`): é uma operação de release e
revogação, não uma mudança de comportamento funcional.

## Impacto

- `elisdb-server/app/database.py`, `elisdb-server/app/keys.py`,
  `elisdb-server/tests/test_database.py`, `elisdb-server/docs/upgrade-2.0.0.md`.
- `elisdb-android/gradle.properties`, `CHANGELOG.md`,
  `BuildConfigVersionTest.kt`.
- Sem alterações ao protocolo, ao schema de clientes/fotos ou às definições da app.
