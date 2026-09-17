# Configuração do servidor — ligar um telemóvel

Este é o guia mais importante: **como dar acesso a um telemóvel**. No final,
vais ter um ficheiro de configuração (`<nome>.config.json`) que a app do
telemóvel importa no ecrã **Servidor**.

## Passo único: emitir a chave de um telemóvel

Um único comando faz tudo: gera a chave do telemóvel, regista-a no servidor (com
o nome do utilizador) e escreve o ficheiro de configuração com o URL do servidor
e a chave privada.

### Modo interativo (recomendado)

Corre `python -m app.keys` **sem argumentos** (dentro do contentor) e escolhe o
número da opção:

```
docker compose exec elisdb-server python -m app.keys
```

O menu pede, em sequência, o **URL do servidor** e o **nome do utilizador**
(se `ELISDB_SERVER_URL` existir, aparece como valor por omissão — basta
carregar em Enter). Depois escreve `<nome>.config.json` e regista a chave.

O mesmo menu permite **listar** e **remover** autenticações:

- `2` lista as chaves confiadas (id, utilizador, chave pública, data).
- `3` remove uma chave **após confirmação** (`s`/`N`) — o telemóvel que a
  usasse deixa de conseguir sincronizar.

### Modo com argumentos (para scripting)

```bash
docker compose exec elisdb-server python -m app.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Exemplo real:

```bash
docker compose exec elisdb-server python -m app.keys issue maria --server-url http://192.168.1.10:8000
```

O que acontece:

```
issued key for 'maria' (id issue-XXXXXXXXXXXX)
configuration written to: maria.config.json
private seed (import into app): <seed_base64>
registered public key:          <public_key_b64>
```

Há **um** sinal de sucesso: a criação do ficheiro **`maria.config.json`**.
O resto da saída é informativo.

O ficheiro é escrito dentro do contentor; copia-o para a tua máquina:

```bash
docker cp elisdb-server:/app/maria.config.json .
```

Então:

1. **Transfere** o ficheiro `maria.config.json` para o telemóvel (email, pen,
   transferência na rede, etc.).
2. **Na app:** abre o menu (⋮) → **Servidor** → **Importar configuração do servidor** e
   escolhe o ficheiro. O URL e a chave ficam preenchidos em simultâneo.
3. Abre o menu (⋮) → **Sincronização** e **sincroniza**. Deve aparecer "Sincronizado".

> O URL do servidor é obrigatório — passa por `--server-url`, pelo modo
> interativo ou pela variável `ELISDB_SERVER_URL`. Sem URL, o comando
> recusa-se a correr. Podes escolher outro nome de ficheiro com
> `--output <caminho>`.

> **A emissão é o único método de dar acesso** — não há comando para registar
> uma chave criada fora do servidor. Para retirar acesso, usa a opção `3`
> (remover) do menu interativo, ou apaga a chave da base de dados.

## Variáveis de ambiente (referência)

| Variável           | O que faz                                        | Omissão        |
| ------------------ | ------------------------------------------------ | -------------- |
| `ELISDB_DB`        | Caminho do ficheiro de dados (base de dados)      | `data/elisdb.db` |
| `ELISDB_PHOTOS`    | Pasta para os ficheiros de fotos                   | `data/photos` ao lado da base de dados |
| `ELISDB_SERVER_URL` | URL pelo qual a app alcança este servidor (usado por `app.keys issue` quando não passas `--server-url`) | — (obrigatória para emitir) |
| `ELISDB_USERS`     | Utilizadores a provisionar no arranque (lista separada por vírgulas). Se vazio ou ausente, todas as chaves auto-provisionadas são revogadas. | — |

Dentro do `compose.yaml`, as variáveis vão na secção `environment`. Exemplo:

```yaml
environment:
  ELISDB_DB: /app/data/elisdb.db
  ELISDB_PHOTOS: /app/data/photos
  ELISDB_USERS: "maria,joao"
  ELISDB_SERVER_URL: "http://192.168.1.10:8000"
```

> Usa sempre o mesmo valor de `ELISDB_DB` também nos comandos `app.keys` — as
> chaves registadas vivem nesse mesmo ficheiro de dados.

## Primeira execução

Numa base de dados nova, **nenhuma chave é confiada**: até emitires a primeira
chave, os telemóveis recebem `401` ao tentar sincronizar.

## Observabilidade (referência)

Podes consultar as chaves registadas com os endereços autenticados:

- `GET /api/keys` — lista todas as chaves (id, chave pública, comentário).
- `GET /api/keys/me` — mostra a chave que está a ser usada neste momento
  (devolve `key_id` e `username` do utilizador autenticado).

Numa base de dados nova, a lista está vazia.

## Passo seguinte

[Utilização](uso.md) — como funciona a sincronização e como resolver problemas.
