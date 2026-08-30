## Context

A camada Compose já segue o sistema: `ElisdbTheme` usa `isSystemInDarkTheme()` como default e, em Android 12+, usa cores dinâmicas. O único desvio encontrado é o tema XML da janela (`Theme.Elisdb` em `values/themes.xml`), fixado em `android:Theme.Material.Light.NoActionBar` — o que deixa a janela clara no arranque em dark mode e, em API 24–28, força ícones escuros nas barras de sistema mesmo em dark mode. Não há testes de tema. Ver proposta.md para a motivação completa.

## Goals / Non-Goals

**Goals:**
- Seguir o modo do sistema em todas as camadas: tema Compose, tema da janela e aparência dos ícones das barras de sistema (API 24+).
- Tornar a seleção do `ColorScheme` testável em JVM puro (os testes unitários atuais usam só JUnit + coroutines-test, sem Robolectric).
- Verificar o comportamento com testes instrumentados e smoke-test manual em emulador.

**Non-Goals:**
- Redesenhar as cores do tema (mantém-se o esquema roxo existente e as cores dinâmicas).
- Adicionar alternador manual claro/escuro dentro da app (não pedido).
- Migrar para AppCompat/Material Components (a app é puramente Compose; as barras de sistema são tratadas por `enableEdgeToEdge`).

## Decisions

### 1. Tema de janela com `values-night/themes.xml`
Adicionar `app/src/main/res/values-night/themes.xml` com `parent="android:Theme.Material.NoActionBar"`, mantendo o `values/themes.xml` atual (claro). O qualifier `night` resolve automaticamente pelo `uiMode` do sistema, sem código extra.
- **Porquê:** a plataforma não tem `Theme.Material.DayNight` (isso é AppCompat); dois ficheiros de recurso são a abordagem determinística documentada para apps sem AppCompat.
- **Alternativas consideradas:** `android:Theme.DeviceDefault.NoActionBar` num único ficheiro — depende do comportamento do fabricante e não é determinístico; usar AppCompat só por causa do tema — dependência nova desnecessária.
- **Efeito observável:** em API 24–28 as barras de sistema passam a ter ícones claros em dark mode; em todas as APIs o arranque em dark mode não pisca fundo claro.

### 2. `enableEdgeToEdge()` mantém-se
O comportamento atual (`enableEdgeToEdge()` sem argumentos → `SystemBarStyle.auto`) adapta os ícones das barras ao modo detetado pelo `uiMode`. Como `MainActivity` não declara `android:configChanges`, mudanças de `uiMode` recriam a activity e a aparência re-aplica-se. Não é necessária mudança de código; fica coberta pela verificação em smoke-test.

### 3. Extração de `resolveColorScheme` testável
Extrair de `ElisdbTheme` uma função pura em `Theme.kt`:
```kotlin
internal fun resolveColorScheme(
  darkTheme: Boolean,
  dynamicColor: Boolean,
  dynamicScheme: ((Boolean) -> ColorScheme)?,
): ColorScheme
```
- Com `dynamicColor = false`: devolve `DarkColorScheme` se `darkTheme`, senão `LightColorScheme`.
- Com `dynamicColor = true` e provider disponível: delega no provider; sem provider cai no esquema estático correspondente.
- O composable passa `dynamicScheme = { if (SDK >= S) if (it) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) else null }`.
- **Porquê:** mantém a lógica de decisão testável em JVM puro sem Robolectric; `ColorScheme` é um data class comparável.
- **Alternativas consideradas:** testar com Robolectric — adiciona dependência pesada só para isto; testar apenas no emulador — não cumpre a verificação automatizada pedida.

### 4. Verificação por testes
- **Unit tests (JVM):** `ThemeTest` com casos escuro→`DarkColorScheme`, claro→`LightColorScheme`, e chamada do provider com a flag certa quando `dynamicColor=true`.
- **Instrumented test (Compose):** `ThemeModeTest` que abre `MainActivity`, alterna o modo noturno do dispositivo (`UiModeManager.setApplicationNightMode(MODE_NIGHT_YES/NO)`) e assere que o `MaterialTheme.colorScheme` correspondente está em uso (ex.: luminância de `background`).
- **Smoke-test manual:** alternar o tema do sistema no emulador e capturar screenshots da lista, form, detail, viewer de fotos e barras de sistema.

## Risks / Trade-offs

- [Tiles do mapa mantêm-se claros em dark mode] → Esperado e intencional (exceção especificada; a renderização pertence à biblioteca de mapas).
- [Mudar `uiMode` em teste instrumentado afeta o dispositivo todo] → Restaurar o modo anterior no `@After`; o emulador de teste é descartável.
- [`values-night` só cobre API ≥ 8 e `Theme.Material` só API ≥ 21] → minSdk 24 cobre ambos; sem risco.
- [Cores dinâmicas (Android 12+) podem divergir do roxo base] → Comportamento pretendido do Material 3; já existe hoje.
