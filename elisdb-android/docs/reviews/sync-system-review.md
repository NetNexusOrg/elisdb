# Revisão do Sistema de Sincronização (sync-system-review)

- **Data da análise**: 2026-08-09
- **Versões avaliadas**: código do repositório `elisdb` (app) e `elisdb-server`, no estado atual da branch de trabalho.
- **Specs avaliadas**: `openspec/specs/client-sync`, `client-sync-state`, `server-sync`.
- **Tipo**: revisão read-only. Sem alterações de código, comportamento ou specs.
- **Entregável**: este relatório.

---

## Contexto

O ElisDB é uma app **offline-first** com sincronização contra um servidor Python de âmbito LAN. O fluxo de sincronização é orquestrado por `SyncCoordinator`, que executa sequencialmente:

1. `pushEdits()` — envia registos com `pending = true` (e as fotos, em base64).
2. `pushDeletions()` — confirma remoções de registos marcados `deletePending`.
3. `pull()` — aplica o estado autoritativo do servidor e remove localmente registos que o servidor já não tem.

O servidor é a autoridade de conflitos, resolvendo por **last-write-wins** comparando `updated_at`. O transporte assina cada pedido com ed25519 + timestamp (janela de ±300 s). A sincronização é acionada manualmente (nas Definições) e automaticamente por `SyncWorker` (WorkManager periódico, 15 min).

As três áreas alvo das specs:

- `client-sync` — push/pull, LWW, duplicados e remoções.
- `client-sync-state` — marcadores `pending`/`deletePending` e estado de sincronização observável.
- `server-sync` — protocolo de transporte, autenticação e perspetiva do servidor.

---

## Metodologia

- Reli os três specs e os artefactos de sincronização da app (`SyncCoordinator`, `SyncManager`, `SyncWorker`, `SyncApi`, `SyncMapper`, `FilePhotoStore`, `ApiClient`, `Signer`, `AuthHeaders`) e do servidor (`app/main.py`, `app/database.py`, `app/auth.py`).
- Para cada área alvo (correção, perda/ganho de dados, segurança, robustez, escala/performance, observabilidade) verifiquei o comportamento do código contra os cenários das specs.
- Cada achado tem **evidência** (ficheiro + linha), **gravidade**, **esforço × impacto** e **recomendação**. Nenhuma alegação sem citação.
- Confirmado que o relatório não contém chaves/segredos reais e que **nenhum código foi alterado** durante a revisão.

Gravidade:

| Gravidade | Significado |
| --------- | ----------- |
| 🟥 Bloqueante | Perde dados do utilizador ou quebra o fluxo garantido pela spec. |
| 🟧 Grave | Risca dados ou comportamento em cenários plausíveis; merece correção próxima. |
| 🟨 Melhorar a médio prazo | Impacto amortizado por cenário de uso atual, mas cresce com uso/escala. |
| ⚪ Cosmético | Nível de polimento/usabilidade; sem perigo real. |

---

## Fluxo de sincronização verificado

```
SyncCoordinator.sync()
  ├─ pushEdits()          → envia pending (cliente + fotos em base64)
  ├─ pushDeletions()      → confirma deletePending
  └─ pull()               → aplica estado do servidor; apaga registos locais órfãos
```

Atributos-chave observados (para referência nas citações):

