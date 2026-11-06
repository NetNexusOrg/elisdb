# Design

## Context

Ver `proposal.md` — Why. O provisionamento usa `keys.issue_and_register`, que imprime sempre a seed; os nomes de utilizador entram em paths; o upload de fotos não exige referência; a deleção não consulta o `updated_at`; o reset de configuração não cancela o sync; e o sweep não conhece fotos staged. As correções tocam servidor, Android e o contrato de push.

## Goals / Non-Goals

**Goals:**

- Eliminar fuga de seeds e escrita/eliminação fora do diretório de configs.
- Impedir uploads não referenciados e limitar o consumo de disco por chave/instância.
- Fazer a deleção competir em LWW no servidor e no cliente.
- Tornar a troca de configuração atómica face ao sync em curso.
- Proteger fotos staged/capturadas do sweep.

**Non-Goals:**

- Não introduzir ownership por cliente (o modelo continua de grupo de confiança).
- Não rever o formato das fotos nem o algoritmo de hash.
- Não implementar quotas de CPU/memória.
- Não substituir o single-worker do servidor.

## Decisions

### 1. Seed só no ficheiro, nunca no log

`issue_and_register` passa a ter `print_seed: bool = False`. O caminho de provisionamento usa sempre `False`; a CLI manual só imprime a seed com a flag explícita `--print-seed`. O output normal mostra `key_id`, `user_name` e o caminho do ficheiro `0600`.

**Alternativas:** remover a impressão por completo (quebra o fluxo manual atual); imprimir a seed apenas uma vez (continua a ir para logs de CI/containers). A flag explícita preserva o fluxo manual sem exposição por omissão.

### 2. Caminhos de configuração confinados

Uma função `safe_config_path(configs_dir, name)` valida `name` contra `[A-Za-z0-9._-]+`, rejeita `.`/`..`, resolve o caminho com `Path.resolve()` e verifica que está dentro de `configs_dir.resolve()`. É usada na criação, na revogação e no `keys.py`.

**Alternativas:** escapar caracteres (continua a permitir colisões); confiar no operador (o objetivo é defender contra erros e comentários importados). A allowlist é simples e testável.

### 3. Upload de fotos referenciado e quota

O P0 introduz o push em duas fases: o registo é aceite primeiro e as fotos em falta são uploadadas depois. O servidor só aceita `PUT /api/photos/{id}` se existir uma linha `fotos` para esse id. A tabela `fotos` ganha `uploaded_by_key` e `size_bytes` (migração idempotente). A quota por chave é a soma de `size_bytes` do uploader; a quota global é a soma total. Antes de escrever, o servidor verifica ambas e responde 413/429 sem criar ficheiro. A quota é configurável por `ELISDB_MAX_PHOTO_BYTES_PER_KEY` e `ELISDB_MAX_PHOTO_BYTES_TOTAL`.

Se o P0 ainda não estiver implementado, o upload passa a exigir uma reserva explícita criada pelo push; a ordem upload-first deixa de ser aceite. Isto é uma dependência deliberada: o fluxo seguro é metadados primeiro.

**Alternativas:** quota por IP (não representa o custo por chave); quotas apenas por cliente (um cliente pode ter muitas fotos); GC mais frequente (não impede o pico). A contabilidade por chave é a unidade de autenticação existente.

### 4. GC periódico

Além do arranque e do pós-push, um task periódico no lifespan do FastAPI chama `gc_orphan_photos`. O intervalo é configurável (`ELISDB_PHOTO_GC_INTERVAL`, default 3600 s) e a carência mantém-se. O task usa um lock para não sobrepor passes e é cancelado no shutdown.

**Alternativas:** cron externo (mais uma peça operacional); GC em cada pedido (custo); confiar no pós-push (não cobre servidores sem pushes).

### 5. Deleção LWW no servidor e no cliente

No servidor, `delete_client` lê o `updated_at` da linha dentro da transação `BEGIN IMMEDIATE`. Se `deleted_at <= updated_at`, devolve um erro de conflito (409/stale) e não altera nada. Timestamps negativos ou acima do skew são rejeitados na validação. No cliente, ao aplicar um tombstone, compara `deletedAt` com o `updatedAt` local: se o local for mais recente e não estiver pending, mantém-no e marca para re-push; se estiver pending, ignora o tombstone.

**Alternativas:** usar sempre a hora do servidor para deleções (perde a intenção do dispositivo e o LWW entre dispositivos); apagar sempre (perda de edição). A comparação explícita mantém o modelo LWW existente.

### 6. Reset de configuração com geração

`SyncManager` ganha um `generation` incrementado em `resetState`. `resetState` cancela o job ativo e aguarda `inFlight`; o coordinator recebe a geração e, antes de publicar estado ou gravar watermark, verifica que ainda é a atual. `syncNow` após reset só arranca com a nova geração. Um `Mutex` de configuração serializa reset e início de sync.

**Alternativas:** apenas cancelar sem esperar (o sync pode publicar depois); recriar o `SyncManager` (perde observadores); ignorar o resultado antigo por comparação de settings (frágil). A geração é explícita e testável.

### 7. Registo de fotos staged

Um `StagedPhotoRegistry` (em `AppContainer`, persistido em prefs) regista o path de cada foto staged/capturada. `stagePhoto` e `createPhotoFile` registam; `save`, remoção e `onCleared` desregistam. O sweep recebe o conjunto e nunca apaga paths registados; além disso, aplica uma carência (`STAGED_PHOTO_GRACE_SECONDS`, default 3600) a ficheiros não referenciados.

**Alternativas:** só carência (um formulário aberto mais de 1 h perde a foto); só guard (não cobre o intervalo entre staging e save); gravar a linha na DB no staging (a foto apareceria antes de guardar). O registo persistido é simples e cobre o ciclo de vida.

## Risks / Trade-offs

- [Quotas mal dimensionadas bloqueiam utilizadores legítimos] → defaults generosos, mensagem clara, configuração por env e documentação.
- [Upload referenciado exige P0] → implementar P0 primeiro; enquanto não estiver, o servidor rejeita upload-first (comportamento seguro, mas quebra o fluxo antigo).
- [GC periódico e push concorrem] → lock e carência; o GC já ignora ficheiros referenciados.
- [Tombstone local mais recente pode ser re-pushado repetidamente] → marcar pending com revision e deixar o LWW do servidor resolver; teste de convergência.
- [Registo staged em prefs pode ficar órfão] → entradas com idade máxima e limpeza no arranque; o sweep continua a ser a rede de segurança.
- [Reset com cancelamento pode bloquear a UI] → cancelar é rápido; aguardar com timeout e mostrar estado.

## Migration Plan

1. Servidor: migração de `fotos` (`uploaded_by_key`, `size_bytes`) idempotente; backfill de `size_bytes` a partir dos ficheiros existentes; `server_meta` do P0.
2. Android: atualizar o repositório/sweep e o sync; a seed deixa de ser impressa no provisionamento.
3. Rodar as seeds que possam ter aparecido em logs e reemitir chaves afetadas.
4. Rollback: as colunas adicionais são compatíveis; o servidor antigo ignora-as. O upload referenciado é o único comportamento que exige coordenação com o P0.
