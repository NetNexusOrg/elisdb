## 1. Modelo de estado da identidade

- [x] 1.1 Substituir `keyIdentityResolved: Boolean` + `keyUser: String?` por `KeyIdentity` (sealed interface: `Unresolved`, `Resolving`, `Resolved(username: String?)`, `Unavailable`) em `SettingsUiState` (SettingsViewModel.kt), ajustando os locais que escrevem o estado
- [x] 1.2 Atualizar `lookupKeyIdentity()` para definir `Resolving` antes do pedido e `Resolved(username)`/`Unavailable` conforme o resultado do `fetchKeyIdentity`
- [x] 1.3 Adicionar testes unitários dos três estados da identidade (`Resolving`, `Resolved` com nome, `Resolved(null)`, `Unavailable`) no `SettingsViewModelTest`

## 2. Ecrã de Definições

- [x] 2.1 Em `SettingsScreen.kt`, renderizar a secção "Utilizador associado" incondicionalmente dentro de `if (state.hasKey)`, mapeando cada estado de `KeyIdentity` para texto/cor
- [x] 2.2 Garantir que label usa `labelMedium` e valor `bodyLarge` (mesmo padrão de URL e chave) para consistência tipográfica

## 3. Re-resolução após sync

- [x] 3.1 No `SettingsViewModel`, observar `syncManager.status`; ao transitar para `SUCCESS` com identidade ainda não `Resolved`, chamar `lookupKeyIdentity()` de novo
- [x] 3.2 Teste unitário: após sync bem-sucedido, uma identidade `Unavailable`/`Resolving` é re-resolvida e o nome aparece

## 4. Verificação

- [x] 4.1 Correr os testes da app (`./gradlew :app:testDebugUnitTest`) e garantir que passam
- [x] 4.2 Instalar a app no emulador, importar a config e confirmar que "Utilizador associado" aparece sempre com a chave configurada (com rede; e sem rede mostra "não foi possível determinar")
- [x] 4.3 Confirmar visualmente que URL, chave e utilizador usam tamanhos de fonte consistentes no ecrã de Definições
