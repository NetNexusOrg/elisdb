## Why

O projeto tem dois executáveis em locais assimétricos: a app Android ocupa a pró­pria root (`/home/ricardo/Projectos/elisdb`) enquanto o servidor vive num repositório irmão (`../elisdb-server`). Isto gera caminhos relativos confusos (`../elisdb-server`), faz a root misturar "projeto" com "app", e obriga a tratamentos especiais para editar ficheiros fora da root de planeamento. Reorganizar num único root de projeto com os subdiretórios `elisdb-android/` e `elisdb-server/` estabelece um layout canónico e autocontido.

## What Changes

- Criar `elisdb-android/` na root do projeto e mover para lá todos os ficheiros da app Android (conteúdo atual da root exceto `openspec/` e `.opencode/`).
- Criar `elisdb-server/` na root do projeto e mover para lá o conteúdo do repositório do servidor (`../elisdb-server`).
- Manter `openspec/` (todos os ficheiros OpenSpec) e `.opencode/` na root do projeto.
- Atualizar referências de caminhos na documentação (READMEs, `docs/`) e nos scripts de build (`scripts/build-release.sh`) para o novo layout.
- Mover artefactos runtime/servidor como estão (`elisdb.db`, `photos/`, `.venv/`, caches) para preservar o estado utilizável.
- **BREAKING**: caminhos que apontavam para `../elisdb-server` ou para ficheiros na root da app mudam — `../elisdb-server` passa a `elisdb-server/`, e ficheiros da app na root passam a `elisdb-android/...`.

## Capabilities

### New Capabilities
- nenhuma.

### Modified Capabilities
- nenhuma — esta é uma mudança puramente estrutural/tooling: o comportamento da app e do servidor não muda, e as specs existentes descrevem apenas comportamento (sem referências a caminhos de ficheiros). Conforme as regras spec-driven, a mudança opta por não ter specs (`skip_specs: true`).

## Impact

- Filesystem: a root `/home/ricardo/Projectos/elisdb` passa a ser a root do projeto; a app move-se para `elisdb-android/`, o servidor para `elisdb-server/`.
- Documentação: README.md (root), `docs/*.md` (app), README.md/docs (servidor), `scripts/build-release.sh` — atualização de caminhos.
- Tooling: `allowedEditRoots` do opencode mantém `/home/ricardo/Projectos/elisdb`, que passa a cobrir naturalmente o servidor (antes exigia aprovação especial por estar fora).
- Sem mudanças no comportamento, APIs, dependências ou semântica de sincronização de app/servidor.
- Nenhum dos dois diretórios é um repositório git hoje; não há migração de histórico git (fora do âmbito por decisão do utilizador).