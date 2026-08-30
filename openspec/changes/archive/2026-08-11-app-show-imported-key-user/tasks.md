## 1. ApiClient: identity lookup

- [x] 1.1 Add `data class KeyIdentity(keyId: String, username: String?)` and `suspend fun fetchKeyIdentity(): KeyIdentity` to `ApiClient`, signing a `GET /api/keys/me` like `pull`/`delete` (timestamp + signature + `checkAuthError`).
- [x] 1.2 Handle non-200 responses (404 on older servers, others) by throwing a typed exception the ViewModel can catch as "no identity".

## 2. ViewModel: fetch and surface the username

- [x] 2.1 Add `keyUser: String?` and a lookup-succeeded flag (or a `KeyIdentityUi` state) to `SettingsUiState`.
- [x] 2.2 After a successful import (`importKey`) and after `save()`, launch a non-blocking `viewModelScope` coroutine calling `fetchKeyIdentity()`.
- [x] 2.3 On success update state (username or "no user associated"); on any failure leave the identity as unknown without blocking save or sync.
- [x] 2.4 Keep the stored key and sync flow untouched by the lookup (identity is informational only).

## 3. Screen: display the associated user

- [x] 3.1 In `SettingsScreen.kt`, show the username next to the masked key when available; show a "sem utilizador associado" hint when the lookup succeeded without a name; show nothing extra when the lookup failed.
- [x] 3.2 Wire the import-path ViewModel method so the identity displays right after importing a key without a manual save.

## 4. Tests

- [x] 4.1 Unit/UI test: importing a key with a fake server returning a username displays that username.
- [x] 4.2 Test: server reporting no user → "sem utilizador associado" shown.
- [x] 4.3 Test: lookup failure (network/404) → no identity shown, key import/save still succeeds.

## 5. Verification

- [x] 5.1 Update any `SettingsViewModelTest` construction sites for the new dependency/signature.
- [x] 5.2 Run `./gradlew testDebugUnitTest` and `compileDebugAndroidTestKotlin`; confirm green.