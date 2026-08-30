# Tasks: Sanitize photo paths

## 1. Servidor — validação do id

- [x] 1.1 Em `app/models.py`, adicionar um validador Pydantic em `FotoIn.id` que rejeita ids contendo `/`, `\` ou `..`
- [x] 1.2 Em `app/database.py`, no `_foto`, adicionar uma verificação de contenção: o caminho resolvido deve ficar dentro de `fotos_dir` (normalizar e verificar prefixo); levantar erro caso contrário

## 2. Servidor — testes

- [x] 2.1 Em `test_main.py`, cenários: push com foto cujo id contém `../` é rejeitado; push com id normal continua a gravar na pasta de fotos; verificar que não é criado nenhum ficheiro fora de `photos/`

## 3. Verificação

- [x] 3.1 Correr os testes pytest do servidor; confirmar que passam
- [x] 3.2 Testar manualmente na VPS: push com id de traversal é rejeitado; push normal funciona
