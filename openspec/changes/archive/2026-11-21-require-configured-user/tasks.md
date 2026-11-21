# Tasks

## 1. Predicados e estado da configuração

- [x] 1.1 Acrescentar `hasCredentials()` e `hasConfiguredUser()` a `settings/SettingsStore.kt` e cobri-los com `ServerSettingsGateTest`.
- [x] 1.2 Expor `settings: StateFlow<ServerSettings>` e `refreshSettings()` no `AppContainer`; usar `hasCredentials()` no `periodicSyncScheduler`.
- [x] 1.3 Acrescentar `recoverUserName()` ao `AppContainer` (pede o nome ao servidor quando falta) e chamá-lo no arranque em `ElisdbApp`.
- [x] 1.4 Acrescentar `onSettingsChanged` ao `ServerConfigViewModel`, chamá-lo depois de importar/remover e adotar o nome resolvido pelo lookup quando o ficheiro não o trazia; cobrir com testes.

## 2. Gate de arranque

- [x] 2.1 Criar `ui/startup/SetupRequiredScreen.kt` com a explicação, a mensagem específica para nome em falta e o botão **Abrir configuração**.
- [x] 2.2 Extrair `ConfigDestination(container, onBack)` do `entry<Config>` e passar a usá-lo no gate e na entrada de navegação, ligando `onSettingsChanged` a `refreshSettings()`.
- [x] 2.3 Reestruturar `MainNavigation` para escolher entre configuração, setup e destinos principais; garantir que o back do sistema na configuração volta ao setup.

## 3. Testes

- [x] 3.1 Adicionar `StartupScreensTest` (setup e falta de nome).
- [x] 3.2 Executar `./gradlew :app:testDebugUnitTest` e confirmar a suite verde.
- [x] 3.3 Executar `./gradlew :app:lintDebug` e `./gradlew :app:compileDebugAndroidTestKotlin`.
- [x] 3.4 Executar `StartupScreensTest` num emulador e confirmar o comportamento.

## 4. Documentação

- [x] 4.1 Atualizar `elisdb-android/docs/uso.md`: configuração completa obrigatória, ecrã de setup e recuperação do nome.

## Workflow follow-up

- Arquivar a change depois da revisão e dos testes verdes.
