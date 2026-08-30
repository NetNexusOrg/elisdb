## Context

O `PhotoViewerScreen` mostra as setas `<`/`>` sobre a foto, ligadas a `viewModel.previous()`/`next()`. Como a troca de foto já é feita por swipe do `HorizontalPager` (sincronizado com `setIndice`), as setas são redundantes.

## Goals / Non-Goals

**Goals:**
- Remover as setas `<`/`>` e o código que só elas usavam, mantendo swipe e contador.

**Non-Goals:**
- Não alterar o swipe, o zoom, o contador nem o botão voltar.
- Não alterar a galeria do ecrã de detalhe.

## Decisions

- **Remover as setas e não os substituir por outro controlo.** O swipe já cobre a troca de foto; o contador "n / total" mantém-se como indicação de posição. Alternativa considerada: manter as setas apenas por acessibilidade — rejeitada por decisão de produto (pedido explícito do utilizador).
- **Remover o código morto associado.** `PhotoPager` perde os parâmetros `onPrevious`/`onNext` e o `PhotoViewerViewModel` perde `previous()`/`next()`; o `setIndice` permanece porque o pager continua a sincronizar o índice.
- **Atualizar o teste unitário.** `PhotoViewerViewModelTest.navegaAnteriorESeguinte` testa `previous()`/`next()`; passa a testar apenas `setIndice` (ou é removido).

## Risks / Trade-offs

- [Perda de alternativa acessível (navegação por toque em botão em vez de swipe)] → Aceite por decisão de produto; o contador mantém-se. Se a acessibilidade voltar a ser requisito, reintroduzir as setas.
- [Código morto deixado para trás] → Remover em conjunto (`PhotoPager`, `PhotoViewerViewModel`, teste), para o `lint` não acusar membros não usados.

## Migration Plan

Sem migração. Rollback trivial.

## Open Questions

Nenhuma.
