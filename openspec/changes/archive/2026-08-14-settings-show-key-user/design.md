## Context

- O `SettingsViewModel` mantém `keyUser: String?` e `keyIdentityResolved: Boolean`. O `lookupKeyIdentity()` corre apenas no `init` (quando já existe chave) e após `importKey()`. Se falhar, ambos ficam no estado inicial e a UI esconde a secção inteira do utilizador (`if (state.keyIdentityResolved)`).
- O `SyncManager` expõe `status: StateFlow<SyncStatus>` (IDLE/IN_PROGRESS/SUCCESS/ERROR) e `lastResult`, consumidos pela SettingsScreen.
- `Navigation.kt` injeta `fetchKeyIdentity` no ViewModel a partir do `ApiClientProvider.create(settingsStore).fetchKeyIdentity()`.
- O `ApiClient` já tem `fetchKeyIdentity()` que devolve `KeyIdentityDto(username)` e lança exceção em erro de rede/auth (o wrapper em `Navigation.kt` converte para null).
- O ecrã usa um padrão label+valor (`labelMedium` + `bodyLarge`) para URL e chave; a secção do utilizador, quando visível, já segue o mesmo padrão.

## Goals / Non-Goals

**Goals:**
- O utilizador associado está sempre visível quando há chave configurada, com três estados explícitos: a resolver, resolvido (nome), ou indisponível (não foi possível obter).
- O estado "indisponível" é distinto de "sem utilizador associado" (servidor respondeu sem nome).
- A tipografia das linhas de informação (label + valor) é consistente entre URL, chave e utilizador.
- A identidade volta a ser resolvida após um sync bem-sucedido, sem depender de reabrir o ecrã.

**Non-Goals:**
- Não alterar o endpoint `/api/keys/me` nem o servidor.
- Não adicionar edição manual do utilizador (mantém-se read-only).
- Não mudar o mecanismo de importação/clear da chave.

## Decisions

### 1. Modelar a identidade como estado tri-estado em vez de flag booleana

Substituir `keyIdentityResolved: Boolean` + `keyUser: String?` por um estado explícito no `SettingsUiState`:

```kotlin
sealed interface KeyIdentity {
  data object Unresolved : KeyIdentity        // ainda não foi resolvido / não há chave
  data object Resolving : KeyIdentity         // lookup em curso
  data class Resolved(val username: String?)  // servidor respondeu (nome ou ausência)
  data object Unavailable : KeyIdentity       // lookup falhou (rede/servidor)
}
```

- `Resolved(username = null)` → "Sem utilizador associado a esta chave."
- `Unavailable` → "Não foi possível determinar o utilizador." (distinto do anterior)
- `Resolving` → "A identificar utilizador…"
- `Unresolved` → sem chave, secção não renderizada.

**Alternativa considerada:** manter `Boolean` + `String?` e apenas remover o `if`. Rejeitada porque não distingue "sem resposta do servidor" de "servidor disse que não há nome" — requisito explícito do spec.

### 2. O ecrã mostra a secção sempre que há chave

Em `SettingsScreen.kt`, dentro do bloco `if (state.hasKey)`, renderizar a secção do utilizador de forma incondicional (label `labelMedium` + valor `bodyLarge`), mapeando o estado para o texto/cores:

- `Resolving` → texto placeholder, cor `onSurfaceVariant`.
- `Resolved(username)` → nome, cor `onSurface`; `null` → "Sem utilizador associado a esta chave.", cor `onSurfaceVariant`.
- `Unavailable` → "Não foi possível determinar o utilizador.", cor `onSurfaceVariant`.

Assim o padrão label+valor é idêntico ao de URL e chave, garantindo consistência visual sem tocar na `Typography`.

### 3. Disparar re-resolução após sync bem-sucedido

O `SettingsViewModel` observa `syncManager.status`. Quando o estado transita para `SUCCESS` e a identidade atual não é `Resolved`, chama `lookupKeyIdentity()` de novo. Isto garante que um utilizador não-resolvido (falha de rede anterior) aparece depois de o sync ter sucesso, sem reabrir o ecrã.

### 4. `fetchKeyIdentity` devolve o estado tri-estado

O `Navigation.kt` continua a converter exceções, mas agora o resultado alimenta o estado: sucesso → `Resolved(username)`, falha → `Unavailable`. O `ApiClient` não muda.

## Risks / Trade-offs

- **Lookup repetido:** a re-resolução após sync adiciona um pedido extra ao servidor a cada sync bem-sucedido (apenas quando a identidade não está resolvida). Baixo custo; `/api/keys/me` é barato. Pode ser mitigado futuramente com cache.
- **Distinção Unavailable vs Resolved(null):** adiciona um estado na UI; risco baixo de confusão, mas o spec exige a distinção e o texto é claro.
- **Consistência tipográfica:** manter `labelMedium`+`bodyLarge` para as três linhas; nenhuma mudança na `Typography` global, evitando impacto noutros ecrãs.
