# Proposal

## Why

Hoje a foto de perfil mostrada na lista de clientes é, implicitamente, a
primeira foto por `ordem`. Ao adicionar mais fotos não existe forma de escolher
outra: a única saída é remover a primeira, perdendo-a. A grelha de edição já tem
um "×" para remover cada foto; falta o controlo equivalente para escolher a foto
de perfil.

## What Changes

- Nova flag `preferida` por foto, persistida localmente (Room) e no servidor
  (SQLite), com migrações aditivas dos dois lados.
- Ecrã de edição: cada foto passa a mostrar uma estrela no canto superior
  esquerdo — preenchida na foto de perfil, contorno nas restantes. Tocar na
  estrela define essa foto como perfil.
- Lista de clientes: usa a foto marcada como `preferida`, com fallback para a
  primeira foto (fotos antigas e servidores antigos continuam a funcionar).
- Remover a foto de perfil reassinala automaticamente a primeira foto restante.
- Sync: a flag viaja no push/pull e a alteração apenas de metadados não
  re-downloada nem reescreve o ficheiro da foto.
- Compatibilidade retroativa: clientes antigos não apagam a escolha feita por
  clientes novos; servidores antigos ignoram o campo novo.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-photos`: passa a cobrir a escolha da foto de perfil do cliente —
  marcar/desmarcar na edição, uso na lista, fallback e reassinalação ao remover.
- `server-photo-storage`: passa a persistir e servir o metadado `preferida` de
  cada foto, preservando a escolha perante pushes de clientes antigos.
- `photo-content-identity`: passa a distinguir alterações de conteúdo de
  alterações apenas de metadados, atualizando a linha da foto sem reescrever o
  ficheiro.

## Impact

- Android: `data/local/Foto.kt`, `data/local/AppDatabase.kt`,
  `AppContainer.kt`, `data/ClienteRepositoryImpl.kt`,
  `ui/form/ClientFormViewModel.kt`, `ui/form/ClientFormScreen.kt`,
  `ui/cliente/ClientListViewModel.kt`, `remote/RemoteDto.kt`,
  `sync/SyncMapper.kt`, `sync/SyncCoordinator.kt`.
- Servidor: `elisdb/models.py`, `elisdb/database.py` (coluna, migração,
  `_replace_fotos`, pull e canonical).
- Testes: unitários Android (formulário, repositório, lista), instrumentado da
  grelha de edição e `pytest` do servidor.
- Sem alterações aos endpoints nem ao formato do protocolo além do novo campo
  opcional nos DTOs de foto.
