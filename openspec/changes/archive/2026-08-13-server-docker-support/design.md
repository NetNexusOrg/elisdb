# Design — Servidor em Docker

## Contexto

O servidor corre hoje com `.venv/bin/uvicorn app.main:app`. O processo usa três
variáveis de ambiente já suportadas:

- `ELISDB_DB` — caminho para o ficheiro SQLite (por omissão `elisdb.db`).
- `ELISDB_PHOTOS` — diretoria das fotos (por omissão `photos/` ao lado da DB).
- `ELISDB_KEYS_DB`? — não existe; a allowlist vive no mesmo SQLite.

O `/health` não exige autenticação e responde `{"status":"ok"}` — servirá de
base ao healthcheck do contentor. Não há código da app a alterar.

## Imagem Docker (`Dockerfile`)

- **Base:** `python:3.14-slim` (alinhada com a versão do venv local; padrão de
  base mínima oficial).
- **Multi-stage não é necessário** (sem compilação de código), mas usamos
  dois estágios apenas para manter a imagem final pequena e sem cache de build:
  1. `build` — instala as dependências de `requirements.txt` num wheel cache.
  2. `runtime` — copia só o `site-packages` instalado e o código da app.
- **Diretório de trabalho:** `/app` (código copiado para lá).
- **Porta exposta:** `8000` (`EXPOSE 8000`).
- **Comando:** `CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]`
  — o uvicorn vem do `uvicorn[standard]` das dependências; o `--host 0.0.0.0` é
  necessário para ser alcançável de fora do contentor.
- **Utilizador não-root:** criar `appuser` e correr como `USER appuser` para a
  imagem não correr como root (prática recomendada).

### Variáveis de ambiente no contentor

| Variável | Omissão | Notas |
|---|---|---|
| `ELISDB_DB` | `data/elisdb.db` | Dentro do volume de dados. |
| `ELISDB_PHOTOS` | `data/photos` | Dentro do volume de dados. |

As omissões são definidas como `ENV` no `Dockerfile` e podem ser sobrepostas no
`compose.yaml` ou na linha de comando.

## Volume de dados

O compose monta um volume `elisdb-data` em `/app/data`. Como os `ENV` do
Dockerfile apontam para `data/`, os dados persistem entre reinícios do
contentor sem configuração extra.

- A imagem é **stateless**; apenas `/app/data` é persistido.
- O `.dockerignore` exclui `data/`, `*.db`, `photos/`, `.venv/`,
  `__pycache__/` — assim a imagem não embala dados locais nem o venv.

## `compose.yaml`

Serviço único `elisdb-server`:

```yaml
services:
  elisdb-server:
    build: .
    container_name: elisdb-server
    ports:
      - "8000:8000"
    volumes:
      - elisdb-data:/app/data
    environment:
      ELISDB_DB: /app/data/elisdb.db
      ELISDB_PHOTOS: /app/data/photos
    healthcheck:
      test: ["CMD", "python", "-c", "import urllib.request as u; u.urlopen('http://127.0.0.1:8000/health')"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 10s

volumes:
  elisdb-data:
```

O healthcheck usa `python -c` (python existe na imagem) em vez de `curl`
(curl não está instalado na base slim).

## Documentação

- Secção **"Docker"** em `docs/execucao.md` com:
  - `docker build -t elisdb-server .`
  - `docker compose up -d`
  - verificação: `curl http://localhost:8000/health`
  - logs: `docker compose logs -f`
  - parar: `docker compose down`
  - nota de que os dados persistem no volume `elisdb-data` e que a allowlist
    de chaves se configura dentro do contentor via `docker compose exec` com o
    comando `app.keys issue` (tal como em [Configuração](configuracao.md)).

## Decisões e trade-offs

- **python:3.14-slim** em vez de `python:alpine`: mantém compatibilidade
  binária com as wheels de `cryptography` e `uvicorn[standard]`, evitando
  recompilação no build.
- **Healthcheck via `python -c`** em vez de instalar `curl` ou `wget` na
  imagem: mantém a imagem mínima.
- **Sem alterações ao código da app**: packaging/deployment apenas.

## Riscos e mitigação

- **Nenhuma alteração funcional** — as únicas peças novas são ficheiros de
  infraestrutura e documentação.
- **Porta ocupada no host**: o compose mapeia `8000:8000`; se já houver um
  uvicorn local a correr, o utilizador muda a porta do lado do host.
