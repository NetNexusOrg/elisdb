## 1. Remover ficheiro em edições

- [x] 1.1 Em `app/database.py`, `_replace_fotos` passa a obter os `caminho`s das fotos removidas e a apagar os ficheiros do disco após o `DELETE`
- [x] 1.2 Não remover ficheiros de fotos que continuam referenciadas

## 2. GC de órfãos

- [x] 2.1 Adicionar `gc_orphan_photos()` que remove ficheiros em `fotos_dir` sem linha `fotos` correspondente
- [x] 2.2 Correr `gc_orphan_photos()` no startup (após migrações) ou expor endpoint de manutenção

## 3. Testes e verificação

- [x] 3.1 Em `test_main.py`, empurrar cliente com foto, editar removendo a foto, e confirmar que o ficheiro desaparece do disco
- [x] 3.2 Confirmar que o GC remove um ficheiro deixado órfão e mantém os referenciados
- [x] 3.3 Correr `pytest`
