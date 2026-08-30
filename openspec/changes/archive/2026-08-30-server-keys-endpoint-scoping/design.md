## Context

`GET /api/keys` (`app/main.py:149-151`) devolve a allowlist completa a qualquer dispositivo autenticado. A gestão de chaves (listar/remover) é, por design, operador (CLI, ver `server-key-provisioning`). Expor a lista a dispositivos é fuga de informação e superfície de abuso. A app Android usa `/api/keys/me`, não `/api/keys`.

## Goals / Non-Goals

**Goals:**
- `/api/keys` (listagem completa) deixa de estar acessível a dispositivos cliente arbitrários.
- `/api/keys/me` (identidade própria) mantém-se.

**Non-Goals:**
- Não alterar a issue/provisioning CLI (operador continua a listar).
- Não mudar `delete_client` (dispositivos continuam a poder eliminar os seus clientes).

## Decisions

**D1: Restringir `/api/keys` a operador**
Opção A: remover o endpoint `/api/keys` do API de dispositivo e manter a listagem só no CLI. Opção B: exigir um header/escopo de operador. Como a gestão já é via CLI, a Opção A é mais simples e segura: remover `GET /api/keys` do servidor (ou devolver 403 para dispositivos). O CLI lê a DB diretamente.
- *Alternativa*: manter endpoint mas exigir papel — rejeitada, o modelo atual não tem papéis; CLI é o caminho operador.

**D2: Manter `/api/keys/me`**
Inalterado; dispositivos continuam a obter a própria identidade a partir de `request.state` (ver `server-fix-request-state-auth`).

## Risks / Trade-offs
- [Remover `/api/keys` pode quebrar algum cliente que o use] → a app Android não o usa (usa `/api/keys/me`); risco baixo.
- [Operador em container perde listagem web] → o CLI continua a listar; documentar.

## Migration Plan
- Sem migração. Remoção de um endpoint não usado pela app.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
