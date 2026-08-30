# Tasks: Generic client avatar

## 1. App — avatar genérico na lista

- [x] 1.1 Em `ui/cliente/ClientListScreen.kt`, no `ClientListItemRow`, extrair o slot à esquerda (48dp, rounded) para renderizar sempre: `AsyncImage` da primeira foto quando existe; caso contrário o ícone `Icons.Default.Person` (tinted `onSurfaceVariant` sobre `surfaceVariant`), mantendo o alinhamento
- [x] 1.2 Manter o `Spacer` após a imagem e o layout do nome inalterados

## 2. Testes

- [x] 2.1 Adicionar teste de instrumentação (`ClientListScreenTest`): um cliente sem foto mostra o avatar genérico; um cliente com foto mostra a thumbnail da foto

## 3. Verificação

- [x] 3.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 3.2 Verificação no emulador: com clientes sem foto e com foto na lista, confirmar que ambos têm thumbnail (genérica ou a foto)
