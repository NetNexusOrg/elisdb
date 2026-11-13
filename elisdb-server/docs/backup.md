# Cópia de segurança e restauro

O servidor guarda tudo o que a app sincroniza em dois sítios, dentro do volume
Docker `elisdb-data` (montado em `/app/data`):

- **`elisdb.db`** — a base de dados SQLite com clientes, chaves e histórico.
- **`photos/`** — os ficheiros das fotos, referenciados pela base de dados.

Uma cópia de segurança útil tem de incluir **os dois**. Copiar só o `.db` deixa
fotos em falta; copiar só as fotos perde os clientes.

> Os ficheiros de configuração emitidos (`configs/*.config.json`) contêm a chave
> privada de cada telemóvel. Se os incluíres na cópia, guarda-a como um segredo:
> nunca a coloques no Git nem num local partilhado.

## Ferramenta integrada (recomendada)

O servidor inclui um utilitário que cria uma cópia **consistente** da base de
dados e de todas as fotos referenciadas, com um manifesto de checksums:

```bash
docker compose exec -T elisdb-server python -m elisdb.backup create /app/data/backups/2026-11-06
docker compose exec -T elisdb-server python -m elisdb.backup verify /app/data/backups/2026-11-06
```

`verify` confirma o SHA-256 da base de dados e de cada foto, corre
`PRAGMA integrity_check` e garante que o manifesto cobre exatamente as linhas de
`fotos`. Para restaurar (com o servidor parado, para ninguém estar a escrever):

```bash
docker compose stop elisdb-server
docker compose exec -T elisdb-server python -m elisdb.backup restore /app/data/backups/2026-11-06
docker compose start elisdb-server
curl http://localhost:8000/health
```

O restauro verifica tudo antes de trocar os ficheiros; se algo falhar, mantém a
base de dados e as fotos anteriores. A ferramenta não copia `configs/` nem
chaves privadas.

## Backups automáticos

Agenda a ferramenta integrada no anfitrião (cron ou systemd timer), fora do
contentor. Exemplo diário às 03:00:

```cron
0 3 * * * root cd /srv/elisdb && docker compose exec -T elisdb-server python -m elisdb.backup create /app/data/backups/$(date +\%Y\%m\%d) && docker compose exec -T elisdb-server python -m elisdb.backup verify /app/data/backups/$(date +\%Y\%m\%d)
```

Copia as cópias para fora do volume (rsync/rclone para outro host) e define uma
retenção explícita — por exemplo 7 diárias e 4 semanais. O `verify` faz parte da
rotina: uma cópia que nunca foi verificada não conta como recuperável. O
`scripts/deploy-server.sh` também cria uma cópia `pre-deploy-*` antes de cada
atualização, quando o servidor está a correr.

## Fazer a cópia manualmente (servidor a correr)

O SQLite tem uma API de backup que produz um ficheiro consistente mesmo com o
servidor a escrever. Corre-a dentro do contentor:

```bash
docker compose exec -T elisdb-server python -c \
  "import sqlite3; s=sqlite3.connect('/app/data/elisdb.db'); d=sqlite3.connect('/app/data/backup-elisdb.db'); s.backup(d); d.close(); s.close()"
```

Depois copia a base de dados e as fotos para fora do contentor:

```bash
docker cp elisdb-server:/app/data/backup-elisdb.db ./backup-elisdb.db
docker cp elisdb-server:/app/data/photos ./backup-photos
```

Por fim, remove o ficheiro temporário de dentro do contentor:

```bash
docker compose exec -T elisdb-server rm /app/data/backup-elisdb.db
```

Guarda `backup-elisdb.db` e `backup-photos/` juntos, com a data no nome (por
exemplo `elisdb-2026-10-31/`).

### Alternativa: cópia a frio

Para uma cópia totalmente parada (sem escritas durante a cópia), para o
servidor primeiro:

```bash
docker compose stop elisdb-server
docker cp elisdb-server:/app/data/elisdb.db ./backup-elisdb.db
docker cp elisdb-server:/app/data/photos ./backup-photos
docker compose start elisdb-server
```

## Restaurar uma cópia

1. **Para o servidor** para ninguém estar a sincronizar durante o restauro:

   ```bash
   docker compose stop elisdb-server
   ```

2. **Substitui a base de dados e as fotos** dentro do contentor:

   ```bash
   docker cp ./backup-elisdb.db elisdb-server:/app/data/elisdb.db
   docker cp ./backup-photos/. elisdb-server:/app/data/photos/
   ```

   O `/.` no fim copia o **conteúdo** da pasta, sem criar uma subpasta
   `backup-photos` dentro de `photos/`.

3. **Volta a subir o servidor** e confirma que responde:

   ```bash
   docker compose start elisdb-server
   curl http://localhost:8000/health
   ```

4. **Sincroniza em cada telemóvel** (menu ⋮ → **Configuração** → **Sincronizar**)
   para que a app volte a ficar alinhada com o servidor.

> Se o contentor tiver sido recriado (por exemplo com `docker compose up` depois
> de uma atualização), os caminhos acima continuam a apontar para o volume
> `elisdb-data`, que é preservado. Só apagar o volume (`docker compose down -v`)
> é que perde os dados.

## Restaurar uma cópia mais antiga: aviso importante

Cada telemóvel guarda um **marcador** (watermark) com o instante dos últimos
dados que recebeu do servidor. Se restaurares uma base de dados mais antiga, o
servidor pode ficar com um `latest_updated_at` **anterior** a esse marcador.

Sem proteção, o telemóvel pediria apenas “o que mudou desde o marcador” e nunca
voltaria a receber os registos que o servidor restaurado já não tem — ficando
permanentemente diferente do servidor.

A app deteta esta situação: antes de cada sincronização compara o valor mais
recente do servidor com o marcador local. Se o servidor estiver atrás, avisa
(“O servidor parece ter sido restaurado de uma cópia mais antiga”) e faz uma
**sincronização completa**, voltando a descarregar tudo. Depois do restauro, a
primeira sincronização de cada telemóvel pode por isso demorar mais e trazer
mais dados do que o habitual — é o comportamento esperado.

> A sincronização acontece ao abrir a app, ao guardar ou apagar um cliente e no
> botão **Sincronizar**. Não há sincronização periódica de fundo; falhas
> transitórias de rede são retentadas automaticamente com backoff exponencial.
> O ecrã **Configuração** mostra “Alterações por sincronizar” quando deteta que
> o servidor tem dados diferentes dos locais.
