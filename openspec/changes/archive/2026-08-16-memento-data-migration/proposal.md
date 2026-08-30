## Why

Um conjunto de dados de clientes vive hoje no **Memento Database**, exportado para `elisdb-memento/ELIS.csv`. Quer-se migrá-lo para o formato do elisdb (servidor SQLite) para que os mesmos clientes passem a ser usados na app. O script é **standalone** e não altera a app nem o servidor: produz um ficheiro de import pronto, que é inserido manualmente mais tarde.

## What Changes

- Script Python standalone em `elisdb-memento/` que lê `ELIS.csv` e gera um ficheiro de import SQLite (ou SQL) compatível com o schema alvo do servidor.
- O script **não escreve na base de dados de produção** — apenas produz o ficheiro de import.
- Trata casos especiais de coordenadas (TIPO A e TIPO B), de nomes duplicados (UNIQUE NOCASE) e de campos vazios, reportando tudo no relatório final.

## Análise real do export (481 linhas)

Colunas: `Rota, PDL, Nome, Coordenadas, Notas, Foto, Plantas`

- **Coordenadas vazias**: 25 casos.
- **Nº ímpar de valores em Coordenadas**: 0 casos (todos os campos têm 0, 2 ou 4 números).
- **PDL vazio**: vários (inclui o nome vazio com PDL "cinemas").

### TIPO A — múltiplos pares no mesmo campo (6 casos, todos com 4 números = 2 pares válidos)

| Nome | Coordenadas (pares) |
|---|---|
| RESTAURANTE CASA ALGARVIA | `37.086146,-8.221660` e `37.085012,-8.221341` |
| BEIRA BAGGA - FATACA | `37.582833,-8.739640` e `37.583198,-8.738020` |
| HOTEL PALÁCIO DE TAVIRA | `37.128690,-7.649476` e `37.128743,-7.649034` |
| GOLF SÃO LOURENÇO | `37.037707,-8.009497` e `37.031370,-8.012059` |
| IMPORTACO BEJA | `38.133745,-7.796633` e `38.140382,-7.785180` |
| RESTAURANTE DA GALÉ | `37.081708,-8.317569` e `37.085388,-8.313001` |

### TIPO B — mesmo Nome em várias linhas (8 casos confirmados)

| Nome | Nº linhas | Coordenadas |
|---|---|---|
| *(nome vazio)* | 5 | `37.642489,-7.659222`, 3× vazio, `37.100602,-8.001749` (uma com PDL="cinemas") |
| CLUB HOUSE | 3 | `37.208761,-7.548375`, vazio, `37.208918,-7.548069` |
| RESTAURANTE VERSATILE | 2 | `37.093918,-8.192704` e `37.093934,-8.192704` |
| PRONTO CHIPS | 2 | `37.159160,-7.890430` e `37.159375,-7.890670` |
| RESTAURANTE AROMA BOM SABOR | 2 | idênticas `37.081972,-8.031312` (notas iguais) |
| VILAS TV'S | 2 | `37.202066,-7.548670` e `37.208918,-7.548069` |
| VERANDA | 2 | `37.202244,-7.552469` e `37.208918,-7.548069` |
| FRUTAS CASEMIRO | 2 | `37.036079,-7.890675` e `37.036056,-7.891506` (notas distintas) |

> Nota: o utilizador estimou 7 casos TIPO B; a análise real encontrou **8** (inclui o nome vazio e FRUTAS CASEMIRO). Todos os TIPO B têm nomes que colidem com UNIQUE COLLATE NOCASE (alguns diferem só em maiúsculas/espaços, ex. "FRUTAS CASEMIRO ").

## Mapping (já decidido)

- `PDL` → contrato
- `Nome` → nome
- `Coordenadas` → latitude + longitude (com validação)
- `Notas` → notas
- `Rota`, `Foto`, `Plantas` → ignorar

## ⚠️ Decisões em aberto (escolher antes de implementar)

**D1 — Estratégia de resolução para coordenadas múltiplas (TIPO A e TIPO B).**
Opções candidatas (escolher uma, ou combinar):
- **O1**: manter a primeira ocorrência/par válido; mover os restantes pares para `notas` como texto de referência.
- **O2**: escolher o par mais próximo do **centróide** de todos os pares do caso.
- **O3**: pedir input manual **linha a linha** via CLI durante a execução (prompt por caso).
- **O4**: combinação — usar a primeira por omissão, mas permitir `--interactive` para decidir por caso.
- O script deve **sempre** reportar cada caso resolvido no relatório final, qualquer que seja a estratégia.

**D2 — Nomes com variação só de maiúsculas/espaços** (ex. "FRUTAS CASEMIRO " com espaço à frente). Colapsar sob o NOCASE? Normalizar o nome (trim/upper) no output para garantir UNIQUE NOCASE sem colisões residuais?

**D3 — Linha com Nome vazio** (5 linhas, uma com PDL="cinemas"). Ignorar todas, ou tentar reconstruir o nome a partir de Notas/PDL?

**D4 — created_at/updated_at (epoch millis)**: usar o timestamp da execução do script, ou um valor fixo/parametrizado?

**D5 — Output**: SQLite (ficheiro `.db` pronto a copiar) ou SQL de import (instruções `INSERT`)? Ou ambos (flag)?

**D6 — Campos vazios**: PDL vazio → contrato `NULL`; notas vazias → `NULL` (não string vazia)?

## Capabilities

### New Capabilities
- `memento-migration`: script standalone que converte o export Memento (CSV) para o formato de dados do elisdb, com validação de coordenadas, resolução de conflitos e relatório final.

### Modified Capabilities
<!-- none -->

## Impact

- **Novo**: diretório `elisdb-memento/` com o script e (novo) ficheiro de output gerado.
- **Não altera**: app Android, servidor elisdb-server, schema da base de dados.
- **Compatível com**: schema alvo do servidor (clientes + fotos) — mas nesta fase **sem fotos** (coluna Foto ignorada).
- **Testes**: o script deve ser testável (funções puras de parsing/validação) e produzir um relatório verificável.
