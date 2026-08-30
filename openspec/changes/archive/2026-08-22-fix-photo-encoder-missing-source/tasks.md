## 1. Correção do tratamento de origem ilegível

- [x] 1.1 Em `PhotoEncoder.decode()`, abrir o `InputStream` de `openInputStream` de forma que `FileNotFoundException`/`IOException` devolva `null` (ex. helper `openOrNull` ou `runCatching`), nas duas leituras (bounds e descodificação)
- [x] 1.2 Confirmar que `encodeToWebp` devolve o `IllegalStateException("Não foi possível ler a imagem.")` e apaga o destino nesse caminho (já existente)

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Correr `./gradlew connectedDebugAndroidTest` e confirmar que `PhotoEncoderTest.missingSource_throwsAndLeavesNoBrokenFile` passa
