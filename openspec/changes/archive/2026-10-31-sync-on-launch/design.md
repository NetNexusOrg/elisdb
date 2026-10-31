# Design

## Context

Ver `proposal.md` — Why. O `ElisdbApp.onCreate` já arranca trabalho de fundo no
`container.applicationScope` (limpeza de fotos órfãs e medição do offset do
relógio). O `SyncManager.syncNow()` é single-flight, corre em `runScope` e publica
o estado observável; o `SyncCoordinator` mede o offset no início de cada sync.

## Goals / Non-Goals

**Goals:**

- Disparar um sync por arranque, sem bloquear o primeiro frame.
- Não incomodar quem usa a app sem servidor configurado.
- Reutilizar o caminho de sync existente, sem WorkManager nem alarmes.

**Non-Goals:**

- Sincronizar ao voltar do background ou em intervalos.
- Sincronização silenciosa que esconda falhas: o estado continua visível.
- Mudar o protocolo, o servidor ou a base de dados.

## Decisions

### Gatilho no arranque da Application

`ElisdbApp.onCreate` lança `initialSyncIfConfigured(...)` no
`container.applicationScope`, depois de criar o `AppContainer`. Alternativa:
`MainActivity`/primeiro ecrã — rejeitada porque rotações e recriações de Activity
repetiriam o gatilho; a `Application` corre uma vez por processo.

### Gate pela chave de assinatura

A função lê `settingsStore.settings().signingKey` e só chama `syncNow()` quando
não está em branco. É o mesmo critério do ecrã de configuração. Alternativa:
deixar o sync falhar com `ConfigFault` — rejeitada porque pintaria um erro de
configuração no arranque de quem nunca configurou servidor.

### Sem espera pelo offset do relógio

O sync já mede o offset no início da execução; não se encadeia o arranque na
medição feita pela `Application`, para não atrasar o sync se essa chamada falhar.
As duas corridas são inofensivas.

### Testabilidade

Extrair a decisão para uma função `internal suspend fun initialSyncIfConfigured(
settingsStore: SettingsStore, syncNow: suspend () -> Unit)` no ficheiro da
`Application`, testável com um `SettingsStore` fake: com chave chama uma vez, sem
chave não chama.

## Risks / Trade-offs

- **Erro visível logo no arranque quando não há rede** → comportamento igual ao
  sync manual; o utilizador pode continuar offline e o estado explica a falha.
- **Tráfego ao abrir a app várias vezes** → uma vez por processo, não por
  foreground; abrir a app de propósito já é uma ação do utilizador.
- **Dois pedidos de hora em paralelo** (offset do arranque + offset do sync) →
  sem efeito funcional; ambos são leituras idempotentes.

## Migration Plan

Só app. Publicar uma versão; não há migração de dados nem alteração do servidor.
Rollback é remover a chamada no arranque.

## Open Questions

Nenhuma.
