## Why

O `elisdb-memento/` foi uma migração pontual dos dados do Memento para o ElisDB. A migração já foi concluída e o componente deixou de ser necessário: nenhum código da app ou do servidor o usa, mas continuam a existir scripts, testes, documentação, uma referência no CI e duas capabilities OpenSpec a manter. Removê-lo reduz a superfície do repositório e evita trabalho de manutenção sem valor.

## What Changes

- Remover o componente `elisdb-memento/`: scripts `migrar_memento.py`, `importar_memento.py`, o wrapper `importar`, testes, README e CHANGELOG.
- Apagar o export real `ELIS.csv` (ignorado pelo Git), com autorização explícita do proprietário dos dados.
- Remover do CI o trigger por `elisdb-memento/**` e o step que corria os testes de migração.
- Remover as entradas de `.gitignore` específicas do memento.
- Atualizar `AGENTS.md`, `.opencode/skills/`, `openspec/config.yaml` e o proposal da change ativa `telegram-release-announcements` para deixarem de referir o componente.
- Remover as capabilities `memento-migration` e `memento-import` e retirar o memento dos requisitos de layout e de changelogs por componente.
- Limpar o histórico OpenSpec: apagar as changes arquivadas dedicadas ao memento e retirar menções incidentais nas restantes changes arquivadas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `repository-layout`: a lista de componentes e de changelogs por componente deixa de incluir `elisdb-memento/`.
- `memento-migration`: a capability é removida na íntegra.
- `memento-import`: a capability é removida na íntegra.

## Impact

- `elisdb-memento/`: diretório eliminado (incluindo `ELIS.csv`, caches e artefactos locais).
- `.github/workflows/server-ci.yml`: triggers e step de testes do memento removidos.
- `.gitignore`, `AGENTS.md`, `.opencode/skills/{project-context,database,security-review,git-workflow,github-actions,openspec-archive-change}/SKILL.md`, `openspec/config.yaml`.
- `openspec/specs/repository-layout/spec.md`: requisitos ajustados; `openspec/specs/memento-migration/` e `memento-import/` eliminadas após arquivar.
- `openspec/changes/archive/2026-08-16-memento-data-migration/` e `openspec/changes/archive/2026-11-06-importar-clientes-memento/`: eliminadas; menções incidentais noutras changes arquivadas limpas.
- Sem alterações ao código Android, ao servidor, às APIs ou às dependências.
