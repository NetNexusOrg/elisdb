## 1. ViewModel

- [x] 1.1 Em `SettingsViewModel.kt`: remover `onBaseUrlChange`, `save()` e o estado `saving`; manter `baseUrl` no estado apenas para apresentação, `importKey`, `clearKey`, `manualSync` e a identidade.
- [x] 1.2 Confirmar com grep que `save()`/`onBaseUrlChange`/`saving` não ficam referenciados fora do ViewModel.

## 2. Screen

- [x] 2.1 Em `SettingsScreen.kt`: substituir o `OutlinedTextField` do URL por texto só de leitura ("URL do servidor" + valor).
- [x] 2.2 Remover o botão "Guardar" e o `CircularProgressIndicator` associado; garantir que a barra de sync/manual sync se mantém.
- [x] 2.3 Mostrar de forma não editável URL + chave mascarada + utilizador associado após o import.

## 3. Testes

- [x] 3.1 `SettingsViewModelTest`: ajustar testes que dependiam de `save`/`onBaseUrlChange` (nenhum chama `save`; confirmar que os de import/identidade continuam válidos).
- [x] 3.2 `SettingsScreenTest`: remover asserções do campo editável e do "Guardar"; validar URL read-only e o preenchimento via import.
- [x] 3.3 `testDebugUnitTest` + `compileDebugAndroidTestKotlin` verdes; instrumentados (`SettingsViewModelTest`, `SettingsScreenTest`) no emulador.

## 4. Verificação manual

- [x] 4.1 No emulador, Definições mostram URL/chave/utilizador sem campos editáveis e sem "Guardar".
- [x] 4.2 Importar o `.config.json` continua a preencher URL+chave e a correr a sincronização.