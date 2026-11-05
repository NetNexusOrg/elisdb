## Context

Ver `proposal.md` — Why. O campo `tag` já existe como `TEXT` em `clientes`
(servidor e Room) e viaja como string no push/pull. Não há tabela `cliente_tags`
nem migração pendente. A app filtra a lista e o mapa em memória.

## Goals / Non-Goals

**Goals**

- Suportar várias tags por cliente sem novas tabelas nem migrações.
- Manter `tag` como string compatível com apps/servidores antigos.
- Normalizar e limitar as tags de forma consistente nos dois lados.
- Mostrar todas as tags e filtrar por uma tag de cada vez.

**Non-Goals**

- Tabela relacional `cliente_tags` (considerada e rejeitada nesta iteração).
- Filtro multi-tag (AND/OR).
- Gestão/renomeação centralizada de tags.
- Incluir tags na pesquisa livre.

## Decisions

### `tag` como string separada por `;`

Guardar `PREMIUM;GOLD;ROTA 21` no campo `tag` existente. Alternativa rejeitada:
tabela `cliente_tags` — exigiria nova tabela, migração, relação Room e alterações
no contrato de sync. A app já filtra em memória, pelo que não precisa de SQL por
tag.

### Normalização

- `trim` em cada tag;
- maiúsculas (`Locale.ROOT` / `str.upper()`);
- remover vazias e duplicados (comparação sem caixa);
- ordenar alfabeticamente antes de juntar com `;`.
A ordenação garante um valor canónico estável para auditoria e comparação.

### Limites

- 10 tags por cliente;
- 100 caracteres por tag;
- 500 caracteres no total (limite do campo no servidor).
O formulário impede adicionar acima destes limites; o servidor valida e rejeita
o registo inválido.

### Sem migração

A coluna `tag TEXT` já existe. O servidor só sobe o limite de 100 para 500 e
normaliza no `ClienteIn`. O Room mantém a versão 11 e a entidade `Cliente`
inalterada. Isto elimina o risco de migração com a FK de fotos.

### Compatibilidade

- App antiga envia uma tag → o servidor normaliza como conjunto de uma tag.
- App nova envia `A;B` → app antiga mostra a string inteira como uma etiqueta,
  sem quebrar.
- O servidor 1.4.0 rejeita strings >100; por isso a ordem de release é
  **servidor 1.5.0 antes da app 1.12.0**.

### Apresentação

- Lista: todas as tags em badges, alinhadas à direita, com wrap para a linha
  seguinte; o cartão cresce em altura.
- Detalhe: chips abaixo do nome.
- Formulário: campo de texto + botão Adicionar + chips com remoção.
- Histórico: `Tags` com os valores antigos/novos divididos por `;`.
- Filtros: uma tag de cada vez, derivada do conjunto de todos os clientes.

## Risks / Trade-offs

- [Tags com `;`] → o separador é reservado; o formulário rejeita `;` dentro de
  uma tag e o servidor divide sempre por `;`.
- [String longa no audit] → a auditoria trunca o texto a 200 caracteres; o log
  mostra o valor truncado, como já acontece com `notas`.
- [App antiga mostra várias tags como uma] → limitação aceite; a app nova
  apresenta-as corretamente.
- [Servidor antigo rejeita >100] → publicar o servidor primeiro.

## Migration Plan

1. Publicar o servidor 1.5.0 (limite 500 + normalização).
2. Publicar a app 1.12.0.
3. Rollback: o campo continua `TEXT`; uma app antiga lê a string inteira, e o
   servidor antigo só rejeita strings acima de 100.

## Open Questions

Nenhuma.
