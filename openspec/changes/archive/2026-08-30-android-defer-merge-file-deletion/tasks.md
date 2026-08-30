## 1. Adiar eliminação de ficheiros do merge

- [x] 1.1 Em `SyncCoordinator.kt`, `mergeDuplicate` deixa de apagar ficheiros; passa a devolver/acumular os caminhos a apagar
- [x] 1.2 `pushEdits` remove os ficheiros acumulados APÓS a transação fazer commit com sucesso
- [x] 1.3 Tolerar `FileNotFoundError` no remove pós-commit

## 2. Testes e verificação

- [x] 2.1 Testar que, se um registo posterior no loop do push falha, os ficheiros do merge não foram apagados e não há `Foto` órfã
- [x] 2.2 Testar que, com commit bem-sucedido, os ficheiros superseded são removidos
- [x] 2.3 Correr testes da app
