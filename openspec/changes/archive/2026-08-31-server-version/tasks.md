## 1. Criar ficheiro VERSION

- [x] 1.1 Criar `elisdb-server/VERSION` com versão `1.0.0`

## 2. Atualizar Dockerfile

- [x] 2.1 Adicionar `ARG VERSION=0.0.0-dev` no Dockerfile
- [x] 2.2 Copiar ficheiro VERSION para a imagem

## 3. Atualizar compose.yaml

- [x] 3.1 Adicionar variável de ambiente `ELISDB_VERSION` no compose.yaml

## 4. Modificar endpoint /health

- [x] 4.1 Ler versão do ficheiro VERSION no `main.py`
- [x] 4.2 Modificar endpoint `/health` para incluir campo `version`

## 5. Atualizar logging de startup

- [x] 5.1 Incluir versão no log de startup do servidor