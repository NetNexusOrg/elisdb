# Tasks

## 1. Changelogs por componente

- [x] 1.1 Confirmar que `elisdb-android/CHANGELOG.md` existe e fica inalterado.
- [x] 1.2 Criar `elisdb-server/CHANGELOG.md` com `## [Unreleased]` e referência a `elisdb-server/VERSION`.
- [x] 1.3 Criar `elisdb-telegram-bot/CHANGELOG.md` com `## [Unreleased]`.
- [x] 1.4 Confirmar que não foi criado nenhum changelog na raiz e que nenhum segredo, token ou dado real foi incluído.

## 2. Spec e validação

- [x] 2.1 Adicionar o requisito de changelogs por componente ao delta de `repository-layout`.
- [x] 2.2 Sincronizar o delta para `openspec/specs/repository-layout/spec.md`.
- [x] 2.3 Validar a change e a spec com `openspec validate --strict`.
- [x] 2.4 Confirmar que `.github/workflows/android-release.yml` continua a ler `elisdb-android/CHANGELOG.md` e não é alterado.
- [x] 2.5 Correr `git diff --check` e rever o diff antes de commitar.

## 3. Arquivar

- [x] 3.1 Arquivar a change em `openspec/changes/archive/2026-11-03-component-changelogs`.
- [x] 3.2 Confirmar `openspec list --json` sem changes ativas e `git status` limpo.
