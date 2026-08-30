## Context

Ver `proposal.md` — Why. O servidor já tem a infraestrutura Docker completa (`Dockerfile`, `compose.yaml`, `docker-entrypoint.sh`, `.dockerignore`). A mudança é sobretudo de documentação e remoção de ficheiros, sem tocar no código de runtime nem no comportamento.

## Goals / Non-Goals

**Goals:**
- Documentar o servidor com Docker como **método único**, com um percurso claro: instalar → subir → provisionar utilizadores → expor (opcional).
- Remover ficheiros mortos (testes, exemplo de config, artefactos locais) e as dependências que só eles usavam.

**Non-Goals:**
- Não alterar `app/*.py`, `Dockerfile`, `compose.yaml`, `docker-entrypoint.sh`, API, auth, persistência ou provisionamento.
- Não introduzir novas dependências nem alterar versões.

## Decisions

- **Docker como método único na documentação**: em vez de manter uma secção "Docker (alternativa)", reescrever `instalacao.md` (pré-requisitos: Docker + Docker Compose) e `execucao.md` (subir, healthcheck, logs, paragem, provisionamento via `compose`). As instruções `venv`/`uvicorn` saem por completo.
  - *Alternativa considerada:* manter o manual como "modo desenvolvimento". Rejeitada — o pedido é Docker-first com método único; menos documentação para manter em sincronia.
- **Remover testes e dependências de teste**: `test_main.py` e `test_provision.py` saem, e `pytest`/`httpx` saem de `requirements.txt` (só eram usados nos testes).
  - *Alternativa considerada:* manter os testes por qualidade. Rejeitada por decisão explícita do utilizador.
- **Remover `demo.config.example.json`**: o `.gitignore` referia-o como "placeholder example"; sem ele, a regra `*.config.json` continua a proteger os ficheiros de chave gerados. Remove-se também a menção no comentário do `.gitignore`. A remoção torna obsoleto o requisito "Example configs contain no live secrets" de `server-key-provisioning` → é removido por delta spec.
- **Artefactos locais**: apagar `.venv/`, `__pycache__/`, `.pytest_cache/`, `elisdb.db`, `photos/` do disco (todos já ignorados; só ocupam espaço local e podem confundir).
- **Guia Nginx Proxy Manager**: ficar só com a "Opção A" (servidor em Docker na mesma rede); remover a "Opção B — fora de Docker".

## Risks / Trade-offs

- [Perda de cobertura de testes do servidor] → aceite por decisão do utilizador; o comportamento não muda, apenas deixa de haver testes automatizados de servidor no repo.
- [Documentação obsoleta em pontos não óbvios] → varrer `docs/` e `README.md` inteiros por referências a `venv`, `uvicorn`, `pytest` e atualizar `index.md` para não apontar para passos removidos.
- [Instruções de contexto desatualizadas] → atualizar referências em `openspec/config.yaml` (comando de teste do servidor) para não apontar para `pytest` removido.

## Migration Plan

Nenhum — mudança de docs e ficheiros, sem dados nem esquema. Aplicável diretamente na branch de trabalho.
