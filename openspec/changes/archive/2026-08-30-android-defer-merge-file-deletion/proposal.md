## Why

`pushEdits` envolve todo o loop numa transação (`SyncCoordinator.kt:115`). `mergeDuplicate` abre a sua própria `runInTransaction` (junta-se à externa) e elimina ficheiros de foto DEPOIS de retornar (`SyncCoordinator.kt:208`) — ou seja, enquanto a transação externa ainda está aberta. Se um registo posterior no mesmo loop lançar, a transação externa faz rollback das linhas DB, mas os ficheiros já apagados desapareceram, deixando linhas `Foto` a apontar para ficheiros inexistentes (dessincronização DB/disco).

## What Changes

- A eliminação de ficheiros de foto no merge é adiada para DEPOIS de a transação externa (push) fazer commit com sucesso.
- Recolher os caminhos a apagar durante o merge e removê-los só após o commit.

## Capabilities

### New Capabilities

### Modified Capabilities
- `duplicate-reconciliation`: nova requirement "Superseded photo files deleted only after commit" — ficheiros de foto substituídos num merge SHALL ser removidos do disco apenas após a transação de persistência fazer commit, para não deixar linhas `Foto` apontando a ficheiros em falta.

## Impact
- Android: `sync/SyncCoordinator.kt` (adiar `os.remove` para após commit do push). Sem alteração de protocolo.
- Testes: testar que falha após o merge não deixa ficheiros apagados com linhas órfãs.
