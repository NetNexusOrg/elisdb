## Why

O URL do servidor é hoje editável manualmente nas Definições (`OutlinedTextField`)
e gravado com o botão "Guardar" (URL + chave). A edição manual é propensa a erros
(host/porta errados) e duplica o que o ficheiro de configuração emitido pelo
servidor já fornece. Com o fluxo de importação do `.config.json`, o URL deve vir
do ficheiro; o ecrã passa a mostrar apenas a informação configurada por esse
import — **não editável** —: URL do servidor, chave de assinatura (mascarada) e
nome de utilizador associado.

## What Changes

- O campo de texto editável "URL do servidor" e o botão "Guardar" saem das
  Definições. O URL passa a ser mostrado como informação só de leitura.
- O `SettingsViewModel` deixa de expor `onBaseUrlChange` e `save()`; o estado
  mantém `baseUrl` apenas para apresentação read-only. A importação do ficheiro de
  configuração continua a persistir URL + chave e a disparar a sincronização; a
  remoção da chave e o sync manual (barra de estado) mantêm-se.
- Após importar o ficheiro, o ecrã mostra, em modo só-leitura: o **URL do
  servidor**, a **chave de assinatura mascarada** e o **utilizador associado**
  (quando resolvido), sem qualquer campo editável para configuração do servidor.

## Capabilities

### New Capabilities
- nenhuma.

### Modified Capabilities
- `client-settings`: o requisito "Server connection settings screen" deixa de
  permitir editar o URL — mostra URL + chave mascarada + utilizador em modo
  só-leitura, configurados pela importação do ficheiro; o requisito "Apply
  configuration to the sync client" passa a aplicar a configuração por ocasião da
  importação (e não de um "Guardar" manual).

## Impact

- App Android: `SettingsViewModel.kt` (remover `onBaseUrlChange`/`save`/`saving`),
  `SettingsScreen.kt` (URL read-only, remover "Guardar"), testes
  `SettingsViewModelTest.kt` e `SettingsScreenTest.kt`.
- Sem alterações ao `SettingsStore`, ao transporte de sync, ao `ApiClient`, ao
  servidor nem à emissão do ficheiro de configuração.