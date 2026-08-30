## Context

Ver `proposal.md` — Why. O servidor já só é documentado em Docker; esta mudança elimina os últimos artefactos que permitiam uma execução fora de Docker e torna o contrato explícito.

## Goals / Non-Goals

**Goals:**
- Fazer do `Dockerfile` a única fonte de verdade das dependências do servidor.
- Remover o caminho de instalação manual (`requirements.txt` + `.venv`).
- Afirmar explicitamente "Docker-only" na documentação.

**Non-Goals:**
- Não alterar `app/*.py`, `compose.yaml`, `docker-entrypoint.sh`, API, auth ou persistência.
- Não introduzir novas dependências nem alterar versões (mantêm-se `fastapi>=0.111`, `uvicorn[standard]>=0.30`, `cryptography>=43.0`).

## Decisions

- **Incluir as dependências no `Dockerfile`** em vez de manter `requirements.txt`. O build multi-stage já usa `pip install --prefix=/install`; passa-se a listar os pacotes diretamente no `RUN`.
  - *Alternativa considerada:* manter `requirements.txt` como manifesto do Docker. Rejeitada — é precisamente o que permite o `pip install` fora de Docker e mantém um segundo ponto de verdade.
- **Remover `.venv/` do `.gitignore`**: num servidor Docker-only ninguém cria venv; manter a entrada normaliza um fluxo que deixou de existir. Mantêm-se `__pycache__/` e `*.pyc` (higiene Python genérica).
- **Documentação**: usar "corre **exclusivamente/only** em Docker" no `README.md`, `docs/index.md`, `docs/instalacao.md`, `docs/execucao.md` e `openspec/config.yaml`, sem margem para alternativa manual.

## Risks / Trade-offs

- [Quem quiser desenvolver/testar localmente perde o `pip install` rápido] → aceite: o servidor é Docker-only; o ciclo de desenvolvimento é via `docker compose up --build`.
- [Esquecer uma referência a `requirements.txt` noutro sítio] → varrer o repo por `requirements.txt` e `pip install` após a mudança.

## Migration Plan

Nenhum — mudança de artefactos de build e documentação, sem dados nem esquema.

## Open Questions

Nenhuma.
