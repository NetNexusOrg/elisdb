# ElisDB Memento — importação de clientes

Ferramentas para levar os clientes do export CSV do **Memento Database** para o
servidor **ElisDB**, sem alterar a app Android nem o servidor.

O comando principal é interativo e foi pensado para uso manual depois de cada
export do Memento:

```bash
./elisdb-memento/importar
```

Não tem flags, `--dry-run` nem `--yes`. O resumo e a confirmação cliente a
cliente são a pré-visualização; se responderes `n`, nada é enviado.

## O que faz

1. Lê e valida `elisdb-memento/ELIS.csv` (UTF-8, com ou sem BOM) **antes de
   contactar o servidor**.
2. Garante a chave ed25519 dedicada em
   `elisdb-memento/memento-import.config.json`.
3. Obtém a hora e a lista de clientes do servidor com pedidos assinados.
4. Compara os nomes normalizados (trim, espaços colapsados, maiúsculas) e
   **ignora quem já existe**. Nunca atualiza nem apaga clientes.
5. Mostra um resumo e escreve o relatório de pré-visualização.
6. Permite rever casos TIPO A/B e possíveis duplicados/renomeações.
7. Pede confirmação **cliente a cliente**.
8. Envia só os confirmados, em lotes de 200, para `POST /api/push`.
9. Escreve o relatório final e indica se há erros.

O `PDL` do Memento é o **contrato** da ElisDB. O contrato não é único (há PDLs
repetidos), por isso a comparação de duplicados usa sempre o **nome
normalizado**. A `Rota` do Memento é a **tag** do cliente.

| Memento | ElisDB |
| --- | --- |
| `Nome` | `nome` |
| `PDL` | `contrato` |
| `Rota` | `tag` |
| `Coordenadas` (`lat,lon`) | `latitude` + `longitude` |
| `Notas` | `notas` |

`Foto` e `Plantas` continuam a ser ignoradas.

## Verificação do CSV (safeguard)

Antes de criar a chave ou falar com o servidor, o script verifica se o export é
compatível com a ElisDB:

- exige as colunas `Nome`, `PDL`, `Coordenadas` e `Notas`;
- aceita `Rota` (usada como tag), `Foto` e `Plantas` (ignoradas) e avisa sobre
  colunas desconhecidas;
- exige pelo menos uma linha de dados e pelo menos um `Nome` preenchido;
- aplica os limites do servidor: nome até 200 caracteres (já normalizado),
  contrato/notas até 100 000 e tag/`Rota` até 100;
- valida as coordenadas: vazias são permitidas, pares completos, valores
  numéricos, latitude entre -90 e 90 e longitude entre -180 e 180.

Se houver um erro estrutural (colunas em falta, CSV vazio, campo demasiado
longo, coordenadas fora dos limites), o script **aborta com exit code 1 sem
contactar o servidor e sem escrever o relatório**. Coordenadas inválidas numa
linha individual são listadas; se a coordenada escolhida estiver fora dos
limites, esse cliente não é enviado, mas as restantes linhas podem ser
importadas.

## Ficheiros

| Ficheiro | Para que serve | Git |
|---|---|---|
| `ELIS.csv` | Export do Memento | ignorado |
| `memento-import.config.json` | URL, utilizador e private seed ed25519 | ignorado |
| `memento-import-report.txt` | Relatório da última execução (contém nomes) | ignorado |
| `memento-import-exclude.txt` | Nomes a não importar (opcional) | ignorado |

## Primeira execução

### Servidor local (Docker na mesma máquina)

Com o contentor `elisdb-server` a correr, basta executar o comando. Se o config
não existir, o script tenta criar a chave dedicada `memento-import` e copiá-la
para o caminho fixo:

```bash
./elisdb-memento/importar
```

O script usa por omissão `http://127.0.0.1:8000`.

### Servidor remoto

Cria a chave uma vez no servidor (ou onde o `docker compose` do ElisDB corre):

```bash
docker compose exec elisdb-server python -m app.keys issue memento-import \
  --server-url https://o-teu-servidor \
  --output /app/data/memento-import.config.json

docker compose cp elisdb-server:/app/data/memento-import.config.json \
  ./memento-import.config.json
```

Copia o ficheiro para `elisdb-memento/memento-import.config.json` e garante que
só tu o consegues ler:

```bash
chmod 600 elisdb-memento/memento-import.config.json
```

Depois o comando é o mesmo:

```bash
./elisdb-memento/importar
```

Se o servidor estiver noutra máquina, o URL no config tem de ser o URL que o
teu computador usa para chegar ao servidor.

### VPS / fora do monorepo

