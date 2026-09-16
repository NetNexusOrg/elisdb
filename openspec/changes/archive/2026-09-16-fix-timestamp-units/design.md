## Context

Motivação no `proposal.md`. O cliente Android gera `updatedAt`/`createdAt`/`dataCriacao` com `nowMillis()` (ms) e `deletedAt`/timestamps de pedido com `normalizedEpochSeconds()` (s). O servidor guarda os valores como chegam, e o `/api/time` devolve segundos. A unidade pretendida (segundo o `project-context` e os timestamps de pedido) é **seconds**.

## Goals / Non-Goals

**Goals:**
- Convergir todos os timestamps de dados para epoch seconds, no cliente e no servidor.
- Migrar as bases existentes (Room e SQLite) de ms para s de forma idempotente e não-destrutiva.

**Non-Goals:**
- Não introduzir versionamento de migrações no servidor (fora de âmbito; usa-se a heurística de limiar para idempotência).
- Não alterar os timestamps de pedido (`X-Elisdb-Timestamp`), que já são seconds.

## Decisions

**D1: Normalizar para seconds (não ms)**
A camada de auth, `/api/time`, `deleted_at` e o `clockOffsetStore` já usam seconds. Migrar os campos de dados para seconds alinha com a convenção existente e evita tocar na camada de auth.
- *Alternativa*: normalizar tudo para ms — rejeitada porque obrigaria a mudar auth e `/api/time`.

**D2: Migração idempotente por limiar**
Como o servidor não tem `PRAGMA user_version`, a conversão usa um limiar (`> 100_000_000_000`) para distinguir ms de s: valores ms (~1.7e12) são divididos por 1000; valores s (~1.7e9) ficam intactos. Isto é naturalmente idempotente (após converter, os valores ficam abaixo do limiar). Aplicado a `clientes.updated_at`, `clientes.created_at` e `client_audit.ts`; `tombstones.deleted_at` e `keys.created_at` já são seconds.
- *Alternativa*: introduzir `user_version` — rejeitada por ser uma refactorização maior, adiada.

**D3: Migração Room 7→8**
`MIGRATION_7_8` divide por 1000 as colunas `updatedAt`, `createdAt`, `dataCriacao` (com o mesmo limiar, por segurança) e o watermark `sync_meta` (`last_pull_watermark`), que antes era dominado pelo valor ms.

**D4: Validação de futuro em seconds**
A validação de `updated_at` futuro (adicionada na change anterior) passa a comparar `updated_at > now_s + MAX_FUTURE_SKEW_SECONDS` (sem `* 1000`), porque o campo passa a ser seconds.

**D5: Exibição do histórico**
`ClientDetailScreen` passa a renderizar `Date(entry.ts * 1000)` porque `ts` passa a ser seconds.

## Risks / Trade-offs

- [Dados antigos em ms sem migração ganham o LWW] → mitigado pelas migrações D2/D3 em ambos os lados.
- [Heurística de limiar pode errar para timestamps absurdos (< 1973)] → os dados do projeto são de 2026; limiar 1e11 é seguro.
- [Migração no servidor corre no arranque sobre a base real] → transformação não-destrutiva e idempotente (só divide valores > limiar).
