## Why

O servidor deve ser **Docker-only** — Docker como o único modo suportado de correr e servir o serviço — e não apenas "Docker-first". Apesar de a documentação já só ensinar Docker, ainda existem artefactos que permitem/normalizam uma execução fora de Docker: o `requirements.txt` (que também permite `pip install` numa venv) e a entrada `.venv/` no `.gitignore`. Tornar o servidor explicitamente Docker-only elimina esse caminho e clarifica o contrato.

## What Changes

- **`Dockerfile`**: deixar de ler `requirements.txt`; instalar as dependências diretamente no `Dockerfile` (única fonte de verdade = Docker). Remover o `COPY requirements.txt`.
- **Remover `elisdb-server/requirements.txt`** — deixa de existir o manifesto `pip install -r`.
- **`.gitignore`**: remover a entrada `.venv/` (artefacto de execução manual, já sem uso num servidor Docker-only).
- **Documentação explícita Docker-only**: atualizar `README.md` e `docs/` do servidor (e `openspec/config.yaml`) para afirmar que o servidor corre **exclusivamente** em Docker, sem qualquer alternativa de execução manual.

## Capabilities

### New Capabilities

_Nenhuma_ — mudança de artefactos de build e documentação; sem comportamento novo.

### Modified Capabilities

_Nenhuma_ — nenhum requisito de especificação muda (a API, o auth, a persistência e o provisionamento ficam intactos).

## Impact

- `elisdb-server/Dockerfile` (dependências inline).
- Remoção de `elisdb-server/requirements.txt`.
- `elisdb-server/.gitignore` (remover `.venv/`).
- `elisdb-server/README.md`, `elisdb-server/docs/` e `openspec/config.yaml` (wording Docker-only).
- Sem impacto no código de runtime (`app/*.py`), no `compose.yaml`, no `docker-entrypoint.sh`, na API ou no esquema de dados.
