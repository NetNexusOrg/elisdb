## 1. Implementação

- [x] 1.1 Mover o bloco de notas (`Text` condicional) em `ClientDetailContent` para depois do bloco mapa/"Navegar até aqui", ficando a ordem: contrato, nome, mapa, notas, fotos

## 2. Verificação

- [x] 2.1 Compilar com `./gradlew :app:assembleDebug` e correr os testes unitários existentes
- [x] 2.2 Testar no emulador: abrir um cliente com coordenadas e confirmar que o mapa aparece após o nome e que as notas aparecem entre o mapa e as fotos; confirmar que um cliente sem notas mantém o layout correto