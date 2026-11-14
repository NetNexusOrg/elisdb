# Instalar o servidor

Uma vez por máquina. No fim tens o servidor a responder em
`http://localhost:8000`.

## O que precisas

- **Docker** com o plugin **Docker Compose**.
- Internet na primeira subida (para descarregar a imagem base).

```bash
docker --version
docker compose version
```

## 1. Obter o código

```bash
git clone https://github.com/NetNexusOrg/elisdb.git
cd elisdb/elisdb-server
```

## 2. Preparar os diretórios

Os dados e as credenciais vivem no host, em `./elisdb-data` e `./configs`. Têm de
pertencer ao utilizador que corre o contentor — o container **não** faz `chown` e
recusa arrancar se não puder escrever.

```bash
mkdir -p elisdb-data configs
```

Se usares `PUID`/`PGID` diferentes do teu, garante os donos:
`sudo chown -R "$(id -u):$(id -g)" elisdb-data configs`.

## 3. Subir

```bash
docker compose up -d
```

Opcional: `cp .env.example .env` (modo `0600`) para definir URL, porta ou
tunables. As omissões servem para uso local.

## 4. Verificar

```bash
curl http://localhost:8000/health
# → {"status":"ok","version":"...","api_version":2,"instance_id":"..."}
```

`"status":"ok"` significa que está vivo. Para o resto dos `/api/*` é preciso uma
chave — o servidor arranca sem nenhuma.

## Onde ficam as coisas

| Caminho (host) | O que é |
|---|---|
| `elisdb-data/elisdb.db` | Base de dados SQLite |
| `elisdb-data/photos/` | Ficheiros das fotos |
| `elisdb-data/apk/latest/` | APKs publicados para atualização |
| `elisdb-data/backups/` | Backups automáticos e manuais |
| `configs/` | Credenciais `*.config.json` (0600) |

Nada disto é apagado por `docker compose down` ou por um rebuild.

## Próximo passo

[Ligar um telemóvel](ligar-telemovel.md).

> O servidor também pode correr **em modo local** (sem Docker) através da TUI —
> útil para desenvolvimento. Em produção usa Docker. Ver
> [servidor](../operar/servidor.md).
