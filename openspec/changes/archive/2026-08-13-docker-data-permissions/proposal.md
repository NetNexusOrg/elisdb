## Why

O container falha no arranque em produção com `PermissionError: /app/data/photos`. O processo corre como `appuser` (uid 1000) mas o volume nomeado `elisdb-data` é inicializado pelo Docker como `root`-owned (`/app/data` não existe na imagem e é criado na 1ª montagem com dono root). Isto quebra o arranque em qualquer VPS e também afetará volumes já existentes até serem corrigidos.

## What Changes

- Adicionar um **entrypoint** (`docker-entrypoint.sh`) com a mesma lógica de permissões das imagens linuxserver: no arranque, criar os diretórios de dados (`ELISDB_DB` e `ELISDB_PHOTOS`) se não existirem e dar `chown` do dono para `appuser` (uid 1000) — ou para o `PUID`/`PGID` fornecidos como env — antes de iniciar o processo.
- No `Dockerfile`: marcar o entrypoint como `ENTRYPOINT`, manter o `CMD` (uvicorn) como argumento, e não assumir que o volume já vem com o dono correto.
- Atualizar `compose.yaml` para expor `PUID`/`PGID` (defaults 1000/1000) de forma documentada e manter o volume.
- **Compatibilidade**: o volume `elisdb-data` **já existente** (root-owned) fica corrigido automaticamente no próximo arranque, sem `docker compose down -v` — o chown do entrypoint resolve o dono. Novos volumes nascem corretos.
- Documentar a correção em `docs/execucao.md` (secção Docker).

## Capabilities

### New Capabilities
<!-- nenhuma -->

### Modified Capabilities
<!-- nenhuma - skip_specs: true (mudança de infraestrutura/deploy; o comportamento do servidor não muda) -->

## Impact

- `elisdb-server/docker-entrypoint.sh` — novo ficheiro (script de arranque).
- `elisdb-server/Dockerfile` — definir `ENTRYPOINT` e instalar o que o script precisar (usa apenas stdlib POSIX: `chown`, `mkdir`, `su-exec`/`gosu` — a avaliar em design).
- `elisdb-server/compose.yaml` — expor `PUID`/`PGID`.
- `elisdb-server/docs/execucao.md` — secção "Execução em Docker": notas de permissões e PUID/PGID.
- Sem alterações ao código da app, banco de dados ou ao comportamento do servidor.