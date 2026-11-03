# Proposal

## Why

O monorepo tem quatro componentes (`elisdb-android/`, `elisdb-server/`,
`elisdb-telegram-bot/`, `elisdb-memento/`), mas só a app Android mantém um
`CHANGELOG.md`. O histórico de mudanças do servidor, do bot Telegram e da
migração Memento não tem um local canónico, o que torna difícil perceber o que
mudou em cada componente sem ler o histórico de commits.

O `CHANGELOG.md` da app Android é específico da app e é lido pelo workflow
`.github/workflows/android-release.yml` para gerar as notas de release. Os
outros componentes não devem partilhar esse ficheiro: precisam do seu próprio
registo, na raiz de cada componente.

## What Changes

- Criar `elisdb-server/CHANGELOG.md`, `elisdb-telegram-bot/CHANGELOG.md` e
  `elisdb-memento/CHANGELOG.md`.
- Cada changelog começa em `## [Unreleased]`, sem inventar versões ou histórico
  de releases que não existam.
- O changelog do servidor indica que a versão de release vive em
  `elisdb-server/VERSION`.
- O `elisdb-android/CHANGELOG.md` fica inalterado e continua a ser a fonte das
  notas de release Android.
- Adicionar um requisito à capability `repository-layout`: cada componente
  mantém o seu próprio `CHANGELOG.md` na raiz do componente.
- Não criar um changelog na raiz do repositório.

## Capabilities

### New Capabilities
<!-- nenhuma -->

### Modified Capabilities
- `repository-layout`: novo requisito que fixa um `CHANGELOG.md` por
  componente, com cenário que preserva as notas de release Android a partir de
  `elisdb-android/CHANGELOG.md`.

## Impact

- **Documentação**: novos `elisdb-server/CHANGELOG.md`,
  `elisdb-telegram-bot/CHANGELOG.md` e `elisdb-memento/CHANGELOG.md`.
- **Specs**: `openspec/specs/repository-layout/spec.md` passa a cobrir os
  changelogs por componente.
- **Sem alterações**: `elisdb-android/CHANGELOG.md`, workflow de release,
  código da app, servidor, bot ou scripts de migração.
- **Segredos/dados**: nenhum token, segredo, dado de cliente ou conteúdo de
  `elisdb-telegram-bot/Telegram-Bot-Info.txt` entra nos changelogs.
