# Configuração do servidor — ligar um telemóvel

Este é o guia mais importante: **como dar acesso a um telemóvel**. No final,
vais ter um ficheiro de credencial (`<nome>.config.json`) que a app do
telemóvel importa no ecrã **Configuração**.

## Passo único: emitir a chave de um telemóvel

Um único comando faz tudo: gera a chave do telemóvel, regista-a no servidor (com
o nome do utilizador) e escreve o ficheiro de credencial com o URL do servidor
e a chave privada.

### Modo interativo (recomendado)

Corre `python -m elisdb.keys` **sem argumentos** (dentro do contentor) e escolhe o
número da opção:

```
docker compose exec elisdb-server python -m elisdb.keys
```

O menu pede, em sequência, o **URL do servidor** e o **nome do utilizador**
(se `ELISDB_SERVER_URL` existir, aparece como valor por omissão — basta
carregar em Enter). Depois escreve `<nome>.config.json` e regista a chave.

O mesmo menu permite **listar** e **remover** autenticações:

- `2` lista as chaves confiadas (id, utilizador, chave pública, data).
- `3` remove uma chave **após confirmação** (`s`/`N`) — o telemóvel que a
  usasse deixa de conseguir sincronizar.

### Modo com argumentos (para scripting)

```bash
docker compose exec elisdb-server python -m elisdb.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Exemplo real:

```bash
docker compose exec elisdb-server python -m elisdb.keys issue maria --server-url http://192.168.1.10:8000
```

O que acontece:

```
issued key for 'maria' (id issue-XXXXXXXXXXXX)
configuration written to: maria.config.json
```

A **seed privada não é impressa** por omissão. O ficheiro `maria.config.json`
(permissões `0600`) é a única forma de a obter. Se precisares mesmo de a ver no
terminal, usa `--print-seed` de forma explícita; evita fazê-lo em ambientes cujos
logs sejam recolhidos ou partilhados.

Há **um** sinal de sucesso: a criação do ficheiro **`maria.config.json`**.
O resto da saída é informativo.

O ficheiro tem três campos: `server_url`, `user_name` (o nome do utilizador, o
mesmo usado como comentário da chave na allowlist) e `private_seed` (a chave
privada ed25519). A app mostra o `user_name` como utilizador associado assim que
importa o ficheiro, sem consultar o servidor.

O ficheiro é escrito dentro do contentor; copia-o para a tua máquina:

```bash
docker cp elisdb-server:/app/maria.config.json .
```

Então:

1. **Transfere** o ficheiro `maria.config.json` para o telemóvel (email, pen,
   transferência na rede, etc.).
2. **Na app:** abre o menu (⋮) → **Configuração** → **Importar credencial do utilizador** e
   escolhe o ficheiro. O URL e a chave ficam preenchidos em simultâneo.
3. No mesmo ecrã, carrega em **Sincronizar**. Deve aparecer "Sincronizado".

> O URL do servidor é obrigatório — passa por `--server-url`, pelo modo
> interativo ou pela variável `ELISDB_SERVER_URL`. Sem URL, o comando
> recusa-se a correr. Podes escolher outro nome de ficheiro com
> `--output <caminho>`.

> **A emissão é o único método de dar acesso** — não há comando para registar
> uma chave criada fora do servidor. Para retirar acesso, usa a opção `3`
> (remover) do menu interativo, ou apaga a chave da base de dados.

## Variáveis de ambiente (referência)

| Variável           | O que faz                                        | Omissão        |
| ------------------ | ------------------------------------------------ | -------------- |
| `ELISDB_DB`        | Caminho do ficheiro de dados (base de dados)      | `data/elisdb.db` |
| `ELISDB_PHOTOS`    | Pasta para os ficheiros de fotos                   | `data/photos` ao lado da base de dados |
| `ELISDB_SERVER_URL` | URL pelo qual a app alcança este servidor (usado por `elisdb.keys issue` quando não passas `--server-url`) | — (obrigatória para emitir) |
| `ELISDB_USERS`     | Utilizadores a provisionar no arranque (lista separada por vírgulas). Se vazio ou ausente, todas as chaves auto-provisionadas são revogadas. | — |
| `ELISDB_MAX_PUSH_RECORDS` | Máximo de registos num único `POST /api/push`; acima disto responde `422` | `200` |
| `ELISDB_MAX_PHOTOS_PER_CLIENT` | Máximo de fotos referenciadas por cliente num push; acima disto responde `422` | `500` |
| `ELISDB_MAX_PHOTO_BYTES` | Tamanho máximo de cada upload de foto (bytes) | `20971520` (20 MiB) |
| `ELISDB_MAX_PHOTO_BYTES_PER_KEY` | Quota total de fotos por chave (bytes); acima disto responde `413` | `2147483648` (2 GiB) |
| `ELISDB_MAX_PHOTO_BYTES_TOTAL` | Quota total de fotos no servidor (bytes); acima disto responde `413` | `21474836480` (20 GiB) |
| `ELISDB_RATE_LIMIT_REQUESTS` | Pedidos permitidos por IP em cada janela; acima disto responde `429` | `300` |
| `ELISDB_RATE_LIMIT_WINDOW_SECONDS` | Duração da janela do rate limit (segundos) | `60` |
| `ELISDB_TRUST_PROXY` | Usar o primeiro IP de `X-Forwarded-For` como origem do pedido. **Só ligar atrás de um proxy de confiança** | `false` |
| `ELISDB_TOMBSTONE_RETENTION_DAYS` | Apaga tombstones mais antigos que N dias (`0` desativa) | `0` (desativado) |
| `ELISDB_AUDIT_RETENTION_DAYS` | Apaga entradas de auditoria mais antigas que N dias (`0` desativa) | `0` (desativado) |
| `ELISDB_PHOTO_GC_INTERVAL_SECONDS` | Intervalo mínimo entre limpezas de fotos órfãs depois de um push | `3600` |
| `ELISDB_BIND_ADDRESS` | (só `compose.yaml`) Interface do host onde a porta 8000 é publicada. Por omissão só escuta em loopback; para expor na LAN define o IP explicitamente e usa TLS no proxy | `127.0.0.1` |

Dentro do `compose.yaml`, as variáveis vão na secção `environment`. Exemplo:

```yaml
environment:
  ELISDB_DB: /app/data/elisdb.db
  ELISDB_PHOTOS: /app/data/photos
  # Opcional: sem ELISDB_USERS não é criada nenhuma conta no arranque.
  # ELISDB_USERS: "maria,joao"
  # ELISDB_SERVER_URL: "https://elisdb.exemplo.pt"
