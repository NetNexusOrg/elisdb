## Why

O parâmetro `suppressConfigMessage` de `SyncStatusBar`/`syncMessage` ficou órfão: foi introduzido pela change `client-list-hide-config-notice` (arquivada a 2026-08-13), mas a barra deixou de existir na lista de clientes quando a `client-list-remove-sync-bar` foi aplicada. Hoje nenhum caller em produção passa `suppressConfigMessage = true` — apenas dois testes o fazem. É código morto que complica a leitura do componente.

## What Changes

- Remover o parâmetro `suppressConfigMessage` do composable `SyncStatusBar` e da função `syncMessage` (ficam com assinaturas mais simples).
- No ramo `SyncFault.CONFIG` de `syncMessage`, mostrar sempre o aviso "Servidor não configurado…" (deixar de devolver `null` condicionalmente).
- Atualizar `SyncStatusBarTest` para retirar os dois casos que só validavam a supressão do aviso de configuração.

Sem alteração de comportamento para o utilizador: a produção já nunca suprimia o aviso.

## Capabilities

### New Capabilities
<!-- nenhuma -->

### Modified Capabilities
<!-- nenhuma - change marcada com skip_specs: true (refactor puro, sem mudança de comportamento) -->

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/settings/SyncStatusBar.kt` — remoção do parâmetro e do ramo condicional.
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — call site inalterado (não passava o flag).
- `elisdb-android/app/src/androidTest/java/com/example/elisdb/ui/settings/SyncStatusBarTest.kt` — remoção dos testes que dependem do flag.