## 1. Dockerfile como única fonte de verdade

- [x] 1.1 Alterar `elisdb-server/Dockerfile` para instalar as dependências diretamente (`fastapi>=0.111`, `uvicorn[standard]>=0.30`, `cryptography>=43.0`) no `RUN pip install`, removendo o `COPY requirements.txt`
- [x] 1.2 Apagar `elisdb-server/requirements.txt`

## 2. Limpar `.gitignore`

- [x] 2.1 Remover a entrada `.venv/` de `elisdb-server/.gitignore`

## 3. Documentação explícita Docker-only

- [x] 3.1 `elisdb-server/README.md`: afirmar que o servidor corre exclusivamente em Docker
- [x] 3.2 `elisdb-server/docs/` (index, instalacao, execucao): reforçar o wording Docker-only
- [x] 3.3 `openspec/config.yaml`: confirmar/ajustar a descrição do servidor como Docker-only
- [x] 3.4 Varrer o repo por referências a `requirements.txt` e `pip install` (fora de `openspec/changes/`) e corrigir
