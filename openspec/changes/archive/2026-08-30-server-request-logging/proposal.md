## Why

O servidor não tem qualquer configuração de `logging` — só existem entradas de auditoria na base. Pedidos, erros e exceções não são registados em lado nenhum, pelo que falhas de sync, 401s e 500s em produção são invisíveis para o operador (o `docker-entrypoint.sh`/compose não têm config de logs para além do default do uvicorn). Para um serviço auto-hospedado isto dificulta o diagnóstico.

## What Changes

- Configurar logging estruturado no arranque (módulo `logging`, nível configurável por env), cobrindo pelo menos: arranque/startup, erros não tratados (500), e rejeições de auth (401) de forma agregada.
- Não registar segredos (nem seeds, nem corpos de foto).

## Capabilities

### New Capabilities
- `server-observability`: nova capability — o servidor SHALL registar eventos operacionais (arranque, erros, rejeições de auth) via logging estruturado, sem expor segredos.

### Modified Capabilities

## Impact
- Servidor: `app/main.py` (setup de logging no lifespan/startup), possível endpoint de health já existe. Sem alteração de protocolo.
- Ops: logs disponíveis no stdout do contentor para recolha pelo orchestrator.
