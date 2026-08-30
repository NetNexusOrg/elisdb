## 1. Assinatura da query string (servidor + app)

- [x] 1.1 Em `app/main.py`, `require_auth` passa a reconstruir `path?query` (usando `request.url.path` + `request.url.query`) e a assinar essa string
- [x] 1.2 Em `app/auth.py`, atualizar a docstring/mensagem para refletir que a query é coberta (sem alterar a assinatura)
- [x] 1.3 Em `remote/ApiClient.kt`, assinar `path?query` nos pedidos `pull` (com `since`) e `delete` (com `deleted_at`)
- [x] 1.4 Atualizar o helper de testes `AuthFixture.request` para assinar o path completo (incluindo query) em vez de `path.split("?")[0]`

## 2. Rejeição de colisão de foto entre clientes

- [x] 2.1 Em `app/database.py`, `_replace_fotos` passa a detetar ids de foto já pertencentes a outro cliente e a `raise ValueError` antes de escrever ficheiros

## 3. Paginação à prova de empates

- [x] 3.1 Em `app/database.py`, `pull_all` passa a aceitar `since_id` e a filtrar com `updated_at > since OR (updated_at = since AND id > since_id)`
- [x] 3.2 Em `app/main.py`, endpoint `/api/pull` passa a aceitar `since_id` e a devolver `next_since_id`
- [x] 3.3 Em `remote/RemoteDto.kt`, adicionar `next_since_id` ao DTO de resposta (opcional, ignorado por versões antigas)

## 4. Testes e verificação

- [x] 4.1 Adicionar testes em `test_main.py`: (a) assinatura que não cobre a query é rejeitada; (b) colisão de foto cross-cliente é rejeitada e a foto original mantém-se; (c) paginação com `updated_at` empatados não salta registos
- [x] 4.2 Correr `pytest` no servidor e `./gradlew :app:assembleDebug` + testes unitários da app