- `app/src/main/java/com/example/elisdb/sync/SyncCoordinator.kt`
- `app/src/main/java/com/example/elisdb/sync/SyncMapper.kt`
- `app/src/main/java/com/example/elisdb/sync/SyncManager.kt`
- `app/src/main/java/com/example/elisdb/sync/SyncWorker.kt` (dentro do SyncManager.kt)
- `app/src/main/java/com/example/elisdb/ui/settings/SyncStatusBar.kt`
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt`
- `elisdb-server/app/auth.py`, `app/database.py`, `app/main.py`
- `docs/uso.md`

---

## Achados

### 1. Correção / consistência

**1.1 — Cliente local recém-criado rejeitado como «duplicate» é apagado silenciosamente (bloqueante)**
`🟥 Bloqueante` · Esforço 3 · Impacto 5

- **Descrição**: um cliente criado localmente que colide com o nome (normalizado, case-insensitive) de um registo já existente no servidor é *rejeitado* pelo servidor como `duplicate`. Na app, qualquer resposta do push com `status != null` (incluindo `duplicate`) limpa o `pending` (`SyncCoordinator.kt:60-61`). No `pull()` seguinte, esse id local não consta em `serverIds` e já não é `pending`, portanto é alvo de `finalizeDeletion()` (`SyncCoordinator.kt:107-112`) — o registo e as suas fotos são removidas **localmente também**. O utilizador perde o cliente (e fotos) que acabou de criar, sem qualquer aviso ou fusão com o registo existente.
- **Cenário**: servidor tem cliente `Maria Santos`; dispositivo B (sem esse registo) cria outro `maria santos` com fotos; push → `duplicate`; pull → o cliente criado localmente desaparece; os dados do BE (fotos, notas, contrato) escritos pelo utilizador perdem-se.
- **Evidência**: `SyncCoordinator.kt:55-63` (limpeza de `pending` para qualquer status) e `SyncCoordinator.kt:107-112` (drop local de quem não está em `serverIds`). Servidor devolve `existing_id` mas a app ignora-o (`database.py` `DuplicateError`, e `pushEdits` não usa `existing_id`).
- **Recomendação**: quando o push devolve `duplicate`, em vez de limpar `pending` cegamente, a app deve **adotar o `existing_id`** (associar os seus dados a esse registo). Ou, em alternativa, manter o `pending` e apresentar um conflito/aviso ao utilizador ("este nome já existe — juntar aos dados existentes?"). A spec `server-sync` deve ser também clara sobre esta semântica.

**1.2 — `dataCriacao` na app == `updatedAt` do servidor (a data de criação original perde-se)**
- `🟨 Melhorar a médio prazo` · Esforço 2 · Impacto 3

- **Descrição**: ao aplicar um item do servidor, a app define `dataCriacao = server.updatedAt` (`SyncCoordinator.kt:126`). O servidor só guarda `updated_at`; não existe `created_at`. Resultado: após edições/`pull`, o ecrã de detalhe mostra a **última atualização** em vez da data de criação real. Para um cliente criado e depois editado, a etiqueta "criado em" fica errada.
- **Evidência**: `SyncCoordinator.kt:126-127`; schema servidor (`app/database.py`) só tem `updated_at`.
- **Recomendação**: adicionar `created_at` no servidor e transportá-lo no pull; ou, enquanto não existir, preservar a `dataCriacao` local quando o registo não foi alterado no servidor (heurística), mantendo a semântica de criação.

**1.3 — Concorrência entre `syncNow()` e `SyncWorker` (sem single-flight)**
- `🟧 Grave` · Esforço 2 · Impacto 3

- **Descrição**: `SyncManager.syncNow()` guarda com `_status == IN_PROGRESS` apenas para chamadas manuais (`SyncManager.kt:55`). O `SyncWorker` (periódico) **não partilha esse lock** — instancia o próprio `SyncCoordinator` (`SyncManager.kt:99-107`) e corre assíncrono ao sync manual. Um sync manual disparado no momento do job periódico executa dois `pushEdits()`/`pushDeletions()`/`pull()` concorrentes.
- **Efeito**: idempotente na maioria dos casos, mas dois `pull()` a correr em paralelo fazem `applyServerItem` a reescrever fotos (deletar + re-escrever na mesma transação) → possibilidade de dobra de trabalho e janela de race na leitura/escrita de ficheiros (`SyncCoordinator.kt:132-149`).
- **Evidência**: `SyncManager.kt:41-50` (periodic scheduler), `SyncManager.kt:93-114` (worker).
- **Recomendação**: promover o `SyncCoordinator` para `SyncManager` (singleton) e serializar com um mutex (ex.: `Mutex.withLock`). O worker deve chamar `syncManager.sync()` reutilizando o mesmo lock.

### 2. Perda / ganho de dados

**2.1 — Reescreve todas as fotos em todos os `pull()`, com risco de perda entre delete e re-store**
- `🟧 Grave` · Esforço 3 · Impacto 4

- **Descrição**: em `applyServerItem`, para **cada** cliente vindo do servidor, a app faz `delete files + delete DB rows` das fotos existentes e em seguida re-baixa por base64 e re-grava (`SyncCoordinator.kt:132-149`), mesmo que as fotos não tenham mudado. Isso gera I/O e tráfego `full base64` de todo o catálogo a **cada sync**. Pior: se falhar `storeBase64` no meio (ex.: disco cheio, formato inválido), as fotos antigas já foram apagadas em disco e a transação Room apenas desfaz as linhas da DB — ficando DB órfã de ficheiros e ficheiros órfãs de DB.
- **Evidência**: `SyncCoordinator.kt:132-134` (delete em disco+DB) e `138-149` (re-store). Além disso: `FotoDao.deleteByIds` dentro da mesma transação.
- **Recomendação**: não re-escrever fotos inalteradas — comparar `mime+ordem` e, se a foto local já existe no mesmo hash/base64, saltar; e fazer a escrita da foto **fora** da transação de Room para que o `rollback` não deixe DB sem ficheiro (ou gravar ficheiro primeiro e só apagar o antigo após commit).

**2.2 — Foto com ficheiro ausente é silenciosamente ignorada no push**
- `⚪` Cosmético · Esforço 1 · Impacto 1

- **Descrição**: em `SyncMapper.toRecord`, `photoBase64(foto) ?: continue` (`SyncMapper.kt:19`) — se o ficheiro da foto não existir em disco, a foto é omitida do push, mas a linha da DB persiste (sem marcação de delete). Fica uma foto "fantasma" que nunca é enviada nem removida.
- **Recomendação**: quando o ficheiro de uma foto não existir em disco, a foto fantasma deve ser marcada para limpeza (`deletePending`) ou removida da DB — nunca ficar "pendente para sempre" sem chegar a ser enviada.

### 3. Segurança da assinatura

**3.1 — Janela de timestamp ±300 s pode causar `401` falso de "chave não registada"**
- `🟧 Grave` · Esforço 2 · Impacto 3

- **Descrição**: o transporte assina com `System.currentTimeMillis()/1000` (segundos). O servidor exige `abs(now - timestamp) <= 300 s` (`app/auth.py:49-51`, `is_timestamp_fresh`), devolvendo `401` com detail `"stale or future timestamp"` (`app/main.py:42`). Dispositivo com relógio dessincronizado >5 min (comum em telefones) é rejeitado em todos os pedidos; a app mapeia o `401` para `AuthException` (`ApiClient.kt:114`) e a UI mostra "Servidor rejeitou a sincronização (chave não registada)" (`SyncStatusBar.kt:72`), o que é **erróneo** — a chave é boa, o relógio está mal.
- **Evidência**: `app/main.py:42` (stale → 401) e `app/auth.py:49-51` (janela); `ApiClient.kt:114` (mapeia 401 para `AuthException`); UI em `SyncStatusBar.kt:72` e `SyncManager.kt:63-66`.
- **Recomendação**: distinguir `AuthException` por causa (assinatura inválida vs timestamp stale) e mapear a diferença de relógio para uma mensagem própria (ex.: "O relógio do dispositivo não está com a hora do servidor"). Também sugerir sincronização de hora no setup em `docs/uso.md`.

**3.2 — Chave única por dispositivo; reutilização da seed entre instalações**
- `⚪` Cosmético (documentado) · por decisão

- O protocolo usa uma chave por dispositivo, regista a chave pública; reinstalação perde a seed → novo par de chaves e novo registo (comportamento intencional da docs). O esforço de rótação eventual (pairing) é maior; não recomendado neste momento.

### 4. Robustez

**4.1 — O `SyncWorker` não atualiza o estado observável (`SyncStatus`/`SyncFault`)**
- `🟧 Grave` · Esforço 2 · Impacto 3

- **Descrição**: o `SyncWorker` do WorkManager não escreve em nenhum `StateFlow` do `SyncManager`. As filas de erro/estado visíveis na UI (`SettingsScreen/SyncStatusBar`) só são actualizadas pelo fluxo manual (`SettingsViewModel.manualSync` → `syncManager.syncNow()`). Assim, o sync periódico **nunca reflete** um erro na barra de estado — se o sync automático falhar (rede, chave, config), o utilizador nunca sabe, porque `SyncStatus` permanece `IDLE`/`SUCCESS` de execuções anteriores.
- **Evidência**: `SyncManager.kt:99-114` (worker cria o seu próprio coord / não toca em `_status`/`_fault`); `SyncManager.kt:53-85` (só `syncNow` atualiza).
- **Recomendação**: o worker deve delegar no `SyncManager.sync()` de modo que haja um único ponto que atualize o estado. Isso também resolve o achado 1.3 (mesmo mutex).

**4.2 — Estados Room: fotos escritas fora da transação**
- já coberta por **2.1** (mesmo root cause).

**4.3 — Desinstalação/eliminação da app perde alterações pendentes não sincronizadas**
- `⚪` (by design) — Room é armazenamento interno do app; reinstall perde pendentes não syncados. Documentado como limitação razoável para o âmbito LAN.

### 5. Escala / performance

**5.1 — Pull completo (todos os clientes + todas as fotos em base64) a cada sync**
- `🟨` Melhorar a médio prazo · Esforço 4 · Impacto 2 (agora), 4 a médio prazo

- **Descrição**: `api.pull()` não tem delta/sinc (sem `since`), baixa o catálogo inteiro com `FotosDto` → base64 de cada foto. Aliado ao re-escrito de 2.1, provoca na prática o custo por sync. Para dezenas de clientes com fotos, já se sente na reutilização da LAN. É a principal alavanca se o número de fotos/registos crescer.
- **Recomendação**: paginação e/ou `since` (timestamp) no get `/pull`; ou sync com densidade (ex.: sem base64 para fotos não alteradas).

**5.2 — Push em lote único (todos os pendentes numa única chamada)**
- `🟨` · Esforço 1 · Impacto 2

- **Descrição**: `api.push(records)` envia todos os pendentes numa única chamada. Sendo o lote pequeno (operação manual), é aceitável; mas para volume maior convém porções (`chunk`) com retoma. Recomendação: limitar o lote a ~dezenas de pendentes por efetivo de mensagem.

### 6. Observabilidade

**6.1 — Estado de sincronização só é visível nas Definições; docs dizem que está na lista de clientes**
- `🟨` · Esforço 1 · Impacto 3

- **Descrição**: as docs (`docs/uso.md:61-63` e `:76`) dizem "use o botão de sincronizar na lista de clientes" e que "o estado de sync é visível na barra no topo da lista de clientes". Na implementação atual, o `SyncStatusBar` só aparece dentro da tela de **Definições** (`SettingsScreen.kt:58`); `ClientListScreen`/`ClientListViewModel` não têm qualquer estado de sync (verificação: nenhuma referência a `Sync` na lista). A doc antecipou uma função que não existe → o utilizador não vê estado nem botão na home.
- **Evidência**: `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt:58`; zero menções em `ui/cliente/*`.
- **Recomendação**: ou mover a `SyncStatusBar` (ou um mini-ícone) para a barra superior da lista de clientes (alinhado com a doc), ou corrigir a doc para dizer "estado em Definições". Recomendado: implementar e corrigir a doc, pois a observação na tela principal é crucial para o relatório de estado da spec `client-sync-state`.

**6.2 — `SyncStatus.IDLE` não diferencia "nunca sincronizou" de "tudo sincronizado"**
- `⚪` · Esforço 1 · Impacto 1

- `syncMessage` retorna `null` para `IDLE` (`SyncStatusBar.kt:76`), enquanto `SUCCESS` mostra "Sincronizado". Pode ser confuso no arranque, mas é cosmético.

---

## Recomendações priorizadas (esforço × impacto)

| # | Recomendação | Gravidade | Esforço | Impacto |
|---| --- | --- | --- | --- |
| R1 | **Não apagar cliente local recém-criado quando o push devolve `duplicate`; adotar `existing_id` ou apresentar conflito/merge** (remover o `finalizeDeletion` silencioso) | Bloqueante (perda de dados) | 3 | 5 |
| R2 | **Single-flight da sincronização**: `SyncManager` com o coordinator partilhado; `SyncWorker` e `syncNow()` usam o mesmo `Mutex`; única fonte de estado (`_status`/`_fault` atualizado pelo coordinator — inclusive no caminho periódico) | Grave | 2 | 4 |
| R3 | **Não re-escrever fotos inalteradas no `pull`; gravar fotos fora da transação de Room** (evita desalinhamento entre DB e ficheiro e reduz tráfego) | Grave | 3 | 4 |
| R4 | **`dataCriacao` fiável** (novo `created_at` no servidor ou preserv da data local quando só `updatedAt` mudou) | Médio | 2 | 3 |
| R5 | **Mensagens de erro de auth diferenciadas** (clock skew → "relógio do dispositivo dessincronizado") em vez de "chave não registada" | Médio | 1–2 | 3 |
| R6 | **Resolver `docs/uso.md` vs tela**: colocar a `SyncStatusBar` (ou botão-ícone) no topo da lista de clientes — ou corrigir a doc | Melhorar a médio prazo | 1–2 | 2–3 |
| R7 | Pull incremental (`since`)/paginação (parte da alavanca de escala) | Melhorar a médio prazo | 4 | 3 |
| R8 | Fotos fantasma em ficheiro ausente → limpar/avisar (`SyncMapper.kt:19`) | Cosmético | 1 | 1 |

### Ordem sugerida de execução

1. **R1** (perda de dados garantida) — prioritária.
2. **R2** + **R3** (resolvem os problemas de roboteza e perda de fotos).
   3. **R3** (perda de fotos + tráfego).
2. **R5 + R6** (experiência de utilizador e mensagens erradas).
3. **R4 / R7 / R8** (qualidade a longo prazo).

---

## Veredito

**🟧 Melhorias prioritárias — não “adequado como está”.**

O mecanismo de base (offline-first via `pending`/`deletePending`, assinatura por pedido, LWW no servidor como autoridade) está sólido e cobre os requisitos das três specs. No entanto, o review identificou **um bloqueante de perda de dados** (R1): um cliente localmente criado com nome duplicado é destruído silenciosamente após o push; e um **grave de robustez** (R2/R3): sincronizações periódicas e manuais correm em paralelo sem lock partilhado, photos são re-gravedas em cada pull com janela de perda entre delete e re-store, e o caminho periódico não reflete o estado na UI.

Os três primeiros achados (R1, R2, R3) tornam o sistema **não adequado para produção real** sem correção imediata: o risco de perda de dados do utilizador é real e repetível.

As restantes são melhorias de usabilidade/escala que podem ser abordadas após R1–R3.