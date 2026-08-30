## Context

Não há configuração de `logging` no servidor; só auditoria na base. Falhas de sync (401/500) são invisíveis em produção. O `docker-entrypoint.sh`/compose não configuram logs para além do default do uvicorn.

## Goals / Non-Goals

**Goals:**
- Logging estruturado no arranque (nível por env).
- Registar startup, 500, e 401 agregados.

**Non-Goals:**
- Não criar sistema de métricas/externalização (stdout basta para o orchestrator).
- Não registar segredos nem corpos de foto.

## Decisions

**D1: Logging no lifespan/startup**
Em `app/main.py` (lifespan ou startup), configurar `logging.basicConfig` (ou dictConfig) com nível de `ELISDB_LOG_LEVEL` (default INFO). Registar startup. Intercetar exceções não tratadas (exception handler ou middleware) para logar 500 sem corpo.

**D2: Não logar segredos**
Nunca logar o `private_seed`, chaves, nem `request.body()` de fotos. Logs de 401 registam apenas método/path/host, não a assinatura nem o body.

## Risks / Trade-offs
- [Logs de 401 muito verbosos sob ataque] → nível INFO/aviso; operador pode subir para DEBUG se quiser. Sem PII.

## Migration Plan
- Sem migração.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
