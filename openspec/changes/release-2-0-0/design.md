# Design — release 2.0.0

## Revogação de chaves

- **Não apagar linhas**: `client_audit.key_id` referencia chaves antigas.
  Revogar é `UPDATE keys SET revoked_at = <epoch>`, preservando o histórico.
- Migração `_migrate_keys_revoked` idempotente, executada em `_apply_schema`.
- `get_all_public_keys` e `get_key_by_public_key` filtram
  `revoked_at IS NULL`; uma chave revogada deixa de autenticar (401).
- CLI `python -m app.keys revoke <utilizador>` revoga todas as chaves ativas
  desse utilizador; a operação é idempotente.

## Upgrade da VPS

- O volume `elisdb-data` nunca é removido ou recriado. O deploy não copia
  `data/`, `*.db` nem `*.config.json`.
- Backup obrigatório com `app.backup create`, verificação e cópia **fora** da
  VPS antes de qualquer migração.
- Ensaio de restauro obrigatório antes do upgrade.
- Sem `docker compose down -v`; rollback repõe o commit/imagem anterior e, se
  necessário, o backup verificado.

## Release Android

- Versão única em `gradle.properties`; a tag `v2.0.0` tem de coincidir com
  `app.versionName` (gate do CI).
- Instrução explícita para sincronizar e confirmar zero alterações pendentes
  antes de desinstalar a app antiga.

## Riscos

- Chave do Keystore inválida em dispositivos antigos → reimportar credenciais.
- Erro de migração na VPS → restaurar o backup verificado e a imagem anterior.
- Chaves antigas ainda válidas se a revogação não for executada; o runbook
  coloca-a depois de as novas credenciais estarem testadas.
