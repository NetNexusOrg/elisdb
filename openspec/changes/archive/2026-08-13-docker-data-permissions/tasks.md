## 1. Entrypoint de permissões

- [x] 1.1 Criar `elisdb-server/docker-entrypoint.sh` (POSIX sh, `set -e`): para cada diretório (dirname de `ELISDB_DB`, valor de `ELISDB_PHOTOS`), `mkdir -p` se não existir e `chown -R "${PUID:-1000}:${PGID:-1000}"` ao diretório
- [x] 1.2 O entrypoint termina com `exec su-exec "${PUID:-1000}:${PGID:-1000}" "$@"` (passa o `CMD`/argumentos ao uvicorn)
- [x] 1.3 Tornar o ficheiro executável (`chmod +x`) e garantir que não é ignorado pelo `.dockerignore`

## 2. Dockerfile

- [x] 2.1 No stage runtime, instalar `su-exec` (apt-get update && apt-get install -y --no-install-recommends su-exec && rm -rf /var/lib/apt/lists/*)
- [x] 2.2 Copiar `docker-entrypoint.sh` para a imagem (ex.: `/usr/local/bin/docker-entrypoint.sh`) e marcar `ENTRYPOINT ["docker-entrypoint.sh"]`
- [x] 2.3 Remover `USER appuser` do Dockerfile (o entrypoint gere o drop de privilégios); manter o `CMD` do uvicorn como argumentos do entrypoint

## 3. Compose

- [x] 3.1 Em `compose.yaml`, adicionar `PUID` e `PGID` ao environment (defaults 1000/1000) com comentário explicando o que fazem
- [x] 3.2 Manter o volume `elisdb-data` e o healthcheck inalterados

## 4. Documentação

- [x] 4.1 Atualizar `elisdb-server/docs/execucao.md` (secção Docker): notar que o container corrige as permissões dos dados no arranque (dono `PUID`/`PGID`) e que volumes antigos ficam corrigidos no próximo arranque

## 5. Verificação

- [x] 5.1 Validar sintaxe: `sh -n docker-entrypoint.sh` e revisão do Dockerfile
- [x] 5.2 Rodar `openspec validate --all` com sucesso