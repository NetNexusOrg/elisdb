## 1. Adicionar o pino vermelho

- [x] 1.1 Criar `app/src/main/res/drawable/ic_map_pin.xml` com um pino vermelho simples (vector, ~32dp, com círculo interior branco)

## 2. Usar o novo ícone no marcador

- [x] 2.1 Em `OsmMap.kt`, carregar `R.drawable.ic_map_pin` e atribuir a `marker.icon` (mantendo o anchor `CENTER`/`BOTTOM`)

## 3. Verificação

- [x] 3.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 3.2 Smoke-test manual no emulador: o pino vermelho aparece no detalhe, viewer fullscreen, picker e preview do formulário; arrasto/fixação mantêm-se
