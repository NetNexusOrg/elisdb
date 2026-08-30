## 1. Dockerfile

- [x] 1.1 Remover o passo `apt-get update && apt-get install -y --no-install-recommends su-exec && rm -rf /var/lib/apt/lists/*` do stage runtime
- [x] 1.2 Adicionar um comentário a explicar que o drop de privilégios usa `setpriv` (já incluído no `util-linux` da imagem base)

## 2. Entrypoint

- [x] 2.1 Substituir `su-exec "${PUID}:${PGID}"` por `setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups` na chamada de provisionação (`python -m app.provision`)
- [x] 2.2 Substituir `exec su-exec "${PUID}:${PGID}" "$@"` por `exec setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups "$@"`

## 3. Verificação

- [x] 3.1 Validar sintaxe do entrypoint: `sh -n docker-entrypoint.sh`
- [x] 3.2 Confirmar que `setpriv` está disponível na imagem base (`docker run --rm python:3.14-slim setpriv --version` na VPS) ou `openspec validate --all` com sucesso