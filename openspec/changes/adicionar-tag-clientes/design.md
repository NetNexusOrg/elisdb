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
- Permitir filtrar a lista pelas tags existentes, sem alterar a pesquisa livre.

**Non-Goals**

- Múltiplas tags por cliente ou gestão de um catálogo de tags.
- Alterar a pesquisa livre para incluir a tag.
- Persistir a escolha do filtro de tag entre arranques da app.

## Decisions

### Campo único nullable

`tag: String?` (uma etiqueta por cliente), em vez de lista/JSON. É o que o nome
do campo e o âmbito atual pedem; o filtro futuro pode comparar o valor da coluna
sem migrar dados.

### Normalização

Guardar `trim(tag)` e converter branco em `NULL`, tal como `contrato`/`notas`,
mas normalizar para maiúsculas: a tag é apresentada sempre em maiúsculas na
lista, no detalhe, no formulário e no histórico. O filtro compara o valor já
normalizado, sem distinção de caixa.

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

Tag editável como 4.º campo do formulário, abaixo das notas; no detalhe aparece
como etiqueta logo abaixo do nome; na lista aparece à direita do cartão, na
mesma linha do contrato e do label "Inativo". A pesquisa livre mantém-se em
nome/contrato/notas.

### Filtro por tag

As tags disponíveis são derivadas dos clientes guardados no dispositivo
(distintas, em maiúsculas e ordenadas), pelo que o menu funciona offline e
acompanha automaticamente a criação/remoção de tags. O filtro é aplicado depois
do filtro de estado e antes da pesquisa livre; "Todas as tags" limpa a seleção.
Se a tag selecionada deixar de existir, o filtro efetivo volta a "todas", para
não deixar a lista presa num resultado vazio. Alternativa rejeitada: persistir a
tag escolhida nas preferências — fica fora do âmbito e o filtro é uma ação
exploratória.

O mapa de clientes reutiliza o mesmo filtro de tag no seu próprio menu, também
independente da lista: a tag escolhida no mapa não altera o filtro da lista e
vice-versa. As tags disponíveis vêm de todos os clientes guardados, mesmo os que
não têm coordenadas.

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
