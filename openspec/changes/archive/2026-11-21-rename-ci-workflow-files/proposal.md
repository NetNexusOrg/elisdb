# Proposal

## Why

O ficheiro `.github/workflows/android-release.yml` chama-se `-release`, mas é o CI
inteiro do Android (testes unitários, lint, build debug, migrações Room, suite
instrumentada e sync smoke) — o release é apenas um job que corre em tags. O nome
sugere um workflow separado que não existe. Ao mesmo tempo, os nomes dos jobs na
lista de checks dos pull requests são assimétricos e ambíguos (`Test` ao lado de
`Server tests`, nomes muito longos e um job do servidor dentro do workflow Android
chamado apenas `Sync smoke`).

## What Changes

- Renomear o ficheiro `.github/workflows/android-release.yml` para `android-ci.yml`,
  mantendo o nome apresentado `Android CI` e todo o comportamento.
- Renomear os jobs visíveis para identificarem componente e propósito:
  - `Test` → `Android tests`
  - `Instrumented tests (full suite, non-blocking)` → `Instrumented suite (non-blocking)`
  - `Sync smoke (release gate)` → `Server sync smoke (release gate)`
- Atualizar as referências ao ficheiro na documentação (`README.md`, skills
  `github-actions` e `project-context`, contexto do OpenSpec).
- **Não** altera triggers, jobs, permissões, secrets, versões de actions nem os
  nomes apresentados dos workflows (`Android CI` / `Server CI`), que são
  dependências do script de deploy e da spec `release-pipeline`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `release-pipeline`: o ficheiro do workflow Android passa a ser identificado por
  nome consistente com o nome apresentado, e os jobs do CI passam a ter nomes
  inequívocos na lista de checks.

## Impact

- `.github/workflows/android-release.yml` → `.github/workflows/android-ci.yml`
- `README.md`
- `.opencode/skills/github-actions/SKILL.md`
- `.opencode/skills/project-context/SKILL.md`
- `openspec/config.yaml` (linha de contexto sobre o CI)
- Sem impacto em `elisdb-server/scripts/deploy-server.sh` (depende do nome
  apresentado `Server CI`, inalterado) nem nas tags/releases existentes.
