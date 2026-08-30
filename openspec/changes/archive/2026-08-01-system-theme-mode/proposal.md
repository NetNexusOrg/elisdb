## Why

A gestão de clientes deve respeitar o modo escuro/claro do sistema, tal como qualquer app Android moderna. Hoje o tema Compose segue o sistema corretamente (`isSystemInDarkTheme()` + cores dinâmicas), mas o tema XML da janela (`Theme.Elisdb`) está fixado em claro (`android:Theme.Material.Light.NoActionBar`), o que causa um fundo claro/borrão ao arrancar a app em modo escuro. Não existe nenhuma verificação automatizada que garanta que a app continua a respeitar o modo do sistema.

## What Changes

- Adicionar um tema de janela para o modo escuro (`values-night/themes.xml`) para que o fundo e as barras do sistema acompanhem o modo do sistema em todo o arranque (sem flash claro em dark mode).
- Garantir que `enableEdgeToEdge()` adapta a cor dos ícones das barras de sistema ao modo do sistema.
- Extrair a seleção do `ColorScheme` para uma função testável e adicionar testes unitários que provem que o modo escuro usa o esquema escuro e o claro usa o esquema claro.
- Adicionar verificação de que nenhuma cor hardcoded no código UI (fora do viewer de fotos e do mapa, que são propositadamente escuros/claramente mapeados) impede o modo escuro.
- Smoke-test em emulador alternando o modo escuro/claro do sistema e capturando screenshots.

## Capabilities

### New Capabilities
- `system-theme`: A app segue o modo escuro/claro do sistema em todas as camadas (tema Compose, tema da janela, barras de sistema) e isso é verificado por testes.

### Modified Capabilities
<!-- Nenhuma capacidade existente muda a nível de requisitos. -->

## Impact

- `app/src/main/res/values/themes.xml` e novo `app/src/main/res/values-night/themes.xml`.
- `app/src/main/java/com/example/elisdb/MainActivity.kt` (`enableEdgeToEdge()`).
- `app/src/main/java/com/example/elisdb/theme/Theme.kt` (extração da função de `ColorScheme`).
- Testes: novos unit tests de tema e instrumented test de dark mode; verificação manual em emulador.
- Sem novas dependências.
