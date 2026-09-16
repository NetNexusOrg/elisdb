## Why

Os timestamps de dados estão em escalas inconsistentes: o cliente Android envia `updated_at`/`created_at` em **milliseconds** (`nowMillis()`) mas `deleted_at` e os timestamps de pedido em **seconds** (`normalizedEpochSeconds()`). O servidor guarda os valores tal como chegam, o que corrompe o `latest_updated_at` (max entre ms e s), faz com que os tombstones nunca sejam devolvidos em pulls incrementais (`deleted_at > since` com `since` em ms), e deixa o last-write-wins a comparar unidades diferentes.

## What Changes

- Normalizar todos os timestamps de dados para **epoch seconds** (a unidade já usada pelos timestamps de pedido, pelo `/api/time` e pelos `deleted_at`).
- No cliente Android: gerar `updatedAt`/`createdAt`/`dataCriacao` em segundos; migrar a base Room existente de ms para s.
- No servidor: migrar `clientes.updated_at`/`clientes.created_at` e `client_audit.ts` de ms para s; ajustar a validação de futuro para segundos.
- O cliente passa a exibir o `ts` do histórico de edição como segundos (`Date(ts * 1000)`).

## Capabilities

### New Capabilities

### Modified Capabilities

- `server-sync`: todos os timestamps de dados persistidos e trocados (`updated_at`, `created_at`, `deleted_at`, `ts` do histórico) são epoch seconds.

## Impact

- Servidor: `app/database.py` (migração ms→s + validação de futuro em segundos).
- Cliente Android: `data/ClienteRepositoryImpl.kt`, `sync/SyncCoordinator.kt`, `data/local/AppDatabase.kt` (migração Room 7→8), `ui/detalhe/ClientDetailScreen.kt`.
- **BREAKING** nos timestamps existentes: ambas as bases (Room e SQLite do servidor) precisam de migração ms→s; sem ela, os valores antigos (ms) ganhariam o LWW e seriam rejeitados pela validação de futuro.
