# Backups e restauro

Uma cópia útil tem **duas** coisas: a base de dados (`elisdb.db`) e os ficheiros
das fotos (`photos/`). Só uma delas deixa o sistema inconsistente.

> Os `configs/*.config.json` contêm as chaves privadas dos telemóveis. Não entram
> no backup automático; se os copiares, trata a cópia como um segredo.

## Automáticos (omissão)

O servidor cria um backup **verificado** a cada `ELISDB_BACKUP_INTERVAL_SECONDS`
(omissão 24 h). Detalhes:

- nome `auto-AAAAMMDD-HHMMSS` em `ELISDB_BACKUP_DIR`
  (omissão `elisdb-data/backups/`);
- só conta depois de `verify` passar; se falhar, a pasta fica para diagnóstico
  mas não é considerada válida;
- a retenção (`ELISDB_BACKUP_RETENTION`, omissão `3`) apaga apenas `auto-*` — os
  manuais (`pre-deploy-*`, `pre-migrate-*`) nunca são tocados;
- antes de copiar, exige espaço livre para o dobro do tamanho atual + 64 MiB; se
  não houver, avisa e salta essa execução;
- o primeiro backup de uma instalação nova corre ~60 s após o arranque.

```bash
# listar automáticos e manuais
docker compose exec -T elisdb-server python -m elisdb.backup list

# aplicar a retenção agora (mantém 3 auto-*)
docker compose exec -T elisdb-server python -m elisdb.backup prune --keep 3
```

O **Status** da TUI mostra quantos existem e qual é o mais recente.

> Os backups vivem **no mesmo servidor**. Copia-os para fora (rsync/rclone) para
> te protegeres da perda do VPS. Se preferires agendar no host com cron, desliga
> o agendador interno com `ELISDB_BACKUP_INTERVAL_SECONDS=0`.

## Manual, com o servidor a correr

```bash
docker compose exec -T elisdb-server \
  python -m elisdb.backup create /app/data/backups/antes-de-x
docker compose exec -T elisdb-server \
  python -m elisdb.backup verify /app/data/backups/antes-de-x
```

`create` usa a API de backup do SQLite (cópia consistente com escritas em curso),
valida `PRAGMA integrity_check` e grava um manifesto com SHA-256 por foto.
`verify` confirma a BD, as fotos e a correspondência com as linhas de `fotos`.

Cópia a frio (servidor parado), se preferires ficheiros crus:

```bash
docker compose stop elisdb-server
cp elisdb-data/elisdb.db /tmp/elisdb-backup.db
cp -a elisdb-data/photos /tmp/elisdb-backup-photos
docker compose start elisdb-server
```

## Restaurar

```bash
docker compose stop elisdb-server
docker compose exec -T elisdb-server \
  python -m elisdb.backup restore /app/data/backups/antes-de-x
docker compose start elisdb-server
curl http://localhost:8000/health
```

O restauro verifica tudo antes de trocar os ficheiros; se algo falhar, mantém a
BD e as fotos anteriores. Se preferires fazer à mão, copia
`backup-elisdb.db → elisdb-data/elisdb.db` e
`backup-photos/. → elisdb-data/photos/` (o `/.` copia o conteúdo, não a pasta) e
acerta o dono com `sudo chown -R "$(id -u):$(id -g)" elisdb-data`.

## Restaurar uma cópia antiga

Cada telemóvel guarda um *watermark* dos últimos dados recebidos. Se o servidor
ficar com um `latest_updated_at` anterior a esse marcador, o telemóvel nunca
voltaria a receber o que falta.

A app deteta isto: avisa que o servidor parece restaurado e faz uma
**sincronização completa**, voltando a descarregar tudo. A primeira sincronização
após um restauro pode demorar mais — é esperado.

## O que os backups não incluem

- `configs/` (credenciais) — copia à parte, como segredo.
- `apk/latest/` — os APKs vêm das releases do GitHub.
- O `.env` e o `compose.yaml` — guarda-os onde guardas a configuração da máquina.
