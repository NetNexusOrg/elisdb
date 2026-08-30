## 1. Implementação

- [x] 1.1 Remover o bloco condicional de contrato do `ClientDetailContent` em `ClientDetailScreen.kt`
- [x] 1.2 Mostrar o contrato no `title` da `TopAppBar` (a seguir ao botão voltar) quando `state.cliente.cliente.contrato` não for nulo/vazio

## 2. Verificação

- [x] 2.1 Compilar com `./gradlew :app:assembleDebug` e correr os testes unitários existentes
- [x] 2.2 Testar no emulador: abrir um cliente com contrato e confirmar que o contrato aparece na top bar junto ao voltar e que não há linha de contrato no corpo; confirmar que um cliente sem contrato não mostra contrato na top bar