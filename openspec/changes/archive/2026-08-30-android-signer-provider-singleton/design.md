## Context

`Signer.init` (`util/Signer.kt:27`) faz `Security.addProvider(EdDSASecurityProvider())` em cada construção de `Signer`. Como um `Signer` é criado por sync/fetch, o provider acumula duplicados na lista estática `Security`.

## Goals / Non-Goals

**Goals:**
- Registar o provider uma única vez por processo.

**Non-Goals:**
- Não alterar o algoritmo de assinatura nem o comportamento.

## Decisions

**D1: Registo lazy/idempotente**
Substituir o `addProvider` por um guarda (flag `AtomicBoolean` ou `synchronized`) ou verificar `Security.getProvider("EdDSA")` antes de adicionar. Ou registar o provider num `companion object` inicializado uma vez.

## Risks / Trade-offs
- [Condição de corrida no registo] → usar `synchronized`/`AtomicBoolean`; adicionar é idempotente e barato.

## Migration Plan
- Sem migração.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
