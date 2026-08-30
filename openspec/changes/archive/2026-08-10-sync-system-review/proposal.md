## Why

O sistema de sincronização (client-sync, client-sync-state, server-sync) está completo e funcional, mas nunca foi objeto de uma revisão crítica estruturada. Antes de decidir entre "está bom como está" ou "há o que melhorar", vale a pena avaliar o design atual de forma fundamentada — com foco em correção (perda de dados, conflitos), robustez (erros, retries) e escala (fotos, lotes).

## What Changes

- Produz um **relatório de revisão** do sistema de sincronização (documento `docs/reviews/sync-system-review.md` ou semelhante), em português, concluindo se o sistema está adequado ou se há melhorias recomendadas.
- A revisão cobre, no mínimo:
  - **Correção/consistência**: cenários de duplicado, `stale`, remoção local vs. servidor, concorrência entre push/pull, `last-write-wins`, coerência de `updatedAt` (o app usa `updatedAt` como `dataCriacao` — verificar o que acontece num update em que o servidor devolve outra fonte).
  - **Perda/ganho de dados**: o que acontece a fotos órfãs em disco, fotos sem ficheiro, `localClients.find` dentro de transaction, e reposição de fotos no `pull`.
  - **Segurança da assinatura**: janela de `timestamp` (`System.currentTimeMillis() / 1000`), reuso de chave, headers, e risco de rejeição por relógio dessincronizado.
  - **Robustez**: tratamento de `AuthException` vs `TransientError`, backoff do WorkManager, concorrência (`syncNow` em paralelo com `SyncWorker` periódico), estados persistentes no Room, e o que acontece quando a app é desinstalada/reinstalada.
  - **Escala/performance**: `pull` sempre completo (todas as fotos em base64), fotos re-gravadas em disco a cada sync, ausência de paginação, lote de push ilimitado.
  - **Observabilidade**: estado exposto, métricas/falhas sem telemetria, mensagens da UI.
- **Não altera código**: documenta apenas o relatório, com achados, riscos e recomendações priorizadas (cada um com esforço/impacto). Nenhuma alteração de comportamento é feita nesta change.
- Se forem encontrados problemas críticos (ex.: possível perda de dados), o relatório marca-os como bloqueantes e aponta as correções sugeridas para changes futuras.

## Constraints

- Revisão sem modificar o comportamento atual do sistema (app ou servidor).
- Saída principal: um documento de revisão em português; nada de alteração de `main`/configs/scripts.
- Não registar segredos nem chaves reais no relatório.

## Impact

- Só acrescenta um artefacto de documentação (relatório de revisão) — nenhum código ou spec muda.
- `skip_specs: true`: a revisão não altera requisitos, logo não há delta de specs a sincronizar.