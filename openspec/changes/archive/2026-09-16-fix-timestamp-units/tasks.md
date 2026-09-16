## 1. Server

- [x] 1.1 Adicionar migração idempotente ms→s em `app/database.py` para `clientes.updated_at`, `clientes.created_at` e `client_audit.ts`; verificar que valores ms são convertidos uma vez e valores s ficam intactos
- [x] 1.2 Ajustar a validação de `updated_at` futuro em `save_client` para segundos (sem `* 1000`); verificar que um `updated_at` em ms antigo não é aceite como future-bound errado

## 2. Cliente Android

- [x] 2.1 Gerar `updatedAt`/`createdAt`/`dataCriacao` em segundos em `ClienteRepositoryImpl` (`nowEpochSeconds`) e `SyncCoordinator.mergeDuplicate`; verificar com os testes unitários
- [x] 2.2 Adicionar `MIGRATION_7_8` (ms→s para `updatedAt`, `createdAt`, `dataCriacao` e watermark) e subir a versão da BD para 8; verificar a migração
- [x] 2.3 Exibir `Date(entry.ts * 1000)` no histórico (`ClientDetailScreen`); verificar compilação
