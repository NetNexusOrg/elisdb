## Context

O `ClientDetailContent` em `ClientDetailScreen.kt` é um `Column` com `verticalScroll` e `Arrangement.spacedBy(12.dp)` onde os blocos aparecem na ordem: contrato → nome → notas → mapa/"Navegar até aqui" → fotos. A mudança é puramente de layout: mover o bloco de notas para depois do bloco do mapa. Ver proposta para motivação e specs para o comportamento exigido.

## Goals / Non-Goals

**Goals:**
- Ordem final: contrato → nome → mapa → notas → fotos.

**Non-Goals:**
- Não alterar conteúdo, lógica de visibilidade ou estilo dos blocos.
- Não tocar na navegação (fullscreen map), no picker de localização nem no form.

## Decisions

**D1: Mover o bloco de notas na composição, sem extrair novos composables**
O bloco de notas (um único `Text` condicional) é simples; basta reposicioná-lo no `Column` entre o bloco do mapa e o bloco de fotos. Não há necessidade de criar um novo composable nem de um estado partilhado.
- *Alternativas consideradas*: refactor para secções individuais — rejeitado, aumento de complexidade sem benefício para uma mudança de uma linha.

**D2: A ordem do bloco mapa + botão "Navegar até aqui" mantém-se unida**
O mapa e o botão de navegação continuam juntos como um bloco; só a posição das notas muda. A nova ordem na prática é: cabeçalho (contrato+nome), bloco mapa/navegação, notas, fotos.

## Risks / Trade-offs

- [Regressão visual em testes de UI existentes que dependam da ordem] → Mudança confinada a um bloco; a verificação é visual e por ordenação de conteúdo. Não há testes de UI automáticos que fixem a ordem atual (confirmar na task de verificação).
- [Espaçamento vertical diferente ao saltar as notas vazias] → Mantém-se `Arrangement.spacedBy(12.dp)`; o comportamento é o mesmo de hoje, apenas com os blocos noutra ordem.

## Migration Plan

- Não aplicável (mudança de UI pura, sem dados nem servidor). Rollback: reverter a posição do bloco.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.