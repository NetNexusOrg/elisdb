## Context

Ver `proposal.md` — Why. O registo do cliente já tem vários campos opcionais de
texto (`contrato`, `notas`) que são persistidos localmente (Room), transportados
como `null` quando vazios, e guardados no SQLite do servidor. A sincronização é
aditiva e tolerante a campos desconhecidos: o cliente Android usa
`ignoreUnknownKeys = true` e os DTOs remotos têm valores por defeito.

## Goals / Non-Goals

**Goals**

- Acrescentar a tag sem quebrar clientes/servidores que ainda não a conhecem.
- Manter uma única migração aditiva em cada base de dados.
- Reutilizar o padrão existente de campo opcional (trim, branco → `NULL`).
- Registar a alteração da tag no histórico de edições.

**Non-Goals**

- Filtro por tag na lista (fase futura).
- Múltiplas tags por cliente, gestão de catálogo de tags ou normalização
  automática de maiúsculas.
- Alterar a pesquisa livre para incluir a tag.

## Decisions

### Campo único nullable

`tag: String?` (uma etiqueta por cliente), em vez de lista/JSON. É o que o nome
do campo e o âmbito atual pedem; o filtro futuro pode comparar o valor da coluna
sem migrar dados.

### Normalização

Guardar `trim(tag)` e converter branco em `NULL`, tal como `contrato`/`notas`.
Preservar as maiúsculas/minúsculas escritas pelo utilizador; o filtro futuro
compara sem distinção de caixa/acentos (mesmo `normalizeForSearch` já usado na
pesquisa). Alternativa rejeitada: forçar maiúsculas como o `nome`, o que
surpreenderia o utilizador numa etiqueta livre.

### Limite de 100 caracteres

O servidor valida `max_length=100` (Pydantic), muito abaixo dos 100 000 de
`notas`. O formulário Android limita a entrada a 100 caracteres, para que o
utilizador nunca construa um registo que o servidor rejeite.

### Migrações aditivas

- Servidor: `ALTER TABLE clientes ADD COLUMN tag TEXT`, idempotente, executada
  antes do rebuild de coordenadas para que este preserve a coluna.
- Android: Room versão 11 com `ALTER TABLE clientes ADD COLUMN tag TEXT`.

Alternativa rejeitada: rebuild de tabela no Android — desnecessário para uma
coluna nullable e mais arriscado com a FK de fotos.

### Sem bump de `API_VERSION`

O campo é aditivo: clientes antigos ignoram a chave extra; clientes novos leem
`null` de servidores antigos. Bumps de versão ficam reservados para alterações
incompatíveis.

### Ordem de apresentação

Tag editável entre Contrato e Nome no formulário; no detalhe aparece como
etiqueta logo abaixo do nome; na lista aparece na linha secundária, junto ao
contrato e ao label "Inativo". A pesquisa livre mantém-se em nome/contrato/notas.

## Risks / Trade-offs

- [Servidor antigo descarta a tag] → O campo é aceite e devolvido apenas depois
  de atualizar o servidor; a app continua funcional e o valor local mantém-se
  pendente até o servidor o aceitar. Documentar que o servidor deve ser
  atualizado antes ou junto com a app.
- [Migração de coordenadas perder a coluna em bases legadas] → A migração da tag
  corre antes do rebuild e o rebuild copia explicitamente `tag`; teste com base
  legada cobre o cenário.
- [Registos com tag acima de 100 caracteres falham no sync] → Limite aplicado no
  formulário Android e no Pydantic.
- [Histórico com entradas antigas sem tag] → O log mostra apenas as entradas que
  têm `tag` nas alterações; entradas antigas continuam válidas.

## Migration Plan

1. Atualizar e reiniciar o servidor (migração automática no arranque).
2. Publicar a app Android (Room 10→11 no primeiro arranque).
3. Rollback: reverter a app é seguro (o servidor mantém a coluna, ignorada pelas
   versões antigas); reverter o servidor deixa a coluna no ficheiro SQLite, sem
   efeito para as versões antigas.

## Open Questions

Nenhuma.
