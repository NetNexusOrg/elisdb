## 1. Teto de upload

- [x] 1.1 Em `app/main.py`, `put_photo` valida `Content-Length` contra `MAX_PHOTO_BYTES` e devolve 413 se exceder
- [x] 1.2 Opcionalmente ler o body em streaming com contador para não carregar tudo para memória

## 2. GC de ficheiros não referenciados

- [x] 2.1 Reutilizar `gc_orphan_photos()` (de `server-orphan-photo-cleanup`) para remover ficheiros PUT nunca referenciados por nenhuma linha `fotos`
- [x] 2.2 Garantir que o GC não remove ficheiros de pushes em curso (correr em startup/manutenção)

## 3. Testes e verificação

- [x] 3.1 Em `test_main.py`, upload acima do limite → 413; upload dentro do limite → 200
- [x] 3.2 PUT de foto nunca referenciada por um push é removido pelo GC
- [x] 3.3 Correr `pytest`
