## 1. Recolha de contexto

- [x] 1.1 Reler os specs `client-sync`, `client-sync-state` e `server-sync` e anotar os requisitos-chave a verificar.
- [x] 1.2 Revisitar o código de sync da app (`SyncCoordinator`, `SyncManager`, `SyncWorker`, `SyncApi`, `SyncMapper`, `FilePhotoStore`) e o transporte (`ApiClient`, `Signer`, `AuthHeaders`).
- [x] 1.3 Revisitar o lado servidor (`main.py` para push/pull/delete, `database.py` para last-write-wins e duplicados, `auth.py` para a janela de timestamp) e os DTOs/`updatedAt`.

## 2. Análise por área

- [x] 2.1 Correção/consistência: duplicados, `stale`, remoção local vs servidor, concorrência push/pull, `last-write-wins`, coerência de `updatedAt` como `dataCriacao`.
- [x] 2.2 Perda/ganho de dados: fotos órfãs em disco, fotos sem ficheiro, `localClients.find` dentro da transação, e reposição de fotos no `pull` (base de dados + disco).
- [x] 2.3 Segurança da assinatura: janela de `timestamp` (`currentTimeMillis()/1000`), reuso de chave, headers obrigatórios, risco de `401` por relógio dessincronizado.
- [x] 2.4 Robustez: `AuthException` vs `TransientError`, backoff do WorkManager, concorrência `syncNow` × `SyncWorker` periódico, estados Room, desinstalação/reinstalação.
- [x] 2.5 Escala/performance: pull completo com fotos em base64, fotos re-gravadas em cada sync, ausência de paginação, lote de push ilimitado.

## 3. Elaboração do relatório

- [x] 3.1 Criar `docs/reviews/sync-system-review.md` com a estrutura fixada no design: Contexto → Metodologia → Achados → Recomendações → Veredito.
- [x] 3.2 Registar cada achado com descrição, evidência (ficheiro/linha), gravidade (bloqueante / grave / melhorar a médio prazo / cosmético), esforço × impacto, e recomendação.
- [x] 3.3 Redigir o veredito final ("adequado como está" vs "melhorias prioritárias") e a lista priorizada de recomendações.

## 4. Verificação

- [x] 4.1 Confirmar que o relatório está descrito, que todas as referências (ficheiro/linha) existem e que nenhuma chave/segredo real entrou.
- [x] 4.2 Confirmar que nenhum código ou spec foi alterado; apenas o novo relatório foi adicionado.