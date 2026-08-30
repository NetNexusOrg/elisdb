## Context

Ver `proposal.md`. O change `docker-data-permissions` (arquivado) introduziu
`su-exec` para o drop de privilégios, mas `su-exec` não existe nos repositórios
Debian trixie (`python:3.14-slim`), causando falha no `docker build`. O Debian
fornece `setpriv`, parte do `util-linux` (pacote priority `required`, já
instalado na imagem base), com o mesmo propósito de drop de privilégios.

## Goals / Non-Goals

**Goals:**
- O `docker build` funcionar em Debian trixie.
- Manter o drop de privilégios para `PUID`/`PGID` antes de iniciar o uvicorn.
- Nenhuma mudança de comportamento da app ou dos scripts de permissões de dados.

**Non-Goals:**
- Não mudar o entrypoint para além da troca da ferramenta de drop.
- Não adicionar dependências novas além do que a imagem já traz.

## Decisions

**Usar `setpriv` em vez de `su-exec`:** `setpriv` vem no `util-linux`, que já
está instalado na imagem `python:3.14-slim` (priority required no Debian).
Comando de drop: `setpriv --reuid=<PUID> --regid=<PGID> --clear-groups` seguido
do comando a correr. No entrypoint, os dois pontos que usam `su-exec` passam a
usar `setpriv`:
- provisionação: `setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups python -m app.provision`
- exec final: `exec setpriv --reuid="${PUID}" --regid="${PGID}" --clear-groups "$@"`

**Remover o passo `apt-get install su-exec` do Dockerfile:** já não é preciso
instalar nada para o drop de privilégios, porque `setpriv` já vem na imagem.

## Risks / Trade-offs

- [`setpriv` requer `--clear-groups` para correr sem grupos suplementares] → comportamento idêntico ao su-exec; o processo corre só com os GIDs explícitos.
- [Diferenças mínimas entre `setpriv` e `su-exec` na passagem de uid/gid numéricos] → `setpriv` aceita uids/gids numéricos diretamente, tal como o entrypoint usa.