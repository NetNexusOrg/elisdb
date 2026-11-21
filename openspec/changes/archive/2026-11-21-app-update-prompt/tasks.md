# Tasks

## 1. Extrair a construção do `UpdateViewModel`

- [x] 1.1 Criar `ui/about/UpdateViewModelFactory.kt` com `rememberUpdateViewModel(container)` que replica exatamente a configuração hoje inline no `entry<About>` (versão instalada, configuração, disponibilidade, fetch e download) e verificar que compila.
- [x] 1.2 Passar o `entry<About>` do `Navigation.kt` a usar o helper, sem mudar o comportamento do ecrã Sobre; verificar com `./gradlew :app:compileDebugKotlin`.

## 2. Aviso de atualização ao abrir a app

- [x] 2.1 Criar `ui/about/AppUpdatePrompt.kt` com a função pura `shouldShowUpdatePrompt(phase, dismissedVersionCode)`, o `LaunchedEffect` que chama `check()` quando a disponibilidade passa a `Online`, o estado `rememberSaveable` da versão adiada e o `AlertDialog` com **Atualizar** / **Adiar**.
- [x] 2.2 Montar o aviso no root de `MainNavigation`: criar o `UpdateViewModel` com o helper e renderizar `AppUpdatePrompt(onUpdate = { backStack.add(About) })`, oculto quando a rota atual é `About`; verificar que o ecrã inicial continua a abrir normalmente.
- [x] 2.3 Confirmar o fluxo manualmente (ou por teste): com versão publicada mais recente aparece o diálogo; **Adiar** fecha; **Atualizar** abre Sobre; sem versão/configuração/online não aparece nada.

## 3. Testes

- [x] 3.1 Adicionar `AppUpdatePromptPolicyTest` em `app/src/test/java/com/example/elisdb/ui/about/` cobrindo `Available` (mostra), versão adiada (não mostra) e `UpToDate`/`NoRelease`/`NotConfigured`/`Offline`/`Failed`/`Checking` (não mostra); verificar com `./gradlew :app:testDebugUnitTest`.
- [x] 3.2 Executar a suite unitária completa e confirmar que os testes existentes de `UpdateViewModel` continuam verdes.

## 4. Documentação

- [x] 4.1 Acrescentar em `elisdb-android/docs/uso.md`, na secção **Atualizar a app**, o aviso automático ao abrir, as ações **Atualizar** (abre Sobre) e **Adiar** (volta a avisar no próximo arranque), e verificar que o texto corresponde ao comportamento implementado.

## Workflow follow-up

- Arquivar a change depois da revisão e dos testes verdes.
