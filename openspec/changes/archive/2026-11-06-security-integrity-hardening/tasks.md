# Tasks

## 1. Provisionamento e seeds

- [x] 1.1 Adicionar `print_seed=False` a `issue_and_register` e garantir que o provisionamento nunca imprime a seed; verificar com teste que captura stdout/stderr e procura a seed.
- [x] 1.2 Adicionar a flag explícita `--print-seed` à CLI manual e manter o output normal limitado a `key_id`, `user_name` e caminho; verificar com teste de CLI.
- [x] 1.3 Implementar `safe_config_path` com allowlist e confinamento ao diretório de configs; verificar com testes de `../`, separadores, nomes válidos e revogação.
- [x] 1.4 Documentar a rotação de seeds potencialmente expostas e atualizar `docs/seguranca.md`.

## 2. Fotos: referência, quotas e GC

- [x] 2.1 Migrar `fotos` com `uploaded_by_key` e `size_bytes` de forma idempotente e fazer backfill dos ficheiros existentes; verificar com teste de migração e contagem de bytes.
- [x] 2.2 Rejeitar `PUT /api/photos/{id}` sem linha `fotos`/reserva e devolver erro claro; verificar com teste de id desconhecido e de id referenciado.
- [x] 2.3 Implementar quotas por chave e por instância antes da escrita, com 413/429 e sem criar ficheiro; verificar com testes de quota excedida e de upload dentro do limite.
- [x] 2.4 Adicionar o GC periódico ao lifespan com intervalo configurável e lock; verificar com teste que um órfão antigo é removido sem push e que um ficheiro referenciado sobrevive.
- [x] 2.5 Atualizar a documentação de armazenamento e as variáveis de ambiente.

## 3. LWW de deleções

- [x] 3.1 No servidor, comparar `deleted_at` com o `updated_at` guardado dentro de `BEGIN IMMEDIATE` e devolver 409/stale sem apagar nem criar tombstone; verificar com testes de delete antigo e delete mais recente.
- [x] 3.2 Rejeitar `deleted_at` negativo ou acima do skew permitido; verificar com testes de validação.
- [x] 3.3 No Android, comparar o tombstone com o `updatedAt` local, preservar edições mais recentes e re-enviá-las; verificar com testes de tombstone antigo e de registo pending.
- [x] 3.4 Documentar a semântica de LWW para deleções no servidor e no cliente.

## 4. Reset de configuração e sync em curso

- [x] 4.1 Adicionar uma geração ao `SyncManager` e fazê-la atravessar o coordinator; verificar com teste de que um resultado da geração antiga é descartado.
- [x] 4.2 Fazer `resetState` cancelar e aguardar `inFlight` antes de limpar watermark/hashes/clock; verificar com teste de corrida com sync lento.
- [x] 4.3 Garantir que `clearConfig`/import só arrancam o novo sync depois do reset; verificar com teste de integração do ViewModel.
- [x] 4.4 Documentar o comportamento e o tempo máximo de espera.

## 5. Fotos staged e sweep

- [x] 5.1 Criar um `StagedPhotoRegistry` persistido e registar/desregistar paths em `stagePhoto`, `createPhotoFile`, `save`, remoção e `onCleared`; verificar com testes de ciclo de vida.
- [x] 5.2 Fazer o sweep ignorar paths registados e aplicar uma carência configurável; verificar com teste de staging + sweep concorrente e de órfão antigo.
- [x] 5.3 Garantir que o sweep continua limitado por lote e que não bloqueia indefinidamente o pull; verificar com teste de batches.
- [x] 5.4 Documentar a política de retenção de ficheiros staged.

## 6. Verificação integrada

- [x] 6.1 Correr a suite do servidor (`pytest -q`) e garantir que os testes de auth, provisionamento, fotos e LWW passam.
- [x] 6.2 Correr os testes Android de sync, repositório e photo store.
- [x] 6.3 Validar a change com `openspec validate security-integrity-hardening`.

## Workflow follow-up

- Arquivar a change depois da implementação e da revisão.
