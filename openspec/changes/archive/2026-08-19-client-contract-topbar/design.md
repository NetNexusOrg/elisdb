## Context

A `TopAppBar` do `ClientDetailScreen` tem hoje `title = {}` (vazio), `navigationIcon` (botão voltar) e ações no overflow. O contrato é exibido no corpo do ecrã, acima do nome, num `Text` condicional no `ClientDetailContent`. A mudança move o contrato para a top bar, logo após o botão voltar. Ver proposta para motivação e specs para o comportamento exigido.

## Goals / Non-Goals

**Goals:**
- Mostrar o número de contrato na top bar do detalhe, junto ao botão voltar, quando existir.
- Remover a linha de contrato do corpo do ecrã.

**Non-Goals:**
- Não alterar o nome, mapa, notas ou fotos no corpo.
- Não alterar o overflow menu nem a navegação.
- Não alterar o form de edição (onde o contrato continua a ser um campo).

## Decisions

**D1: Usar o slot `title` da `TopAppBar` para o contrato**
O `title` da `TopAppBar` do Material 3 é colocado imediatamente a seguir ao `navigationIcon`, exatamente a posição pedida ("a seguir ao botão/ícone de voltar"). Com `title = {}` hoje, basta passar um `Text(cliente.contrato)` condicional quando o contrato existir. Estilo coerente com a top bar (titleLarge/onSurface), sem custom layout.
- *Alternativas consideradas*: colocar o contrato como um segundo item do `navigationIcon` (Row com botão+texto) — rejeitado, mexe no hit-area do botão voltar; `TopAppBar` com `title` dedicado é o padrão do Material.
- *Nota*: o `title` é levemente indented à direita do ícone; satisfaz a cena "next to the back button".

**D2: O contrato fica fora do `ClientDetailContent`**
Remover o bloco condicional de contrato do `ClientDetailContent` e passar o valor (ou null) até à `TopAppBar`. O `data.cliente.contrato` já está disponível no `state.cliente` usado para o menu; a top bar só renderiza o `Text` quando `contrato` não é null/blank.
- *Alternativa*: manter o contrato no corpo e só duplicá-lo na top bar — rejeitado, contradiz a cena "no contract SHALL be displayed above the client's name in the body".

## Risks / Trade-offs

- [Contrato longo pode comprimir o título ou colidir com as ações do overflow] → Material 3 `TopAppBar` trunca o `title` com ellipsis; o contrato é curto por natureza. Sem ação adicional.
- [A cena "Top bar does not repeat the client name" não é afetada] → O contrato não é o nome; a regra mantém-se.

## Migration Plan

- Não aplicável (mudança de UI pura). Rollback: reverter a top bar e repor o bloco no corpo.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.