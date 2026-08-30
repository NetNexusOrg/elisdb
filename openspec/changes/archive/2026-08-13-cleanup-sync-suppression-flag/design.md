## Context

Ver proposta em `proposal.md`. Código morto num único ficheiro de UI:

- `SyncStatusBar.suppressConfigMessage` (default `false`) e `syncMessage.suppressConfigMessage` — nenhum caller em produção passa `true`.
- `SettingsScreen` chama `SyncStatusBar` sem o flag (`SettingsScreen.kt:69-73`).
- Únicos `true` encontram-se em `SyncStatusBarTest.kt` (linhas 88 e 96).
- O propósito original (suprimir aviso de configuração na lista de clientes) desapareceu quando `client-list-remove-sync-bar` foi aplicada.

## Goals / Non-Goals

**Goals:**
- Remover `suppressConfigMessage` de `SyncStatusBar` e `syncMessage`, simplificando o ramo `SyncFault.CONFIG` para devolver sempre o aviso.
- Atualizar os testes afetados.
- Manter exatamente o mesmo comportamento observável em produção.

**Non-Goals:**
- Não alterar mensagens, ícones ou fluxo de sincronização.
- Não mexer em `SettingsScreen` (o call site fica igual).
- Não tocar em specs (refactor puro; `skip_specs: true`).

## Decisions

**Remover o parâmetro em vez de o manter com default:** um flag que nenhum caller usa é ruído de API. Como o caso de uso que o motivava foi definitivamente retirado do produto (a barra deixou de viver na lista), não há razão para o preservar "para o futuro". Alternativa considerada: manter (custo zero, sem risco), descartada por deixar dívida de leitura.

**Ramo CONFIG devolve sempre `null`? Não — passa a devolver sempre o aviso.** O aviso descreve exatamente o estado atual da lista e das Definições: sem servidor configurado, o utilizador deve ir às Definições. Não há contexto em que o aviso deva ser suprimido.

**Testes: remover os dois casos que passavam `true`** — deixariam de compilar porque o parâmetro desaparece e não representam comportamento real. Os restantes casos (IN_PROGRESS/SUCCESS, defaults de CONFIG/AUTH/CLOCK/NETWORK) permanecem intactos como guarda do comportamento atual.

## Risks / Trade-offs

- [Teste a remover reduz cobertura de um caso do ramo CONFIG] → os restantes testes de `syncMessage` cobrem o ramo; o comportamento é trivial (sempre devolver o aviso).
- [Compilação: assinaturas mudam] → apenas `SettingsScreen` (sem flag) e `SyncStatusBarTest` chamam estas funções; ambos serão atualizados na mesma change.