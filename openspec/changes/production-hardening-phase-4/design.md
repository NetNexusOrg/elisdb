# Design — production-hardening-phase-4

## Decisions

### Fotos em streaming

`Database.get_photo_path()` devolve `(mime, caminho)` sem ler o ficheiro.
O endpoint usa `FileResponse`, que fornece `Content-Length`, `Last-Modified`,
`ETag`, `Range` e respostas condicionais. Mantém-se `nosniff` e
`Content-Disposition: attachment`, e acrescenta-se
`Cache-Control: private, max-age=86400`.

### Dependências

Dependabot abre PRs semanais para `pip` (`/elisdb-server`), `gradle`
(`/elisdb-android`) e `github-actions` (`/`), limitados a 5 PRs por ecossistema.
`pip-audit` corre no CI do servidor como passo informativo
(`continue-on-error`), para não bloquear por vulnerabilidades transitivas sem
correção disponível.

### `security-crypto`

A migração para Keystore/Tink não é feita neste lote. O armazenamento atual
guarda a seed ed25519 e uma migração mal testada pode perder a credencial do
utilizador. O plano (formato versionado, leitura verificada, testes de upgrade)
fica em `elisdb-android/docs/migracao-security-crypto.md` para uma change
dedicada.

## Risks

- `FileResponse` depende de o ficheiro continuar no caminho devolvido; o registo
  e o ficheiro são verificados antes da resposta.
- Dependabot pode abrir PRs que exijam intervenção manual; o limite reduz ruído.
- `pip-audit` não bloqueante pode passar despercebido; o comentário no workflow
  e o Dependabot compensam.
