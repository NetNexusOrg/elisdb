## 1. Remover ficheiros versionados irrelevantes

- [x] 1.1 Apagar `elisdb-server/test_main.py` e `elisdb-server/test_provision.py`
- [x] 1.2 Apagar `elisdb-server/demo.config.example.json`
- [x] 1.3 Remover `pytest` e `httpx` de `elisdb-server/requirements.txt`
- [x] 1.4 Atualizar o comentário do `elisdb-server/.gitignore` que refere o `demo.config.example.json`

## 2. Remover artefactos locais ignorados

- [x] 2.1 Apagar `.venv/`, `__pycache__/`, `.pytest_cache/`, `elisdb.db` e `photos/` do disco em `elisdb-server/`

## 3. Reescrever a documentação para Docker-first

- [x] 3.1 `README.md`: início rápido em Docker; remover instruções `venv`/`uvicorn`; remover secção de testes
- [x] 3.2 `docs/index.md`: guias reordenados e resumo em Docker; remover `venv`/`uvicorn`
- [x] 3.3 `docs/instalacao.md`: pré-requisitos Docker + Docker Compose; remover `venv`/`pytest`
- [x] 3.4 `docs/execucao.md`: Docker como método único; remover secções `venv`/`uvicorn` e "Docker (alternativa)"
- [x] 3.5 `docs/configuracao.md`: exemplo de env vars sem `.venv/bin/uvicorn`; comandos de emissão de chave via `docker compose exec`
- [x] 3.6 `docs/uso.md`: substituir referências a execução manual por Docker
- [x] 3.7 `docs/nginx-proxy-manager.md`: remover "Opção B — Servidor fora de Docker (ex.: `.venv`)"

## 4. Atualizar referências de contexto

- [x] 4.1 Atualizar `openspec/config.yaml` (comando de teste do servidor) para não referir `pytest`
- [x] 4.2 Varrer o repo por referências obsoletas a `venv`, `uvicorn`, `pytest`, `test_main`/`test_provision` no contexto do servidor e corrigir

## 5. Delta spec (descoberto durante a implementação)

- [x] 5.1 Criar `specs/server-key-provisioning/spec.md` a remover o requisito "Example configs contain no live secrets" (obsoleto com a remoção de `demo.config.example.json`)
- [x] 5.2 Remover `skip_specs: true` do `.openspec.yaml` e registar a capability modificada no `proposal.md`
