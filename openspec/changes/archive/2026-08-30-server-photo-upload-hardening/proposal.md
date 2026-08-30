## Why

`PUT /api/photos/{id}` (`app/main.py:130-134`) faz `data = await request.body()` sem limite de tamanho, carregando o upload completo para memória; um dispositivo (ou um pedido reenviado) pode esgotar a RAM. Além disso, o endpoint aceita QUALQUER `PUT` autenticado e escreve o ficheiro de imediato, antes de qualquer push o referenciar — se o push for rejeitado/nunca enviado, o ficheiro fica órfão sem limpeza (não há GC de `fotos` sem linha).

## What Changes

- Limitar o tamanho do upload de foto (content-length máximo) devolvendo 413 se excedido; idealmente streaming com teto.
- Adicionar limpeza de ficheiros de foto escritos mas nunca referenciados por nenhuma linha `fotos` (GC periódico ou no startup).

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-photo-storage`: nova requirement "Bounded photo upload" (tamanho máximo) e nova requirement "Unreferenced photo files are garbage collected" (ficheiros PUT nunca ligados a um push são removidos).

## Impact
- Servidor: `app/main.py` (`put_photo` valida tamanho), `app/database.py` (GC de fotos não referenciadas, partilhado com `server-orphan-photo-cleanup`).
- Testes: `test_main.py` (upload acima do limite → 413; ficheiro PUT nunca referenciado é limpo).
