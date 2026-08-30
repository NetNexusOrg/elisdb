# Tasks: Settings remove info texts

## 1. App — remover parágrafo de instruções

- [x] 1.1 Em `ui/settings/SettingsScreen.kt`, remover o bloco `Text` com a instrução "Importe o ficheiro de configuração emitido pelo servidor ..."

## 2. App — remover mensagens de confirmação

- [x] 2.1 Em `ui/settings/SettingsViewModel.kt`, no `importKey`, não definir `message = "Configuração importada e guardada."` (deixar `message = null` após importação bem-sucedida)
- [x] 2.2 Em `ui/settings/SettingsViewModel.kt`, no `clearConfig`, não definir `message = "Configuração removida."` (deixar `message = null`); atualizar o teste em `SettingsViewModelTest` que espera essa mensagem

## 3. Testes e verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: nas Definições confirmar que (a) não existe o parágrafo de instruções, (b) importar um ficheiro de configuração não mostra "Configuração importada e guardada." e (c) remover a configuração não mostra "Configuração removida."
