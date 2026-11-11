# Tasks

## 1. Remover o componente

- [x] 1.1 Apagar `elisdb-memento/` (scripts, testes, docs, wrapper, `ELIS.csv` e caches) e confirmar que o diretório já não existe
- [x] 1.2 Remover do `.github/workflows/server-ci.yml` os filtros `elisdb-memento/**` e o step de testes do memento; confirmar que o YAML continua válido e que não há menções no ficheiro

## 2. Limpar referências ativas

- [x] 2.1 Remover as entradas do memento em `.gitignore`, `AGENTS.md`, `openspec/config.yaml` e nas skills `project-context`, `database`, `security-review`, `git-workflow`, `github-actions` e `openspec-archive-change`; confirmar com `git grep -i "memento\|ELIS.csv"` fora do histórico
- [x] 2.2 Remover a comparação com os testes do memento em `openspec/changes/telegram-release-announcements/proposal.md`

## 3. Limpar o histórico OpenSpec

- [x] 3.1 Apagar as changes arquivadas `2026-08-16-memento-data-migration` e `2026-11-06-importar-clientes-memento`
- [x] 3.2 Retirar as menções incidentais ao memento nas restantes changes arquivadas (`reorganize-monorepo`, `component-changelogs`, `setup-git-github-ci-cd`, `multiplas-tags-clientes`)

## 4. Specs e arquivo

- [x] 4.1 Validar a change com `openspec validate remove-memento --strict`
- [x] 4.2 Arquivar a change e confirmar que `openspec/specs/memento-migration/` e `memento-import/` desapareceram e que `repository-layout` já não menciona o memento
- [x] 4.3 Confirmar que `git grep -i "memento\|ELIS.csv"` não devolve resultados fora do arquivo da change e que `git status` não tem ficheiros inesperados ou segredos

## Workflow follow-up

- Fazer commit da limpeza após confirmação do utilizador.
