# Configuração do servidor

O servidor lê a configuração do ambiente. No Docker, o `compose.yaml` traduz o
`.env` da pasta `elisdb-server/` para dentro do contentor.

```bash
cp .env.example .env
chmod 600 .env
```

O `.env` está no `.gitignore` — nunca o comites. As omissões funcionam para uso
local; só `ELISDB_SERVER_URL` é obrigatório, e apenas para emitir chaves.

## Caminhos

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_DB` | Ficheiro SQLite | `elisdb.db` |
| `ELISDB_PHOTOS` | Pasta das fotos | `photos/` ao lado da base de dados |
| `ELISDB_CONFIGS_DIR` | Pasta dos `*.config.json` | `configs/` ao lado da base de dados |
| `ELISDB_APK_DIR` | APKs para atualização da app | `apk/latest/` ao lado da base de dados |
| `ELISDB_BACKUP_DIR` | Backups automáticos | `<pasta da BD>/backups` |
| `ELISDB_DATA_DIR` | *(só compose)* Pasta do host montada em `/app/data` | `./elisdb-data` |
| `PUID` / `PGID` | *(só compose)* uid/gid do contentor | `1000` / `1000` |

## Ligação

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_SERVER_URL` | URL que os telemóveis usam; usado por `elisdb.keys issue` sem `--server-url` | — (obrigatório para emitir) |
| `ELISDB_BIND_ADDRESS` | *(só compose)* interface onde a porta 8000 é publicada | `127.0.0.1` |
| `ELISDB_PORT` | Porta usada pela TUI e pelo health check | `8000` |
| `ELISDB_TRUST_PROXY` | Usa o primeiro IP de `X-Forwarded-For`. **Só atrás de um proxy de confiança** | `false` |

## Limites de pedido

Um push acima do limite é recusado com `422` **antes** de escrever — o cliente
nunca fica com meio lote aplicado.

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_MAX_PUSH_RECORDS` | Registos por `POST /api/push` | `200` |
| `ELISDB_MAX_PUSH_BYTES` | Tamanho máximo do corpo de um push | `67108864` (64 MiB) |
| `ELISDB_MAX_PHOTOS_PER_CLIENT` | Fotos referenciadas por cliente num push | `500` |
| `ELISDB_MAX_PHOTO_BYTES` | Tamanho de cada foto | `20971520` (20 MiB) |
| `ELISDB_MAX_PHOTO_BYTES_PER_KEY` | Quota total de fotos por chave | `2147483648` (2 GiB) |
| `ELISDB_MAX_PHOTO_BYTES_TOTAL` | Quota total de fotos no servidor | `21474836480` (20 GiB) |

## Rate limit

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_RATE_LIMIT_REQUESTS` | Pedidos por IP em cada janela (`429` acima) | `300` |
| `ELISDB_RATE_LIMIT_WINDOW_SECONDS` | Duração da janela | `60` |

O limite é **por IP**, não por chave: o `X-Elisdb-Key` é opcional e não está
verificado nesse ponto, por isso usá-lo permitiria rodar chaves para contornar o
limite. Atrás de um proxy, define `ELISDB_TRUST_PROXY=true` **e** garante que o
servidor só é alcançável pelo proxy (ver [nginx](nginx.md)).

## Retenção e manutenção

Tudo é opt-in; por omissão o servidor não apaga histórico.

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_TOMBSTONE_RETENTION_DAYS` | Apaga tombstones com mais de N dias (`0` desativa) | `0` |
| `ELISDB_AUDIT_RETENTION_DAYS` | Apaga auditoria com mais de N dias (`0` desativa) | `0` |
| `ELISDB_IDEMPOTENCY_RETENTION_DAYS` | Apaga chaves de idempotência antigas | `30` |
| `ELISDB_PHOTO_GC_INTERVAL_SECONDS` | Intervalo entre limpezas de fotos órfãs | `3600` |

> **Cuidado com tombstones:** um telemóvel offline por mais tempo do que a
> retenção pode ressuscitar um cliente apagado ao sincronizar, porque o tombstone
> que bloqueava o push já não existe. Mantém a janela acima do período máximo
> offline, ou deixa em `0`.

## Backups

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_BACKUP_INTERVAL_SECONDS` | Intervalo entre backups automáticos (`0` desativa) | `86400` (diário) |
| `ELISDB_BACKUP_RETENTION` | Quantos `auto-*` manter | `3` |
| `ELISDB_BACKUP_DIR` | Pasta dos backups | `<pasta da BD>/backups` |

Os backups manuais (`pre-deploy-*`, `pre-migrate-*`) nunca são podados. Detalhes
em [backups](backup.md).

## Logs e versão

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_LOG_LEVEL` | `DEBUG`, `INFO`, `WARNING`, `ERROR` | `INFO` |
| `ELISDB_VERSION` | Versão reportada em `/health` | lida do ficheiro `VERSION` |

## Só para a TUI

| Variável | O que faz | Omissão |
|---|---|---|
| `ELISDB_PYTHON` | Interpretador para o modo local e para `elisdb.keys` | `.venv/bin/python`, senão `python3` |

## Exemplo de `.env`

```dotenv
ELISDB_DATA_DIR=./elisdb-data
ELISDB_SERVER_URL=https://elisdb.exemplo.pt
PUID=1000
PGID=1000
ELISDB_BIND_ADDRESS=127.0.0.1
ELISDB_TRUST_PROXY=false
ELISDB_BACKUP_RETENTION=3
```

A lista completa, com comentários, está em
[`.env.example`](../../elisdb-server/.env.example). Qualquer alteração ao `.env`
exige `docker compose up -d` para ser aplicada — a TUI faz isso por ti.
