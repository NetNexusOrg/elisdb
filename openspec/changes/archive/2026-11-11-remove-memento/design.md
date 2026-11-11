# Design

## Context

Ver `proposal.md` — Why. O `elisdb-memento/` é um componente autónomo: nenhum
código Android ou do servidor o importa. As referências externas estão no CI
(`server-ci.yml`), em `.gitignore`, em `AGENTS.md`, nas skills do OpenCode, na
configuração do OpenSpec e nas specs `repository-layout`, `memento-migration` e
`memento-import`.

## Goals / Non-Goals

**Goals:**

- Remover o componente e todas as referências ativas, sem tocar em código da app
  ou do servidor.
- Remover `ELIS.csv` e os artefactos locais (caches), conforme autorização do
  proprietário dos dados.
- Deixar o repositório sem menções ao memento fora do histórico que o utilizador
  pediu para limpar.

**Non-Goals:**

- Alterar o schema, as APIs ou os dados já migrados no servidor.
- Reverter clientes importados ou revogar automaticamente a chave
  `memento-import` (fica como ação operacional opcional).

## Decisions

- **Ordem: apagar ficheiros e referências antes de arquivar a change.** O
  arquivo aplica os deltas e elimina as capabilities vazias; se o diretório já
  não existir, a validação e o arquivo continuam a funcionar.
- **Remover as capabilities com `## REMOVED Requirements`**, em vez de editar as
  specs principais à mão. É o mecanismo suportado e mantém o histórico da change.
- **Limpar o arquivo histórico conforme pedido explícito.** As duas changes
  inteiramente dedicadas ao memento são apagadas; nas restantes apenas se
  retiram as menções incidentais, preservando o registo do trabalho não
  relacionado.
- **CI:** remover o step de testes do memento e os filtros de caminho. O job
  `test` continua a correr servidor e bot; não há substituição.
- **Dados reais:** `ELIS.csv` é ignorado pelo Git e não tem recuperação após
  eliminação. A decisão foi confirmada pelo utilizador; não é feito backup.
- **Sem alterações de dependências, versões ou `CHANGELOG.md`.** Não é um
  release.

## Risks / Trade-offs

- [Perda definitiva do export `ELIS.csv`] → Decisão explícita do utilizador;
  os dados já foram migrados para o servidor.
- [Referências residuais em documentação histórica] → Verificação final com
  `git grep -i "memento\|ELIS.csv"` e revisão manual dos resultados.
- [CI deixar de validar os scripts] → Os scripts deixam de existir; não há
  comportamento a validar.
- [Confusão por o arquivo histórico ficar incompleto] → As changes apagadas são
  integralmente sobre o memento; as restantes mantêm o contexto e só perdem as
  menções ao componente removido.
