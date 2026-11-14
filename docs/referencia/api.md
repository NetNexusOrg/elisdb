# API HTTP

Referência do contrato servidor ↔ app. Para o comportamento do lado do
telemóvel, ver [sincronização](sincronizacao.md).

## Autenticação

Cada pedido é assinado com **ed25519**. A app guarda a seed privada; o servidor
só conhece a chave pública.

| Cabeçalho | Conteúdo |
|---|---|
| `X-Elisdb-Timestamp` | Epoch em segundos |
| `X-Elisdb-Signature` | Assinatura base64 |
| `X-Elisdb-Key` | Chave pública base64 (raw) |

A mensagem assinada é:

```
MÉTODO\nCAMINHO?QUERY\nSHA256(corpo)\nTIMESTAMP
```

- Janela de ±300 s (relógio dessincronizado → `401`).
- Cache anti-replay por processo: a mesma assinatura não é aceite duas vezes.
- Sem os cabeçalhos, a resposta é `401 missing signature headers`.

## Endpoints

| Método | Caminho | Auth | Para que serve |
|---|---|---|---|
| GET | `/health` | — | Versão, `api_version`, `instance_id` |
| GET | `/ready` | — | Base de dados e pasta de fotos utilizáveis |
| GET | `/api/time` | — | Hora do servidor (desvio de relógio) |
| GET | `/api/latest` | ✓ | `latest_updated_at`, `client_count`, `api_version`, `instance_id`, `max_push_records` |
| POST | `/api/push` | ✓ | Enviar clientes e fotos |
| GET | `/api/pull` | ✓ | Receber alterações desde `since` |
| GET | `/api/clients` | ✓ | Listar clientes (sem fotos) |
| GET | `/api/clients/{id}/history` | ✓ | Histórico de auditoria |
| DELETE | `/api/clients/{id}` | ✓ | Apagar cliente e fotos |
| GET | `/api/photos/{id}` | ✓ | Descarregar foto |
| PUT | `/api/photos/{id}` | ✓ | Enviar foto (só se já referenciada) |
| GET | `/api/keys` | ✓ | **Sempre `403`** — listagem é da CLI |
| GET | `/api/keys/me` | ✓ | `key_id` e `username` da chave atual |
| GET | `/api/app/latest` | ✓ | Metadados do APK publicado |
| GET | `/api/app/download` | ✓ | Download do APK |

## Códigos de erro

| Código | Significado |
|---|---|
| `400` | Pedido mal formado (ex.: cliente sem nome, timestamps inválidos) |
| `401` | Sem chave, chave revogada, assinatura inválida ou relógio fora da janela |
| `403` | Operação só de operador (`/api/keys`) |
| `404` | Registo ou foto inexistente; `PUT` de foto ainda não referenciada |
| `409` | Conflito LWW (`deleted_at` não é mais recente) ou `Idempotency-Key` reutilizada com outro payload |
| `413` | Quota ou tamanho de foto excedido |
| `422` | Lote acima de `ELISDB_MAX_PUSH_RECORDS`/`ELISDB_MAX_PHOTOS_PER_CLIENT` |
| `429` | Rate limit por IP |
| `503` | Base de dados ocupada (WAL + `busy_timeout`); repetir mais tarde |

## Push

- Aceita `Idempotency-Key` (opcional): o mesmo par chave+payload devolve a
  resposta anterior sem escrever; a mesma chave com payload diferente dá `409`.
- Resposta por registo: `accepted`, `duplicate` (com o registo canónico),
  `stale` (LWW) ou erro.
- `missing_photos` lista fotos referenciadas cujo ficheiro ainda não existe — o
  cliente reenvia-as; nunca são dadas como enviadas.

## Pull

- `GET /api/pull?since=<delivery_ts>&limit=<n>` devolve `{records,
  tombstones, next_since}`.
- Ordena e pagina por `delivery_ts` — o cursor é do servidor, não do relógio do
  telemóvel ([detalhe](sincronizacao.md)).
- Clientes e fotos são lidos em lote por página, não registo a registo.

## Histórico

`GET /api/clients/{id}/history` devolve a lista completa (até 200) por
compatibilidade. Com `limit` (1–200) e/ou `after_id` devolve
`{entries, next_after_id}`; continua enquanto `next_after_id` não for `null`.

## Observabilidade

- `/health` — `api_version` sobe quando o contrato muda de forma relevante para
  clientes antigos; `instance_id` identifica a base de dados e mantém-se entre
  reinícios (muda se a BD for recriada ou restaurada).
- `/ready` — `200 {"status":"ready"}` ou `503`.
- `latest_updated_at` é o maior `delivery_ts` entre clientes **e** tombstones,
  por isso inclui eliminações.
- `max_push_records` permite ao cliente dividir lotes antes de apanhar `422`.
- `GET /api/keys` responde sempre `403` para que uma app comprometida não
  enumere as chaves dos outros dispositivos.
