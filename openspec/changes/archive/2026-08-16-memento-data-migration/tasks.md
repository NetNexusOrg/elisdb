# Tasks: Memento data migration

## 0. Pré-requisito

- [x] 0.1 **Resolução das decisões em aberto (D1–D6 do design.md)** — requer escolha do utilizador antes de implementar o parsing/resolução

## 1. Script — parsing do CSV

- [x] 1.1 Criar `elisdb-memento/migrar_memento.py` (standalone, sem dependências da app/servidor): ler `ELIS.csv` com `csv.DictReader`
- [x] 1.2 Implementar funções puras de parsing: `extract_numbers(coord)` (todos os números), `classify(coords)` → `ok | tipo_a | erro` (2 números=ok, pares>2=tipo_a, ímpar=erro)

## 2. Script — resolução de conflitos (após D1/D2/D3 decididos)

- [x] 2.1 Implementar a estratégia escolhida para TIPO A (múltiplos pares no mesmo campo)
- [x] 2.2 Agrupar por nome normalizado (trim/upper) e implementar o colapso de TIPO B
- [x] 2.3 Tratar linhas com nome vazio e coordenadas vazias conforme decidido
- [x] 2.4 Confirmar que o output final não tem nomes duplicados sob COLLATE NOCASE

## 3. Script — geração do output

- [x] 3.1 Gerar ids TEXT únicos consistentes com a convenção da app
- [x] 3.2 Gerar `created_at`/`updated_at` (epoch millis) conforme D4
- [x] 3.3 Escrever o ficheiro de import (SQLite ou SQL) conforme D5, compatível com o schema alvo (clientes; sem fotos)

## 4. Relatório final

- [x] 4.1 Emitir relatório: linhas processadas, ignoradas, avisos, erros de parsing, TIPO A resolvidos, TIPO B resolvidos — com a lista explícita de cada caso (nome + coordenadas consideradas + escolhida)

## 5. Testes

- [x] 5.1 Adicionar testes (pytest) para o parsing de coordenadas (ok / tipo_a / erro) e para o colapso de nomes duplicados
- [x] 5.2 Rodar os testes; confirmar que passam

## 6. Verificação manual

- [x] 6.1 Correr o script sobre o `ELIS.csv` real e gerar o output
- [x] 6.2 Rever o relatório: confirmar 6 TIPO A e 7 TIPO B listados (o grupo "nome vazio" é ignorado por D3), 0 erros de parsing, 25 vazias tratadas, sem duplicados no output
- [x] 6.3 (Opcional) Importar o output numa base de teste e validar o schema
