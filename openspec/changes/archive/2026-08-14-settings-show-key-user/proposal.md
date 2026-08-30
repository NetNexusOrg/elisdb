## Why

O ecrã de Definições mostra o URL do servidor e a chave de assinatura, mas a linha "Utilizador associado" só aparece quando o lookup de identidade (`GET /api/keys/me`) tem sucesso no momento exato em que o ecrã é criado. Como esse lookup corre uma única vez no `init` do `SettingsViewModel`, qualquer falha transitória de rede faz com que o utilizador fique permanentemente oculto — mesmo que a chave esteja configurada e o servidor a reconheça. O utilizador é a informação mais importante para confirmar a identidade a usar, por isso deve estar sempre visível.

## What Changes

- A secção "Utilizador associado" passa a ser **sempre exibida** quando existe uma chave configurada, com três estados claros:
  - lookup em curso → indica que a identidade está a ser resolvida;
  - lookup com sucesso → mostra o nome do utilizador;
  - lookup sem sucesso (rede/servidor) → indica que não foi possível obter a identidade, sem fabricar um nome.
- O lookup de identidade volta a correr em ocasiões relevantes (após importar configuração, após um sync bem-sucedido) em vez de apenas uma vez no init.
- A tipografia das linhas de informação (label + valor) é uniformizada visualmente, para que URL, chave e utilizador tenham o mesmo padrão de tamanho de fonte.

## Capabilities

### New Capabilities
- Nenhuma.

### Modified Capabilities
- `client-settings`: a linha do utilizador associado passa a estar sempre visível quando há chave, com estados explícitos de resolução/ausência, e o lookup não fica limitado ao init.
- `client-key-import`: o estado de identidade não resolvida deixa de ser tratado como ausência — é mostrado de forma distinta (indisponível) sem fabricar um nome.

## Impact

- `elisdb-android`:
  - `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt` — estado de identidade com estados (loading/resolved/unavailable), retry após sync.
  - `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — secção do utilizador sempre presente com tipografia consistente.
  - `app/src/main/java/com/example/elisdb/sync/SyncManager.kt` (ou evento equivalente) — gatilho para re-fetch da identidade após sync.
- Testes unitários em `app/src/test/` para os novos estados do ViewModel.
- Sem alterações no servidor (o endpoint `/api/keys/me` já existe).
