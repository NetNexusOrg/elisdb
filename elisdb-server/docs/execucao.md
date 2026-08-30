# Execução do servidor

Com a instalação feita (ver [Instalação](instalacao.md)), este guia mostra como
correr o servidor, como saber se está a funcionar e onde ficam os dados.

## Correr o servidor

A partir da pasta `elisdb-server/`:

```bash
.venv/bin/uvicorn app.main:app
```

O servidor fica a ouvir em `http://127.0.0.1:8000` (só nesta máquina).

**Para a app num telemóvel (na mesma rede) aceder ao servidor**, tens de o pôr a
ouvir em todas as interfaces:

```bash
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

> Na app, usa o **IP da máquina do servidor** (ex.: `http://192.168.1.10:8000`),
> nunca `127.0.0.1` — esse endereço aponta para o próprio telemóvel.

## Como saber se está a funcionar

Com o servidor a correr, abre no navegador (ou com o comando):

```bash
curl http://127.0.0.1:8000/health
# → {"status":"ok"}
```

Se responderes `{"status":"ok"}`, o servidor está vivo e pronto.

> Todos os outros endereços (`/api/*`) só respondem a telemóveis com a chave
> registada — até lá devolvem `401`. Isso é o comportamento normal e esperado.

## Onde ficam os dados

- **Base de dados:** `elisdb.db` na pasta onde iniciaste o servidor (ou o
  caminho definido em `ELISDB_DB`).
- **Fotos:** numa pasta `photos/` ao lado da base de dados (ou a pasta definida
  em `ELISDB_PHOTOS`).

Para mudar estes caminhos, vê a referência em [Configuração](configuracao.md).

## Parar o servidor

Prima `Ctrl+C` na janela do terminal onde o servidor está a correr.

## Correr em Docker (alternativa)

Se preferires (ou quiseres o servidor sempre acessível), o projeto inclui um
`Dockerfile` e um `compose.yaml`:

```bash
docker build -t elisdb-server .      # construir a imagem (uma vez)
docker compose up -d                 # subir o serviço
```

- O serviço fica em `http://localhost:8000`.
- **Os dados sobrevivem a reinícios:** ficam num volume (`elisdb-data`), em
  `/app/data` dentro do contentor.
- Verificação: `curl http://localhost:8000/health`.

> **Permissões dos dados:** no arranque, o contentor ajusta o dono de
> `/app/data` para `PUID`/`PGID` (por omissão `1000`/`1000`) antes de iniciar
> o servidor. Isto corrige automaticamente volumes que nascem com dono
> `root` — não precisas de apagar o volume para resolver. Se usares um *bind
> mount* do host, define `PUID`/`PGID` no `compose.yaml` com o id do teu
> utilizador (`id -u` / `id -g`) para que o servidor consiga escrever.

### Provisionar utilizadores automaticamente

Podes declarar os utilizadores no `compose.yaml` e o contentor cria as chaves
no arranque, sem entrar no contentor:

```yaml
environment:
  ELISDB_USERS: "maria,joao"      # lista separada por vírgulas
  ELISDB_SERVER_URL: "http://192.168.1.10:8000"   # URL que os telemóveis usam
```

- No arranque, o contentor emite uma chave para cada utilizador que ainda não
  tenha chave confiada (não reemite quem já tem — os telemóveis já ligados
  continuam a funcionar).
- Os ficheiros de configuração ficam em `/app/data/configs/` dentro do volume.
  Recupera-os com:

  ```bash
  docker cp elisdb-server:/app/data/configs/maria.config.json .
  ```

  (ou monta `/app/data/configs` como *bind mount* para aparecerem no host) e
  importa cada ficheiro na app — ver [Configuração](configuracao.md).

Logs e paragem:

```bash
docker compose logs -f    # acompanhar os logs
docker compose down       # parar (não apaga o volume de dados)
```

Para emitir a chave de um telemóvel dentro do contentor:

```bash
docker compose exec elisdb-server python -m app.keys issue <nome> --server-url http://<ip-do-host>:8000
```

O ficheiro `<nome>.config.json` é escrito dentro do contentor; copia-o para a
máquina e importa-o na app (ver [Configuração](configuracao.md)).

> Se a porta 8000 já estiver ocupada no teu computador (ex.: um servidor já a
> correr), muda o mapeamento `8000:8000` para outra porta no `compose.yaml`.

## Passo seguinte

[Configuração](configuracao.md) — como ligar cada telemóvel ao servidor.