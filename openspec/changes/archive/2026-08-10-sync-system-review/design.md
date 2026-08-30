## Context

Este é um review (audit) read-only do sistema de sincronização. A máquina de trabalho já vista no código:

- `SyncCoordinator` faz `pushEdits()` → `pushDeletions()` → `pull()`, com o servidor a ser a autoridade de conflitos (`last-write-wins`).
- O app marca registos como `pending`/`deletePending` e só os liberta após confirmação do servidor.
- O transporte (`ApiClient`) assina cada pedido (timestamp + assinatura ed25519).
- O `SyncManager`/`SyncWorker` agendam sync periódico (WorkManager) e manual.
- Especificações em `openspec/specs/client-sync|client-sync-state|server-sync`.

O objetivo não é fazer alterações — é produzir um relatório em português com achados, riscos e recomendações priorizadas (esforço × impacto), e uma conclusão sobre se o sistema está adequado como está.

## Goals / Non-Goals

Goals:
- Revisar o código e os specs contra os cenários de aplicação real (correção, robustez, segurança, escala).
- Produzir um relatório navegável em `docs/reviews/`, com secções temáticas, verificações executadas, achados, gravidade e recomendações com esforço/impacto.
- Concluir explicitamente por um veredito: "adequado como está" vs. "melhorias prioritárias".

Non-Goals:
- Não alterar código, comportamento, specs, ou scripts.
- Não reparar nem implementar otimizações nesta change.
- Não abrir issues/PRs; o relatório é o entregável único.

## Decisions

- **Relatório em `docs/reviews/sync-system-review.md`**, dentro do repositório da app (junto ao código). Alternativa: dentro do `openspec/changes/…` — rejeitado porque o review permanece útil depois do archive, é parte da documentação do projeto.
- **Estrutura de secções fixa**: Contexto → Metodologia → Achados (por área, cada um: descrição, evidência (ficheiro/linha), gravidade × impacto, recomendação) → Recomendações priorizadas → Veredito. Alternativa: relatório livre — rejeitada para garantir comparabilidade.
- **Avaliação com critério objetivo**: cada achado é verificado contra a spec correspondente (`client-sync`, `server-sync`, etc.) e contra o código no momento atual; nenhum "risco" sem citação.
- **Sem alteração de código**: a change é `skip_specs: true` e apenas documento.

## Risks / Trade-offs

- **Posição mudada no meio da análise** (código a evoluir durante o review) → o relatório data e versão dos ficheiros/specs avaliados.
- **Veredito subjetivo** → mitigado com critérios explícitos de gravidade (bloqueante / melhorar a médio prazo / cosmético).

## Migration Plan

Sem migração — artefacto de documentação apenas; sem deployment.

## Open Questions

- Nenhuma.