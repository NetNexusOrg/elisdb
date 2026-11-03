# Proposal

## Why

O nome apresentado do workflow, `Android CI / Release`, sugere um workflow separado de release, mas o `Build & Release` é apenas um job dentro do mesmo ficheiro. O nome pode ser mais simples e continuar a refletir que o release faz parte do pipeline.

## What Changes

- Renomear o nome apresentado do workflow de `Android CI / Release` para `Android CI`.
- Atualizar a referência na skill `github-actions`.
- Manter o job `Build & Release`, o trigger de tags `v*` e todo o comportamento existente.

## Capabilities

### Modified Capabilities

- `release-pipeline`: nome apresentado do workflow Android.

## Impact

- `.github/workflows/android-release.yml`
- `.opencode/skills/github-actions/SKILL.md`
