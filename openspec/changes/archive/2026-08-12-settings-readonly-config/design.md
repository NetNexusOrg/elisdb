## Context

Nas Definições (SettingsScreen.kt) o URL é hoje um `OutlinedTextField` editável
(linhas 84-91) e existe um botão "Guardar" (linhas 140-149) que chama
`viewModel.save()`. O `SettingsViewModel` expõe `onBaseUrlChange` (linha 60),
`importKey`, `clearKey`, `save()` (110-121) e `saving`. A configuração eficaz vive
no `SettingsStore` (URL + chave) e é aplicada ao sync via `createApiClient` que lê
`store.settings()` em cada sincronização — o "Guardar" apenas persiste valores
manuais. Com o import do `.config.json` a fornecer URL+chave, a edição manual e o
botão de gravar deixam de ter propósito.

## Goals / Non-Goals

**Goals:**
- URL do servidor sem edição manual nas Definições (apresentação só de leitura).
- Sem botão "Guardar"/`save()`; configurar o servidor = importar o ficheiro.
- Após o import, mostrar de forma não editável: URL, chave mascarada e utilizador.

**Non-Goals:**
- Não alterar `SettingsStore`, `ApiClient`/transporte, servidor ou formato do ficheiro.
- Não alterar a barra de sync/manual sync (mantém-se como está).
- Não alterar o fluxo de importação do ficheiro (já persiste URL+chave e sincroniza).

## Decisions

**D1 — URL em modo só de leitura.**
O `OutlinedTextField` é substituído por um texto informativo ("URL do servidor"
+ valor). O URL vem do ficheiro importado (ou do default de build); não há campo
para o alterar.
Alternativa considerada: manter o campo desativado (`enabled = false`) — rejeitada,
um campo desativado sugere que pode ser editado.

**D2 — Remover `save()` e `onBaseUrlChange` do ViewModel.**
O estado mantém `baseUrl` apenas para apresentação. A importação (`importKey`)
continua a gravar URL+chave e a disparar `syncNow()`; o sync manual faz-se pela
barra de estado. O estado `saving` e a lógica do botão "Guardar" desaparecem.
Alternativa considerada: manter `save()` para "aplicar" — rejeitada, duplica o
import e contradiz o non-goal.

**D3 — Mostrar a informação configurada de forma não editável.**
Secção "Configuração" com: URL, chave mascarada (`maskedKey`), e utilizador
associado (`keyUser`, quando `keyIdentityResolved`). A pesquisa de identidade
(`lookupKeyIdentity`) mantém-se como está.

**D4 — Testes atualizados.**
- `SettingsViewModelTest`: remover/ajustar o que dependia de `save`/`onBaseUrlChange`
  (nenhum teste atual chama `save`; os de identidade usam `configJson()`).
- `SettingsScreenTest`: remover asserções do campo editável/"Guardar"; validar que
  o URL aparece como texto não editável e que o import continua a preencher URL/chave.

## Risks / Trade-offs

- [Utilizadores que apontavam manualmente para outro servidor] → O onboarding
  passa a ser via ficheiro de configuração; o default de build continua a servir
  quem não importa ficheiro.
- [Configuração "Guardar" já não existe] → O import persiste e sincroniza no ato;
  comportamento pretendido (uma única via de configuração).