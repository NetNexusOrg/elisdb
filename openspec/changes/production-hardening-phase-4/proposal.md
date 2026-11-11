# Proposal — production-hardening-phase-4

## Why

A auditoria de produção deixou em aberto itens de robustez operacional e de
dependências. As fotos ainda eram lidas inteiras para memória no download, as
dependências não tinham atualizações automáticas nem auditoria no CI, e a
migração do `security-crypto` precisava de uma decisão explícita.

## What Changes

- Servir as fotos com `FileResponse`, sem carregar o ficheiro inteiro para
  memória, mantendo `ETag`, `Last-Modified`, `Range` e `Cache-Control`.
- Adicionar Dependabot semanal para `pip`, `gradle` e `github-actions`.
- Adicionar `pip-audit` não bloqueante ao CI do servidor.
- Documentar o plano de migração do `security-crypto` para Keystore/Tink e
  adiar a implementação para uma change dedicada (risco de perder a chave).
- Confirmar nas docs as regras já implementadas de backups agendados,
  restauro e processo único.

## Capabilities

Nenhuma capability nova ou alterada: são melhorias operacionais internas, sem
alteração de comportamento observável pelos clientes.

## Impact

- `elisdb-server/app/database.py`, `elisdb-server/app/main.py`
- `.github/dependabot.yml`, `.github/workflows/server-ci.yml`
- `elisdb-android/docs/migracao-security-crypto.md`
- Sem alterações de versão, `CHANGELOG`, app Android ou base de dados.
