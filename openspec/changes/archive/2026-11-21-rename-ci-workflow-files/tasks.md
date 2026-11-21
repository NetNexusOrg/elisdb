# Tasks

## 1. Renomear o workflow

- [x] 1.1 `git mv .github/workflows/android-release.yml .github/workflows/android-ci.yml` (sem alterar conteúdo).
- [x] 1.2 Confirmar que `name: Android CI`, triggers, jobs, `needs`, permissões e secrets ficaram intactos.

## 2. Nomes dos jobs

- [x] 2.1 `test`: `Test` → `Android tests`.
- [x] 2.2 `instrumented-suite`: `Instrumented tests (full suite, non-blocking)` → `Instrumented suite (non-blocking)`.
- [x] 2.3 `sync-smoke`: `Sync smoke (release gate)` → `Server sync smoke (release gate)`.
- [x] 2.4 Manter `instrumented` (`Instrumented tests (Room migrations)`) e `release` (`Build & Release`).

## 3. Documentação

- [x] 3.1 Atualizar o caminho em `README.md`.
- [x] 3.2 Atualizar `.opencode/skills/github-actions/SKILL.md` (descrição, caminho e nomes dos jobs).
- [x] 3.3 Atualizar `.opencode/skills/project-context/SKILL.md`.
- [x] 3.4 Atualizar a linha do CI no contexto de `openspec/config.yaml`.

## 4. Verificação

- [x] 4.1 `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/android-ci.yml')); yaml.safe_load(open('.github/workflows/server-ci.yml'))"`.
- [x] 4.2 `grep -rn "android-release" --exclude-dir=.git --exclude-dir=archive .` sem resultados.
- [x] 4.3 `openspec validate rename-ci-workflow-files --strict`.

## Workflow follow-up

- Sincronizar o delta para `openspec/specs/release-pipeline/spec.md`.
- Arquivar a change (commit + push automáticos no fluxo de arquivo).
