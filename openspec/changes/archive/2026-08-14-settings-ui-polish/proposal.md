## Why

O ecrã de Definições apresenta as linhas de informação (URL, chave, utilizador) como textos soltos empilhados com espaçamento uniforme, misturando estilos tipográficos de forma que os tamanhos parecem inconsistentes e o look&feel é cru face ao resto da app. O objetivo é dar um polimento visual coerente: agrupar a informação em cartões, hierarquizar os títulos e normalizar os tamanhos de fonte para um padrão consistente.

## What Changes

- Agrupar a secção "Configuração do servidor" em cartões (`Card`/`ElevatedCard`) com padding interno, em vez de textos soltos.
- Normalizar a hierarquia tipográfica do ecrã: títulos de secção (`titleMedium`), labels de campo (`labelMedium`/`labelLarge`) e valores (`bodyLarge`) com tamanhos consistentes; garantir que texto de apoio/erros usa `bodySmall`.
- Uniformizar o espaçamento vertical (grupos de campos vs separadores entre cartões).
- Manter toda a funcionalidade existente (importar/remover chave, sync manual, estados de identidade) inalterada.
- Sem alterações de comportamento de backend ou de dados.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: melhora a apresentação visual (look&feel) da grelha de definições — hierarquia tipográfica e agrupamento em cartões da secção de configuração do servidor.

## Impact

- `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — reestruturação do layout (cartões, espaçamento, tipografia).
- `app/src/main/java/com/example/elisdb/ui/settings/SyncStatusBar.kt` — possível ajuste de espaçamento/estilo para integrar com os cartões.
- `app/src/main/java/com/example/elisdb/theme/Type.kt` — apenas se necessário ajustar estilos globais; de preferência sem tocar na `Typography` global.
- Testes unitários existentes (`SettingsViewModelTest`) não devem ser afetados (mudança de UI pura).