O `./importar` é um script **bash**, não um ficheiro Python. Não o executes com
`python3 importar`; usa sempre:

```bash
./importar
```

Se não existir `elisdb-server/.venv`, o wrapper usa o `python3` ativo (por
exemplo, um venv já ativado) desde que tenha o módulo `cryptography`. Se não
tiver, cria um venv local:

```bash
cd elisdb-memento
python3 -m venv .venv
.venv/bin/pip install cryptography
./importar
```

## O que aparece durante a execução

Primeiro mostra a verificação do CSV: linhas, linhas com nome, nomes únicos,
sem coordenadas, TIPO A, coordenadas inválidas, avisos e erros. Se o safeguard
falhar, termina aqui.

Depois mostra um resumo com linhas lidas, clientes no servidor, novos, já
existentes, excluídos, nomes vazios, erros de parsing, clientes sem
coordenadas, TIPO A/B e possíveis duplicados.

Depois pode perguntar:

- **Rever TIPO A/B um a um?** — por omissão `N`. TIPO A tem vários pares de
  coordenadas no mesmo campo; TIPO B tem o mesmo nome em várias linhas. Se
  responderem `s`, é possível escolher o par a usar.
- **Rever possíveis duplicados/renomeações?** — por omissão `N`, que os salta
  todos. Se responderem `s`, cada caso pode ser incluído (`s`), saltado (`n`)
  ou visto (`v`).

No fim, para cada cliente:

```text
[12/375] NOME DO CLIENTE
  PDL (contrato): 129020
  Coordenadas: 38.123, -9.123
  Notas: ...

Importar? [s/N] (v=ver, f=finalizar, q=abortar)
```

- `s` — importa este cliente.
- `n` ou Enter — salta este cliente.
- `v` — mostra as notas completas e, se houver, o cliente existente
  semelhante.
- `f` — termina a revisão e envia os clientes já confirmados; o atual e os
  restantes ficam como **não revistos**.
- `q` — aborta tudo e não envia nada, incluindo os que já tinham sido
  confirmados.

Não há uma segunda pergunta "Aplicar todos?". Só os `s` seguem para envio.

## Relatório e exclusões

O relatório é escrito antes da confirmação e atualizado no fim. Inclui o estado
por cliente: `importado`, `duplicado`, `saltado`, `nao_revisto`, `abortado`,
`stale` ou `erro`.

O ficheiro `memento-import-exclude.txt` é opcional. Uma linha por nome, com o
nome tal como aparece na ElisDB/Memento (a normalização é trim, espaços
colapsados e maiúsculas). Linhas vazias e linhas começadas por `#` são
ignoradas.

## Limitações

- **Fotos não são importadas.** O CSV do Memento tem URLs de fotos, não
  imagens. A coluna `Rota` é importada como a **tag** do cliente (valor com
  trim, preservando maiúsculas/minúsculas).
- **Eliminações não são detetadas.** O CSV não tem tombstones. Se um cliente
  foi apagado na ElisDB mas continua no Memento, volta a ser criado. Usa o
  ficheiro de exclusões ou apaga também no Memento.
- **Renomeações parecem clientes novos.** Não há um ID estável entre as duas
  apps; o script avisa por semelhança e contrato, mas a decisão é manual.
- **O contrato (PDL) não é único.** Não uses o contrato como chave de
  duplicação.
- **Servidor remoto:** a criação automática da chave só funciona com o Docker
  local; para remoto é preciso o setup manual acima.

## Segurança

- `memento-import.config.json` contém a private seed ed25519. Nunca o versiones,
  nunca o mostres e mantém permissões `600`.
- O relatório e o ficheiro de exclusões contêm nomes de clientes; ficam fora do
  Git.
- Antes de importar em produção, faz uma cópia de segurança do servidor. Ver
  `elisdb-server/docs/backup.md`.
- O script nunca imprime a private seed; o comando de criação da chave no
  servidor imprime-a, por isso não executes esse comando em logs públicos.

## Depois de importar

1. Confirma o total no servidor com uma chamada autenticada a `/api/latest`
   (`client_count`).
2. Na app Android, toca em **Sincronizar** para receber os novos clientes.
3. Volta a executar o comando depois do próximo export; os clientes já
   existentes são ignorados.

## Testes

```bash
elisdb-server/.venv/bin/python -m pytest -q elisdb-memento
```

## Script antigo

`migrar_memento.py` continua disponível para gerar um ficheiro SQLite de
migração (`--input`, `--output`, `--timestamp`). O fluxo recomendado para o dia
a dia é o `./elisdb-memento/importar`, que usa a API e não escreve diretamente
na base de dados.
