## Why

O servidor ElisDB deve correr e ser usado **via Docker** como método único. Hoje a documentação (README e `docs/`) ainda lidera com a execução manual em Python (`venv` + uvicorn) e trata o Docker como alternativa, e o diretório acumula ficheiros que já não são relevantes para esse fluxo (testes unitários, exemplo de configuração, artefactos locais).

## What Changes

- **Documentação Docker-first**: reescrever `README.md` e `docs/` para documentar **apenas** Docker como método de correr/servir o servidor; remover todas as instruções de execução manual (`venv` + `uvicorn`), incluindo a "Opção B — Servidor fora de Docker" do guia Nginx Proxy Manager.
- **Remover ficheiros versionados irrelevantes**:
  - `elisdb-server/test_main.py` e `elisdb-server/test_provision.py` (testes unitários).
  - `elisdb-server/demo.config.example.json` (exemplo de configuração).
- **Remover artefactos locais ignorados** do disco: `.venv/`, `__pycache__/`, `.pytest_cache/`, `elisdb.db`, `photos/`.
- **`requirements.txt`**: remover `pytest` e `httpx`, que só eram usados pelos testes removidos.
- **Atualizar referências de contexto** (`.gitignore`, docs, metadados) que apontem para testes ou execução manual, de modo a não deixarem instruções obsoletas.

## Capabilities

### New Capabilities

_Nenhuma_ — mudança de documentação e limpeza de ficheiros; sem comportamento novo.

### Modified Capabilities

- `server-key-provisioning`: remover o requisito "Example configs contain no live secrets" — deixa de existir qualquer ficheiro de configuração de exemplo/demo commitado, pelo que o requisito (e os seus cenários) ficam obsoletos. A proteção passa a ser garantida simplesmente por não haver exemplo commitado e por `*.config.json` continuar no `.gitignore`.

## Impact

- `elisdb-server/README.md`, `docs/` (index, instalacao, execucao, configuracao, uso, nginx-proxy-manager).
- Remoção de `test_main.py`, `test_provision.py`, `demo.config.example.json`.
- `requirements.txt` (dependências de teste removidas).
- Artefactos locais apagados do disco (já ignorados pelo git).
- Sem impacto no código de runtime (`app/*.py`), no `Dockerfile`, `compose.yaml`, `docker-entrypoint.sh`, nem na API ou no esquema de dados.
