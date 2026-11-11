# Upgrade do servidor para 2.0.0 (vps01)

Runbook para atualizar o servidor da ElisDB na `vps01` para a versão mais
recente, preservando a base de dados e as fotos existentes.

> **Regra de ouro:** a base de dados (`elisdb.db`) e as fotos (`photos/`) vivem
> no volume `elisdb-data`. Nenhum passo deste runbook pode remover, substituir
> ou apagar esse volume. Um backup guardado dentro do volume não conta como
> backup: tem de ser copiado para fora da VPS.

## 0. Pré-requisitos

- Acesso SSH à `vps01` e permissão para `docker compose`.
- Código em `main` já publicado e CI verde (`scripts/deploy-server.sh` recusa
  código sujo, não publicado ou com CI vermelho).
- Espaço livre suficiente para um backup completo (BD + fotos) **fora** do
  volume e na máquina local.
- Janela de manutenção combinada: durante o upgrade o servidor fica indisponível
  alguns minutos.

## 1. Registar o estado atual

No diretório do projeto na VPS:

```bash
docker compose ps
docker compose exec -T elisdb-server sh -c 'ls -lh /app/data/elisdb.db; du -sh /app/data/photos'
docker compose exec -T elisdb-server sh -c 'find /app/data/photos -type f | wc -l'
```

Contagens a guardar (antes do upgrade):

```bash
docker compose exec -T elisdb-server python - <<'PY'
import os, sqlite3
db = sqlite3.connect(os.environ.get("ELISDB_DB", "/app/data/elisdb.db"))
for table in ("clientes", "fotos", "tombstones", "keys"):
    print(table, db.execute(f"select count(*) from {table}").fetchone()[0])
PY
```

Guardar também o commit atual (`git rev-parse HEAD`) e a imagem em uso
(`docker compose images`).

## 2. Criar, verificar e copiar o backup

```bash
docker compose exec -T elisdb-server python -m app.backup create /app/data/backups/pre-2.0.0
docker compose exec -T elisdb-server python -m app.backup verify /app/data/backups/pre-2.0.0
```

Copiar o backup para o host e depois para fora da VPS:

```bash
docker compose cp elisdb-server:/app/data/backups/pre-2.0.0 ./pre-2.0.0-backup
tar czf pre-2.0.0-backup.tgz pre-2.0.0-backup
sha256sum pre-2.0.0-backup.tgz
scp pre-2.0.0-backup.tgz <destino-seguro>:/caminho/
```

- O backup contém dados de clientes: guardar em local encriptado e com acesso
  restrito.
- Confirmar que o `sha256` do ficheiro copiado coincide com o original.

## 3. Ensaio de restauro (obrigatório)

Restaurar para diretórios temporários **dentro** do contentor, sem tocar nos
dados reais:

```bash
docker compose exec -T elisdb-server sh -c 'rm -rf /tmp/drill && mkdir -p /tmp/drill'
docker compose exec -T elisdb-server python -m app.backup restore /app/data/backups/pre-2.0.0 --db /tmp/drill/elisdb.db --photos /tmp/drill/photos
docker compose exec -T elisdb-server python - <<'PY'
import sqlite3
db = sqlite3.connect("/tmp/drill/elisdb.db")
for table in ("clientes", "fotos", "tombstones"):
    print(table, db.execute(f"select count(*) from {table}").fetchone()[0])
PY
```

Comparar as contagens com as registadas no passo 1 e abrir uma foto do
diretório `/tmp/drill/photos` para confirmar que o conteúdo está legível.
Apagar o ensaio no fim.

## 4. Upgrade

```bash
cd <diretório-do-projeto-na-vps>
git fetch --prune
git checkout main
git pull --ff-only
docker compose up -d --build
```

- **Nunca** usar `docker compose down -v` nem remover o volume `elisdb-data`.
- O deploy não copia `data/`, `*.db` nem `*.config.json`.
- As migrações correm automaticamente no arranque; se falharem, não continuar.

## 5. Validar depois do upgrade

```bash
docker compose ps
curl -fsS http://127.0.0.1:8000/health
curl -fsS http://127.0.0.1:8000/ready
docker compose logs --tail=200 elisdb-server
```

Repetir as contagens do passo 1 e confirmar que **clientes**, **fotos** e
**tombstones** não diminuíram. Confirmar também a contagem/tamanho das fotos e
descarregar 2–3 fotos pela API com uma credencial nova.

## 6. Credenciais novas e revogação das antigas

Emitir novas credenciais para os mesmos utilizadores:

```bash
docker compose exec -T elisdb-server python -m app.keys issue <utilizador> --server-url <URL>
```

Testar cada credencial nova (sync + foto) antes de revogar as antigas. Só depois:

```bash
docker compose exec -T elisdb-server python -m app.keys revoke <utilizador>
```

A revogação marca `revoked_at` nas chaves antigas (não apaga linhas, para
preservar o histórico de auditoria). Confirmar que a chave antiga recebe `401` e
que a nova continua a funcionar.

## 7. App 2.0.0

- Distribuir o APK assinado da release `v2.0.0`.
- Instruir os utilizadores a:
  1. abrir a app antiga e carregar em **Sincronizar**;
  2. confirmar que não há alterações por sincronizar;
  3. desinstalar a app antiga;
  4. instalar a nova e importar a nova credencial.

> Sem o passo 1–2, alterações que só existam no telemóvel perdem-se. O servidor
> não as consegue recuperar.

## 8. Rollback

Se o upgrade falhar:

1. Parar o contentor novo.
2. Voltar ao commit/imagem anterior.
3. Se a BD ou as fotos estiverem afetadas, restaurar a partir do backup
   verificado:

```bash
docker compose exec -T elisdb-server python -m app.backup restore /app/data/backups/pre-2.0.0
```

4. Validar contagens, fotos e `/health` antes de reabrir o serviço.

## Checklist final

- [ ] Contagens e tamanho das fotos registados antes do upgrade.
- [ ] Backup criado, verificado e copiado para fora da VPS (com checksum).
- [ ] Restauro de ensaio validado.
- [ ] `docker compose up -d --build` sem remover o volume.
- [ ] `/health` e `/ready` OK.
- [ ] Contagens e fotos iguais ou superiores às iniciais.
- [ ] Credenciais novas emitidas e testadas.
- [ ] Chaves antigas revogadas.
- [ ] APK 2.0.0 distribuído e utilizadores instruídos.
