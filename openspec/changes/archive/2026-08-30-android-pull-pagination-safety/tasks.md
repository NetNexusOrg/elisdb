## 1. Loop de paginação no pull

- [x] 1.1 Em `sync/SyncCoordinator.kt`, `pull()` passa a acumular registos e repetir `api.pull` enquanto a resposta devolver `next_since`/`nextSinceId`
- [x] 1.2 O primeiro `pull` usa o `watermark` atual como `since`; páginas seguintes usam o cursor devolvido

## 2. Sweep só após página completa

- [x] 2.1 O código que remove clientes locais ausentes (`SyncCoordinator.kt:269-276`) passa a correr apenas após o loop terminar sem `next_since` (full-pull completo)
- [x] 2.2 Pulls incrementais (`watermark != null`) continuam a não fazer sweep de remoção (apenas tombstones)

## 3. Watermark da última página

- [x] 3.1 O watermark avança com o cursor da página final (`next_since`/`nextSinceId` ou máximo `updated_at` recebido)

## 4. Testes e verificação

- [x] 4.1 Testar pull paginado (servidor com `limit` pequeno) e confirmar que todos os registos chegam e nenhum local é apagado por engano
- [x] 4.2 Correr `./gradlew :app:assembleDebug` e testes da app
