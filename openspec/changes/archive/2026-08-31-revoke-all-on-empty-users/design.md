## Context

O `docker-entrypoint.sh` executava `python -m app.provision` apenas quando
`ELISDB_USERS` era não-vazio. A função `main()` em `provision.py` retornava
0 imediatamente quando a lista de users estava vazia. O resultado: chaves
auto-provisionadas e respetivos configs ficavam no sistema mesmo sem nenhum
utilizador listado.

A função `provision_users()` já tratava corretamente o caso de lista vazia
(revogava todas as chaves `provisioned=1`), mas nunca era chamada nesse
cenário por causa dos dois atalhos.

## Goals / Non-Goals

**Goals:**
- Garantir que `ELISDB_USERS` vazio ou ausente revoga todas as chaves
  auto-provisionadas e remove os respetivos ficheiros de configuração.
- Manter chaves emitidas manualmente via CLI intactas (provisioned=0).

**Non-Goals:**
- Alterar o comportamento de chaves manuais — são da responsabilidade do
  operador.
- Adicionar novos endpoints ou funcionalidades de API.
- Alterar o schema da base de dados.

## Decisions

### 1. Remover guard no entrypoint

**Decisão:** Remover `if [ -n "${ELISDB_USERS:-}" ]` do `docker-entrypoint.sh`
para que `python -m app.provision` execute sempre no arranque.

**Alternativa considerada:** Manter o guard e chamar `provision_users()` diretamente
com lista vazia. Rejeitada porque duplicaria lógica e o entrypoint já era o ponto
de entrada natural.

### 2. Condicionar server_url em main()

**Decisão:** Em `main()`, a exigência de `ELISDB_SERVER_URL` passa a ser
condicional: `if users and not server_url`.

**Rationale:** Quando a lista está vazia, não há nada a provisionar — só revogar.
A URL do servidor não é necessária para revogação. Se for obrigatória mesmo
com lista vazia, o operador teria de definir uma variável desnecessária.

### 3. Não alterar provision_users()

**Decisão:** Não alterar a lógica de `provision_users()`. A função já tratava
corretamente o caso de lista vazia (o loop de revogação nas linhas 49-61
funciona com `desired` vazio).

**Rationale:** A mudança era nos dois pontos de entrada (entrypoint e main),
não na lógica core.

## Risks / Trade-offs

- **Operador removing ELISDB_USERS acidentalmente:** Se o operador remover a
  variável por engano, todas as chaves auto-provisionadas são revogadas no
  próximo arranque. Mitigação: as chaves manuais ficam intactas, e o operador
  pode re-adicionar os users para reprovisionar.

- **ELISDB_SERVER_URL não definido com users:** O `main()` retorna erro 2 e
  o container não arranca. Isto é intencional — sem URL não é possível
  escrever configs para novos users.
