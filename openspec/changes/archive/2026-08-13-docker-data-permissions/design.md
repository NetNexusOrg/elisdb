## Context

Ver `proposal.md`. O problema: `Dockerfile` corre o processo como `appuser` (uid 1000), mas o volume nomeado `elisdb-data` é montado em `/app/data` e, como esse diretório não existe na imagem, o Docker cria-o na primeira montagem com dono `root`. O `appuser` não consegue criar `/app/data/photos` → `PermissionError` no arranque. Isto é o clássico problema de permissões de volumes nomeados; as imagens linuxserver resolvem-no com um init que faz `chown` do dono dos dados para `PUID`/`PGID` antes de arrancar o serviço.

## Goals / Non-Goals

**Goals:**
- Arranque do container funcionar em volumes nomeados novos e já existentes (root-owned), sem `docker compose down -v`.
- Replicar a lógica linuxserver: `chown` dos diretórios de dados para `PUID`/`PGID` (defaults 1000/1000) no arranque.
- Manter o serviço a correr como utilizador não-root após o ajuste de permissões.
- Suportar bind mounts do host (que podem ter qualquer dono) ao expor `PUID`/`PGID`.

**Non-Goals:**
- Não mudar o comportamento da app, do servidor HTTP ou do banco.
- Não migrar para o stack s6/init completo das imagens linuxserver (só a lógica de permissões).
- Sem novas dependências de runtime além do utilitário de drop de privilégios.

## Decisions

**Entrypoint POSIX sh + `su-exec`:** o entrypoint (`docker-entrypoint.sh`) corre como `root` (default da imagem), cria os diretórios de dados se não existirem, faz `chown -R PUID:PGID` em ambos, e depois `exec su-exec PUID:PGID "$@"` para iniciar o uvicorn como utilizador não-root. `su-exec` é um utilitário minúsculo disponível nos repositórios Debian (imagem `python:3.14-slim` é Debian-based), instalado com `apt-get install -y su-exec`. Alternativas consideradas:
- **`setpriv` (util-linux)** — já vem no Debian, mas a sintaxe para grupos complementares é mais frágil; `su-exec` é o padrão usado pelas imagens linuxserver.
- **`gosu`** — não está nos repositórios Debian padrão; exigiria download manual. Descartado.
- **Manter `USER appuser`** — impossível: o entrypoint precisaria de `root` para `chown`, e `USER` não-`root` impediria o ajuste.

**`PUID`/`PGID` como env, default 1000/1000:** `compose.yaml` expõe `PUID`/`PGID` documentados. O `chown` usa os valores numéricos, por isso funciona quer o dono coincida com `appuser` (uid 1000) quer com um uid arbitrário de bind mount do host. Removemos `USER appuser` do `Dockerfile` (o entrypoint gere o drop); o `su-exec` aceita uid/gid numéricos sem exigir entrada em `/etc/passwd`.

**Criação dos diretórios no entrypoint:** usar `dirname` de `ELISDB_DB` e o valor de `ELISDB_PHOTOS` — assim respeita configurações personalizadas e garante que os dois caminhos existem antes do `chown`.

## Risks / Trade-offs

- [Serviço corre como root durante o init (antes do su-exec)] → é uma janela mínima e só o entrypoint; o processo final (`uvicorn`) corre como não-root.
- [`su-exec` adiciona um pacote ao runtime] → ~30 KB, apt-get no mesmo RUN que já existe; sem impacto funcional.
- [Volumes já existentes no host: o `chown` do primeiro arranque fixa o dono em `PUID:PGID`] → se o operador mudar `PUID`/`PGID` depois, o `chown` no próximo arranque reajusta — comportamento esperado (igual linuxserver).
- [Healthcheck do compose usa `python`] → o `CMD` continua a ser `uvicorn`; o entrypoint só acrescenta o passo de permissões, sem afetar o healthcheck.