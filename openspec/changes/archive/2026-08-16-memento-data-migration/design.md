## Context

O `ELIS.csv` (481 linhas) do Memento tem que ser convertido para o formato do elisdb. O schema alvo suporta apenas 1 par de coordenadas por cliente e nomes únicos (COLLATE NOCASE). Há 6 casos TIPO A (2 pares no mesmo campo) e 8 casos TIPO B (nomes repetidos, incluindo o nome vazio), 25 campos de coordenadas vazias e 0 números ímpares.

## Goals / Non-Goals

**Goals:**
- Script standalone que gera um ficheiro de import válido.
- Todos os casos de conflito são resolvidos por uma estratégia escolhida e **reportados** individualmente.
- Não altera app, servidor nem a base de produção.

**Non-Goals:**
- Migrar fotos (coluna Foto ignorada nesta fase).
- Escrever na base de produção.
- Alterar o schema alvo.

## Decisões (resolvidas)

**D1 — Estratégia de resolução de coordenadas múltiplas (TIPO A e TIPO B usam a mesma lógica).**
- **ESCOLHIDO: O1** — Manter o primeiro par/linha válido; mover os restantes pares para `notas` como texto de referência.
- *Impacto*: determina o parsing de TIPO A e o colapso de TIPO B. O relatório final deve listar cada caso e a coordenada escolhida.

**D2 — Normalização de nomes.** **ESCOLHIDO:** colapsar variações só de maiúsculas/espaços sob NOCASE; normalizar o nome (trim/upper) no output.

**D3 — Linhas com Nome vazio** (5). **ESCOLHIDO:** ignorar (não geram clientes).

**D4 — Timestamps.** **ESCOLHIDO:** `created_at`/`updated_at` = timestamp da execução do script (parametrizável por flag).

**D5 — Formato do output.** **ESCOLHIDO:** SQLite (`.db`).

**D6 — Campos vazios.** **ESCOLHIDO:** contrato/notas vazios → NULL.

## Decisões de implementação (não dependem das em aberto)

**E1 — Parsing de coordenadas.**
- `re.findall(r'-?\d+(?:\.\d+)?', campo)` extrai todos os números.
- len == 2 → usar. len % 2 == 0 e > 2 → TIPO A (conflito). len % 2 == 1 → erro de parsing.
- Funções puras testáveis (`parse_coords`, `classify`).

**E2 — Agrupamento por nome normalizado.**
- Chave = `nome.strip().upper()`; TIPO B = grupos com >1 linha.
- O nome vazio é um grupo à parte (D3 decide se entra).

**E3 — Geração de id.**
- UUID v4 (formato `xxxx-...`) consistente com a app (verificar a convenção exata da app antes).

**E4 — Relatório.**
- Contagens + lista explícita de cada caso TIPO A/B resolvido (nome + coordenadas consideradas + escolhida).

## Files / Structure

```
elisdb-memento/
  migrar_memento.py     # script standalone
  ELIS.csv              # input (já existe)
  <output>.db ou .sql   # gerado, fora do repo ou gitignored
  tests/                # testes do parsing (pytest, sem tocar em nada da app/servidor)
```

## Risks / Trade-offs

- [Estratégia errada gera coordenadas incorretas] → Mitigado pelo relatório que permite auditar antes do import.
- [Nomes normalizados podem perder informação] → O relatório lista os nomes originais.

## Migration Plan

N/A — script de uso único; o output é revisto manualmente antes do import.

## Open Questions

D1–D6 acima (decisões em aberto — a escolher pelo utilizador antes de implementar).
