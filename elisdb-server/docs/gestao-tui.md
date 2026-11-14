# Gestão do servidor (TUI)

`start.py` é uma interface de terminal, sem dependências externas (só a
biblioteca padrão), para operar o servidor sem decorar comandos de Docker ou
editar ficheiros à mão.

```bash
cd elisdb-server
python start.py
```

## Menu

```
1) Status
2) Iniciar
3) Parar
4) Utilizadores
5) Configuração
0) Sair
```

### Status

Mostra o modo ativo (`docker`, `local` ou `parado`), o diretório de dados, o
dono e se é gravável, os tamanhos de `elisdb.db`, `photos/`, `backups/` e `apk/`,
as contagens de clientes, fotos e tombstones, as chaves ativas, quantos backups
existem (e o mais recente) e o `/health`. Não escreve nada: lê a base de dados em
modo `read-only`.

Os backups automáticos são criados pelo próprio servidor
(`ELISDB_BACKUP_INTERVAL_SECONDS`, omissão diária) e a retenção
(`ELISDB_BACKUP_RETENTION`, omissão `3`) só apaga pastas `auto-*`. Ver
[backup.md](backup.md).

### Iniciar

- **Local** — arranca `uvicorn elisdb.main:app` em `127.0.0.1:8000` como o teu
  utilizador, com o mesmo `ELISDB_DATA_DIR` e a mesma base de dados do Docker.
  O log vai para `.run/server.log` e o PID para `.run/server.pid`.
- **Docker** — `docker compose up -d --build`.

O modo local precisa das dependências do Python. O interpreter usado é, por
ordem: `ELISDB_PYTHON`, `.venv/bin/python` (ou `.venv/Scripts/python.exe`) e, por
último, o Python que executou o `start.py`. Recomendado, com
[uv](https://docs.astral.sh/uv/):

```bash
uv venv .venv
uv pip install -r requirements.txt
```

Alternativa sem uv: `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt`.
Em produção usa o modo Docker — não é preciso instalar nada no host.

Nunca corre os dois ao mesmo tempo: se um modo estiver ativo, o outro é recusado.
Antes de arrancar, verifica que `elisdb-data/` e `configs/` pertencem ao teu
uid/gid (o contentor corre com `PUID:PGID`) e que os diretórios são graváveis.

### Utilizadores

Lista agrupada por utilizador (chaves ativas/total e se existe
`configs/<nome>.config.json`), adiciona (emite chave e escreve o ficheiro
`0600`), revoga por utilizador ou por ID, e mostra o detalhe de uma chave.

- Revogar é *soft*: a linha fica na base de dados com `revoked_at`, preservando a
  auditoria. Apagar a linha exige confirmação escrita.
- Nunca sobrescreve um `.config.json` existente: sugere outro nome.
- Recusa revogar a última chave ativa (o servidor ficaria sem acesso).
- A seed privada nunca é impressa.

### Configuração

Edita grupos de variáveis do `.env` (utilizadores, URL, rede, limites, retenção,
caminhos, log) ou o ficheiro inteiro no `$EDITOR`. Cada alteração é validada
antes de ser escrita (inteiros, booleanos, URL, nível de log), guarda um backup
`.env.bak-<timestamp>` e mostra um diff. No fim pergunta **“aplicar agora?”** e,
se aceitares, reinicia o modo que estava ativo; se o arranque falhar, oferece
repor o backup.

`PUID`/`PGID` diferentes do dono de `elisdb-data/` ou `configs/` são recusados:
a TUI nunca corre `sudo` nem `chown`.

## Flags (sem menu)

Úteis para scripts:

```bash
python start.py --status
python start.py --list-users [--json]
python start.py --add-user maria --server-url https://elisdb.exemplo.pt
python start.py --revoke maria --yes
```

## Limites

- Não faz deploy nem substitui o CI: usa `scripts/deploy-server.sh` para isso.
- Não executa `docker compose down -v` nem apaga dados.

## Nota: Docker via snap

Em instalações do Docker empacotadas como *snap*, o `security_opt:
no-new-privileges` combinado com um entrypoint em script pode falhar com
`exec /usr/local/bin/entrypoint.sh: operation not permitted` no modo Docker. Não
é um problema do compose (num Docker normal, como o da VPS, funciona): é uma
limitação do confinamento do snap. Para testar localmente com snap, usa um
override só de teste:

```bash
# /tmp/compose.snap.yaml
services:
  elisdb-server:
    security_opt: !reset []

COMPOSE_FILE=compose.yaml:/tmp/compose.snap.yaml python start.py
```

Não commites esse override; a VPS usa o compose normal.
- Não altera o esquema da base de dados.
