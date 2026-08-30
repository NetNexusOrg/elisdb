## Why

Quando uma foto é removida ou renomeada numa edição de cliente, `_replace_fotos` (`app/database.py:303-321`) apaga as linhas `fotos` cujo id já não vem no push (`DELETE FROM fotos WHERE cliente_id = ? AND id NOT IN (...)`) mas NUNCA remove o ficheiro correspondente em `self.fotos_dir`. Só `delete_client` limpa ficheiros (`database.py:542-544`). Resultado: qualquer edição que remova uma foto deixa o ficheiro para sempre no disco — fuga de armazenamento que cresce com o uso normal.

## What Changes

- `_replace_fotos` passa a remover também os ficheiros em disco das fotos cujas linhas são apagadas.
- Adicionar um processo de limpeza (GC) que remove ficheiros em `fotos_dir` sem linha `fotos` correspondente, para recuperar órfãos já existentes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-photo-storage`: a requirement "Cascading photo deletion" passa a abranger também a remoção de fotos individuais dentro de uma edição de cliente; nova requirement "Orphaned photo files are removed" garante GC de ficheiros sem linha.

## Impact
- Servidor: `app/database.py` (`_replace_fotos` remove ficheiros; novo `gc_orphan_photos`), chamado no startup ou num endpoint de manutenção.
- Testes: `test_main.py` (empurrar cliente com foto, editar removendo-a, confirmar que o ficheiro desaparece do disco).
