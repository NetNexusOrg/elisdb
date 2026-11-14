# Deploy do servidor

O código publicado tem de ser um commit que o **Server CI** já testou. Como não
há *branch protection* a exigir checks na `main`, essa garantia vive no script.

```bash
./elisdb-server/scripts/deploy-server.sh --check   # só verifica
./elisdb-server/scripts/deploy-server.sh           # publica (pede confirmação)
./elisdb-server/scripts/deploy-server.sh -y        # sem confirmação
```

## O que o script faz

1. **Guardas** — recusa publicar se:
   - houver alterações por commitar em `elisdb-server/`;
   - a `main` local não coincidir com `origin/main`;
   - o último commit que tocou em `elisdb-server/` não tiver Server CI verde.
2. **Backup pré-deploy** — `elisdb.backup create` dentro do contentor (se estiver
   a correr).
3. **Cópia** — `rsync` para a VPS, **sem `--delete`** e sem tocar nos dados:
   exclui `elisdb-data/`, `configs/`, `compose.yaml`, `*.db`, `*.config.json`,
   `.env*`, `photos/`, `data/`, logs e caches.
4. **Rebuild** — `docker compose up -d --build`.
5. **Health check** opcional com `ELISDB_HEALTH_URL`.

## Variáveis

| Variável | Omissão | Para quê |
|---|---|---|
| `ELISDB_DEPLOY_HOST` | `vps01` | Host SSH |
| `ELISDB_DEPLOY_TARGET` | `docker/elisdb-server` | Pasta de destino (relativa à home) |
| `ELISDB_HEALTH_URL` | (vazio) | `/health` a confirmar no fim |

## O que o script não faz

- Não altera o `.env` nem o `compose.yaml` de produção (são do host).
- Não apaga nada no servidor.
- Não mexe em chaves nem em utilizadores.

## Depois do deploy

Confirma três coisas:

```bash
cd /home/ubuntu/docker/elisdb-server
docker compose ps
curl http://127.0.0.1:8000/health
python3 start.py --status
```

O `instance_id` tem de ser o mesmo de antes (se mudou, a base de dados foi
recriada). As contagens, as chaves ativas e o último backup aparecem no
`--status`.

## Rollback

1. `git checkout <commit-anterior>` na árvore do servidor.
2. `docker compose up -d --build`.
3. Se houver suspeita de corrupção de dados, restaurar o backup pré-deploy
   ([backup](backup.md)).

Nunca uses `docker compose down -v`: apaga o volume (e, com bind mounts, não é
necessário para nada).

## Notas de produção

- `compose.yaml` e `.env` vivem **só** na VPS; o repositório tem versões de
  referência.
- Os dados são bind mounts (`./elisdb-data`, `./configs`) com dono `PUID:PGID`.
- Um deploy não reinicia a app dos telemóveis: o APK novo é descoberto no
  pedido seguinte ([publicar uma versão da app](publicar-app.md)).