```

> Sem `ELISDB_USERS` e `ELISDB_SERVER_URL` o servidor arranca sem contas e sem
> chaves; emite a primeira com `elisdb.keys issue`. O `compose.yaml` não traz
> utilizadores nem URL de exemplo, e a porta fica publicada apenas em
> `127.0.0.1` por omissão.

> Usa sempre o mesmo valor de `ELISDB_DB` também nos comandos `elisdb.keys` — as
> chaves registadas vivem nesse mesmo ficheiro de dados.

## Limites de push e fotos

Um push demasiado grande é recusado com `422` **antes** de qualquer escrita, para
que o cliente não fique com metade do lote aplicado:

- `ELISDB_MAX_PUSH_RECORDS` (omissão `200`) limita o número de registos num
  `POST /api/push`;
- `ELISDB_MAX_PHOTOS_PER_CLIENT` (omissão `500`) limita as fotos que um registo
  pode referenciar.

Cada upload de foto continua limitado por `ELISDB_MAX_PHOTO_BYTES` (omissão
20 MiB), com `413` acima do limite.

## Retenção e manutenção

Por omissão o servidor **nunca apaga histórico**. A retenção é opt-in e faz-se
com duas variáveis independentes:

- `ELISDB_TOMBSTONE_RETENTION_DAYS=N` remove tombstones com mais de N dias. A
  idade é medida pelo `delivery_ts` atribuído pelo servidor, não pelo
  `deleted_at` do telemóvel, para que um relógio atrasado não faça expirar o
  tombstone cedo demais.
- `ELISDB_AUDIT_RETENTION_DAYS=N` remove entradas de `client_audit` com mais de
  N dias (medidos pelo `ts` do registo).

A limpeza corre no arranque e, no máximo, uma vez por hora após um push
bem-sucedido; é idempotente e segura. **Atenção:** um telemóvel que fique
offline durante mais tempo do que a retenção de tombstones pode ressuscitar um
cliente apagado ao sincronizar, porque o tombstone que bloqueava esse push já
não existe. Mantém a janela superior ao período máximo em que um dispositivo
pode estar offline, ou deixa a retenção desativada.

As fotos órfãs (uploads que nenhum push referenciou) são limpas no arranque
e, depois, periodicamente em segundo plano
(`ELISDB_PHOTO_GC_INTERVAL_SECONDS`, omissão `3600`), mesmo que não haja
pushes. O varrimento só remove ficheiros sem linha na tabela `fotos` e com mais
de 5 minutos (margem para uploads cujo push ainda está a decorrer).

Um upload (`PUT /api/photos/{id}`) só é aceite quando o id já foi referenciado
por um push que criou a respetiva linha em `fotos`; caso contrário responde
`404`. Isto evita que uma chave autenticada encha o disco com ficheiros que
nunca serão usados. Além do limite por ficheiro, o servidor aplica as quotas
`ELISDB_MAX_PHOTO_BYTES_PER_KEY` e `ELISDB_MAX_PHOTO_BYTES_TOTAL` **antes** de
escrever, respondendo `413` sem deixar ficheiros para trás.

## Rate limiting e proxy

O limite de pedidos é aplicado **por IP**. O header `X-Elisdb-Key` não é usado
para o orçamento porque é opcional e não está verificado nesse ponto — usá-lo
permitiria rodar chaves para contornar o limite.

Atrás de um proxy inverso, o IP que o servidor vê é o do proxy. Para limitar
pelo IP real do cliente, define `ELISDB_TRUST_PROXY=true`: o servidor passa a
usar o primeiro IP de `X-Forwarded-For`. **Só atives esta opção se o servidor
só for alcançável através do proxy** (por exemplo, com
`ELISDB_BIND_ADDRESS=127.0.0.1`); caso contrário, qualquer cliente pode forjar
o header e obter um orçamento novo. Ver
[Nginx Proxy Manager](nginx-proxy-manager.md).

## Contrato de sincronização (referência)

Cada registo tem dois relógios, com papéis diferentes:

- **`updated_at`** (cliente) e **`deleted_at`** (eliminação) vêm do dispositivo e
  **só decidem conflitos** (last-write-wins). O servidor guarda-os exatamente como
  os recebe, mesmo que venham de um telemóvel com o relógio atrasado.
- **`delivery_ts`** é atribuído pelo servidor (`max(timestamp do dispositivo,
  watermark + 1)`, sob o *write lock*) e é **o cursor do pull**: `GET /api/pull`
  filtra, ordena e pagina por `delivery_ts`, e cada registo e tombstone devolve o
  seu. `next_since` vem da última linha lida.

Consequências que valem a pena saber ao diagnosticar:

- Uma eliminação feita com o relógio atrasado **chega na mesma** aos outros
  dispositivos: o tombstone guarda `deleted_at` (para o LWW) mas é entregue pelo
  `delivery_ts`, que é sempre maior do que tudo o que já saiu.
- A eliminação também participa no last-write-wins: o servidor compara
  `deleted_at` com o `updated_at` guardado e responde `409` se a eliminação não
  for mais recente, sem apagar o registo nem criar tombstone. Timestamps
  negativos ou demasiado no futuro são recusados com `400`. O cliente compara o
  tombstone com o `updated_at` local: se a cópia local for mais recente, preserva-a
  e volta a enviá-la em vez de a apagar.
- Um `push` com `updated_at` mais antigo do que o watermark **não é rejeitado**:
  fica com o seu `updated_at` e recebe um `delivery_ts` novo.
- A resposta do push traz `missing_photos`: fotos referenciadas cujo ficheiro não
  existe no servidor. Não são gravadas e o cliente volta a enviá-las; nunca são
  dadas como enviadas.
- O estado **`ativo`** (booleano, por omissão `true`) faz parte do registo e
  participa no last-write-wins como os restantes campos. Um payload antigo sem o
  campo é guardado como ativo; o histórico regista a passagem como
  `details.fields` com `field: "ativo"` e os valores `Ativo`/`Inativo`. Atualiza
  primeiro o servidor: uma app antiga ignora o campo e volta a enviá-lo como
  ativo.
- O `ts` de cada entrada de `client_audit` é o `updated_at` do dispositivo
  (o momento da edição), **não** a hora a que o servidor recebeu o push. Assim o
  histórico mantém a ordem real das edições mesmo quando um telemóvel sincroniza
  mais tarde; um relógio atrasado no dispositivo aparece refletido no histórico
  e o servidor não o corrige.

## Custo do pull e hashes de fotos

Um `GET /api/pull` pagina por `(delivery_ts, id)` e lê os clientes e as fotos da
página em **lotes**, numa única ligação SQLite, com sub-lotes de ids (500). O
custo não cresce com uma ligação por registo; uma página de 1000 registos usa um
número limitado de consultas.

Quando uma foto guardada ainda não tem `hash`, o servidor calcula-o na primeira
leitura, **grava-o na mesma operação** e reutiliza-o nas leituras seguintes. Isto
evita recalcular SHA-256 de todas as fotos em cada pull; o hash continua a ser a
identidade de conteúdo usada pelo cliente para decidir se precisa de transferir
a foto.

## Primeira execução

Numa base de dados nova, **nenhuma chave é confiada**: até emitires a primeira
chave, os telemóveis recebem `401` ao tentar sincronizar.

## Observabilidade (referência)

Endpoints de diagnóstico (todos exigem chave, exceto `/health`):

- `GET /health` — estado do servidor, versão de release, `api_version` e
  `instance_id`. A `api_version` serve para diagnóstico: sobe-a sempre que uma
  alteração de contrato importe para clientes antigos. Não é mostrada ao
  utilizador da app. O `instance_id` identifica de forma estável esta base de
  dados: mantém-se após reinícios e muda se a base de dados/volume for recriado,
  para a app distinguir o mesmo servidor de um servidor novo ou restaurado.
- `GET /api/latest` — além de `latest_updated_at`, `client_count` e
  `instance_id`, devolve `max_push_records`: o número máximo de registos aceite
  num único `POST /api/push`. A app usa este valor para dividir lotes grandes;
  clientes antigos ignoram os campos adicionais. O `latest_updated_at` é o maior
  `delivery_ts` entre **clientes e tombstones**: inclui alterações de clientes e
  eliminações, não apenas o `updated_at` dos registos ainda existentes.
- `GET /api/clients/{id}/history` — histórico de criação, edição e eliminação.
  Sem parâmetros devolve a lista completa (compatibilidade). Com `limit` (1–200)
  e/ou `after_id` devolve `{entries, next_after_id}` para paginação; usa o
  `next_after_id` na chamada seguinte até vir `null`.
- `POST /api/push` — aceita o cabeçalho opcional `Idempotency-Key`. O mesmo par
  chave+payload devolve a resposta anterior sem voltar a escrever; a mesma chave
  com payload diferente responde `409`. Sem o cabeçalho mantém-se o
  processamento registo a registo.
- `GET /api/keys` — **só para operadores**: responde sempre `403` a um cliente
  autenticado. A listagem real (id, chave pública, comentário) é feita pela CLI
  (`python -m elisdb.keys`, opção `2`).
- `GET /api/keys/me` — mostra a chave que está a ser usada neste momento
  (devolve `key_id` e `username` do utilizador autenticado).

Numa base de dados nova, a lista de chaves está vazia.

## Passo seguinte

[Utilização](uso.md) — como funciona a sincronização e como resolver problemas.
