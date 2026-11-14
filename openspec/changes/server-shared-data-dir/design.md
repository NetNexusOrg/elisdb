# Design

## Layout final

```
elisdb-server/
  docker/Dockerfile
  docker/entrypoint.sh
  elisdb/                 # pacote Python (fase 1)
  configs/                # runtime, 0700, gitignored (bind mount)
  elisdb-data/            # runtime, gitignored (bind mount)
    elisdb.db
    photos/
    apk/latest/
    backups/
  .env                    # 0600, gitignored
  .env.example            # versionado
  compose.yaml
  start.py                # fase 3
```

## Porque bind mounts e não volume nomeado

O operador precisa de correr o servidor **também sem Docker** (diagnóstico,
migrações, TUI) sobre os mesmos dados. Um volume nomeado esconde os ficheiros e
obriga a `docker cp`. Com bind mounts, o caminho do host é a fonte de verdade e o
container apenas o vê em `/app/data` e `/app/data/configs`.

`configs/` fica **fora** de `elisdb-data/` porque contém seeds privadas: assim o
backup de dados pode ser copiado/partilhado sem arrastar credenciais, e o
`.dockerignore` exclui-a sem excluir a BD.

## Permissões sem root

- `user: "${PUID:-1000}:${PGID:-1000}"` no compose; o entrypoint deixa de fazer
  `chown -R` e `setpriv`.
- Fontes dos bind mounts têm de existir e pertencer a `PUID:PGID` **antes** do
  `up`; se não existirem, o daemon do Docker cria-as como root.
- O arranque falha com mensagem acionável se `/app/data` ou `/app/data/configs`
  não forem graváveis, em vez de corromper ou rebentar mais tarde.
- Emergência: `docker compose exec -u root …`; nunca `chown` automático.

## Configuração

- `.env` é a única fonte de valores do operador; `compose.yaml` só interpola com
  defaults seguros.
- `ELISDB_DATA_DIR` (default `./elisdb-data`) resolve o bind mount; caminhos
  dentro do contentor ficam fixos (`/app/data/...`), para não depender do host.
- `.env.example` documenta todas as chaves; sem URL real, sem utilizadores.
- `ELISDB_USERS` desaparece. `ELISDB_SERVER_URL` só é exigido ao emitir chaves.

## Provisionamento

`elisdb/provision.py` é removido, tal como a chamada no entrypoint e as entradas
em compose/docs/testes. Servidor novo arranca com 0 chaves: `/health` responde,
`/api/push` e `/api/pull` recusam por falta de chave válida, e o operador cria a
primeira chave. A coluna `keys.provisioned` mantém-se (sem migração destrutiva);
novas chaves são sempre `0`.

## Migração da VPS (uma vez, com autorização)

1. Backup verificado (`elisdb.backup create`) + cópia off-VPS de backup e
   `configs/`.
2. Parar o contentor; `cp -a elisdb-data/configs/. configs/`; `chown -R 1001:1001
   configs elisdb-data`.
3. Criar `.env` com os valores atuais; trocar o `compose.yaml` host-local pela
   versão nova; `docker compose config` para validar.
4. `docker compose up -d --build`; verificar `/health`, contagens, chaves ativas,
   uma foto e `stat` 1001.
5. Rollback: restaurar `compose.yaml.bak-<ts>` e o `.env` anterior, `up -d
   --build`.

## Riscos

- **Dados**: mitigado por backup verificado + off-VPS e por nunca usar
  `down -v`/`rsync --delete`.
- **Permissões**: se `PUID` não corresponder ao dono, o arranque falha cedo (por
  desenho) em vez de escrever ficheiros inacessíveis.
- **Transição**: durante o deploy o contentor fica parado enquanto se move
  `configs/`; os clientes são offline-first e sincronizam depois.
- **Backup do script**: o backup pré-deploy passa a usar `elisdb.backup` (já
  disponível na imagem atual); a fase 1 já validou esse caminho.
