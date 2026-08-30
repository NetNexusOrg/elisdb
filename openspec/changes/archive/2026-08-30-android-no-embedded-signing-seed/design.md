## Context

`build.gradle.kts:22-25` injeta `ELISDB_PRIVATE_KEY` em `BuildConfig.SIGNER_PRIVATE_KEY_B64`. Se definido no build, uma seed real vai para o APK e é partilhada por todas as instalações. A importação por dispositivo (`client-key-import`) é o caminho correto.

## Goals / Non-Goals

**Goals:**
- A app NUNCA autentica com uma seed embutida no build.
- A seed vem sempre de importação por dispositivo.

**Non-Goals:**
- Não remover a capacidade de build assinar em dev, mas essa seed NUNCA pode ser usada para auth real em release.
- Não mudar o protocolo de assinatura.

## Decisions

**D1: Ignorar seed do BuildConfig para auth**
O código que consome `BuildConfig.SIGNER_PRIVATE_KEY_B64` para assinar pedidos é removido; a única fonte de signing key é o `SettingsStore` (importada). O `BuildConfig` pode manter a constante mas não é usada para auth.
- *Alternativa*: usar a seed do BuildConfig como fallback — rejeitada, é exatamente o anti-pattern.

**D2: Fault se não houver chave importada**
Se não houver chave importada, o sync reporta configuration fault (já existe em `client-sync` "Sync result distinguishes configuration faults"), em vez de assinar com seed embutida.

## Risks / Trade-offs
- [Builds de dev perdem atalho de seed] → aceitável; importar o config é o caminho documentado.

## Migration Plan
- Sem migração. Apenas build/auth.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
