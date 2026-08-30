## Context

`demo.config.json` no `elisdb-server/` contém um `private_seed` ed25519 real e funcional, commitado no repo. A `server-key-provisioning` emite configs com seeds reais em runtime; estes não devem estar no VCS. Não há `.gitignore` a cobrir ficheiros de config gerados.

## Goals / Non-Goals

**Goals:**
- Remover o segredo vivo do repo.
- Fornecer um exemplo não-funcional para documentação.
- Garantir que seeds reais ficam gitignorados.

**Non-Goals:**
- Não alterar a issue/provisioning flow (já gera seeds em runtime).
- Não forçar rotação automática de chaves (fica a cargo do operador se o seed estiver comprometido).

## Decisions

**D1: Remover `demo.config.json` do repo**
`git rm` (ou remover do working tree e adicionar ao histórico de remoção) de `demo.config.json`. Se o seed já esteve na allowlist, documentar revogação via `python -m app.keys` (remove pelo id).

**D2: Exemplo placeholder `demo.config.example.json`**
Novo ficheiro com `server_url` exemplo e sem `private_seed` (ou com campo marcado claramente como a preencher). Importá-lo não autentica.

**D3: `.gitignore` cobre configs com seeds**
Adicionar padrão (ex.: `*.config.json` gerado, ou `configs/`) ao `.gitignore` do servidor, mantendo o exemplo trackeado. Cuidado para não ignorar o `demo.config.example.json`.

## Risks / Trade-offs
- [Remover `demo.config.json` quebra quem o usava] → é um demo; documentar que cada utilizador deve emitir o seu próprio (`python -m app.keys issue`).
- [Seed já comprometido] → recomendar revogar na allowlist e reemitir.

## Migration Plan
- Sem migração de dados. Apenas ficheiros e `.gitignore`.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
