## Why

O ecrã de Definições mistura tamanhos e estilos de texto no mesmo bloco de configuração: o URL usa `bodyLarge`, a chave usa `bodyMedium`, o utilizador usa `bodySmall`, alguns rótulos usam `labelMedium` e outros estão embutidos no texto do valor. O resultado é visualmente assimétrico e dificulta a leitura como um conjunto coerente.

## What Changes

- Padronizar o bloco "Configuração do servidor" num padrão de **rótulo + valor** simétrico: todos os rótulos com o mesmo estilo e todos os valores com o mesmo estilo.
- Aplicar o mesmo tratamento tipográfico a estados vazios ("Não configurado", "Nenhuma chave...") e à mensagem de utilizador associado, para que nenhum valor se destaque com um tamanho diferente.
- Unificar os textos de feedback (aviso de sync e mensagem pós-import) num único estilo.
- Sem alteração de comportamento, lógica ou conteúdo dos textos — apenas estilos tipográficos (tamanho, peso, cor).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma — mudança puramente de apresentação (estilos tipográficos); nenhum requisito comportamental muda. A change declara `skip_specs: true`.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/settings/SyncStatusBar.kt` (se necessário para alinhar o texto do estado de sync)
- Testes de UI de Definições (`SettingsScreenTest`) apenas se verificarem estilos.