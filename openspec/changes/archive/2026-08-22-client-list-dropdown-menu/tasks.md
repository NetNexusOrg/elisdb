## 1. Menu suspenso na barra superior

- [x] 1.1 Em `ClientListScreen.kt`, substituir os dois `IconButton` (Settings/Add) do `TopAppBar` por um `IconButton` de overflow com `DropdownMenu` contendo "Criar novo cliente" e "Definições"
- [x] 1.2 Manter os callbacks `onAddClick`/`onSettingsClick` ligados aos itens do menu

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Ajustar/adicionar teste instrumentado cobrindo a abertura do menu e as ações criar/settings
- [ ] 2.3 Smoke-test manual no emulador: menu abre, "Criar novo cliente" abre o formulário, "Definições" abre as definições
