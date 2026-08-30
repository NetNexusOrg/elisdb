# Tasks: Remove server configuration

## 1. ViewModel — clearConfig

- [x] 1.1 Em `ui/settings/SettingsViewModel.kt`, substituir `clearKey()` por `clearConfig()`: guardar `settingsStore.save("", "")` e repor o estado (`baseUrl`, `signingKey`, `hasKey=false`, `keyIdentity=Unresolved`, `signingKeyError=null`) com mensagem de confirmação
- [x] 1.2 Adicionar teste unitário em `app/src/test/.../ui/settings/SettingsViewModelTest.kt` para `clearConfig()`: URL e chave ficam vazios e o estado reflete a configuração removida

## 2. Ecrã — botão "Remover configuração"

- [x] 2.1 Em `ui/settings/SettingsScreen.kt`, mudar o texto do botão para "Remover configuração" e ligar `onClick` a `viewModel::clearConfig`
- [x] 2.2 Atualizar teste de instrumentação `settings/SettingsViewModelTest.kt` (`clearKey_persistsBlankAndKeyHidden` → remoção completa: baseUrl e signingKey ficam vazios, hasKey false); adicionar cobertura do rótulo novo em `ui/settings/SettingsScreenTest.kt` se viável

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e os testes de instrumentação relevantes; confirmar que passam
- [x] 3.2 Verificação no emulador: abrir Definições com configuração importada, tocar em "Remover configuração", confirmar que URL e chave ficam "Não configurado/a" e que uma sincronização devolve erro de configuração