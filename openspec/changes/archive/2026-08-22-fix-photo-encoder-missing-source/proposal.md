## Why

O teste instrumentado `PhotoEncoderTest.missingSource_throwsAndLeavesNoBrokenFile` falha: ao codificar um ficheiro de origem em falta, `decode()` deixa `ContentResolver.openInputStream` lançar `FileNotFoundException`, que propaga para fora em vez do erro documentado `IllegalStateException("Não foi possível ler a imagem.")`. Como a origem é aberta duas vezes sem tratar a ausência do ficheiro, o caminho "origem ilegível → devolve null → limpa destino e lança erro claro" nunca é alcançado.

## What Changes

- `decode()` passa a tratar uma origem ilegível (ausente ou com erro de leitura) como falha de descodificação, devolvendo `null`, para que `encodeToWebp` lance o `IllegalStateException` com a mensagem clara e não deixe ficheiros WebP partidos.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- Nenhuma. Correção interna de tratamento de erro (a especificação `photo-encoding` já cobre "Failed encoding preserves the source"). Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/data/PhotoEncoder.kt`: `decode()` passa a tolerar `FileNotFoundException`/`IOException` de `openInputStream`.
- Testes: `PhotoEncoderTest` (instrumentado) volta a passar; nenhum novo teste necessário.
