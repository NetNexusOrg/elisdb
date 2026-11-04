## Why

A equipa de pest control usa a ElisDB e o Memento em paralelo durante a fase de
experimentação. Criar cada cliente nas duas apps duplica trabalho e deixa as
bases desalinhadas. O Memento já exporta `ELIS.csv`; falta uma forma segura,
simples e repetível de levar os clientes novos desse export para o servidor
ElisDB sem tocar nos clientes que já lá estão.

## What Changes

- Novo comando único `./elisdb-memento/importar`, sem flags e sem modos de
  automação, que lê o export fixo `elisdb-memento/ELIS.csv` e importa clientes
  através da API HTTP já existente (`/api/time`, `/api/clients`, `/api/push`).
- Antes de criar a chave ou contactar o servidor, um **safeguard** valida o CSV:
  colunas obrigatórias, limites de tamanho do servidor e coordenadas. Erros
  estruturais abortam; coordenadas inválidas numa linha são reportadas e essa
  linha é saltada.
- A importação é **aditiva e idempotente**: compara nomes normalizados com os
  clientes do servidor e ignora os que já existem; nunca atualiza nem apaga.
- Antes de enviar, mostra um resumo, escreve um relatório local, permite rever
  TIPO A/B e possíveis duplicados/renomeações e pede **confirmação cliente a
  cliente** (`s`, `n`, `v`, `f`, `q`).
- Só os clientes confirmados são enviados, em lotes de 200, com a hora do
  servidor em `updated_at`.
- Configuração dedicada em `memento-import.config.json`; na primeira execução
  local a chave é criada via Docker, ou é usado um config pré-provisionado para
  servidores remotos. A private seed nunca é impressa.
- Relatório em `memento-import-report.txt` e exclusões opcionais em
  `memento-import-exclude.txt`; ambos contêm dados de clientes e ficam fora do
  Git.
- `migrar_memento.py` passa a usar a mesma normalização de nomes do servidor e
  deixa de gerar clientes para linhas cujo campo de coordenadas é um erro de
  parsing.
- A coluna `Rota` do Memento passa a ser importada como a **tag** do cliente
  (valor com `trim`, preservando maiúsculas/minúsculas; limite de 100
  caracteres).

Não há alterações ao servidor, à app Android, ao schema da base de dados nem à
API. As fotos continuam fora do âmbito; a `Rota` passa a ser importada como tag.

## Capabilities

### New Capabilities
- `memento-import`: importação interativa e autenticada dos clientes do export
  Memento para o servidor ElisDB, com comparação por nome, confirmação 1 a 1,
  relatório e salvaguardas contra alterações a clientes existentes.

### Modified Capabilities
- Nenhuma. A capacidade `memento-migration` continua a descrever a geração do
  ficheiro de migração; a nova capacidade cobre a importação via API.

## Impact

- Novos: `elisdb-memento/importar_memento.py`, `elisdb-memento/importar`,
  `elisdb-memento/test_importar_memento.py`, `elisdb-memento/README.md`.
- Alterados: `elisdb-memento/migrar_memento.py` (normalização e erros de
  parsing), `elisdb-memento/test_migrar_memento.py`, `elisdb-memento/CHANGELOG.md`,
  `.gitignore` (relatório e exclusões).
- Sem dependências novas: o script usa a stdlib e a `cryptography` já presente
  no venv do servidor.
- Operação: o servidor tem de estar acessível; a primeira execução local pode
  criar a chave dedicada via Docker. Servidores remotos exigem um config
  provisionado uma vez.
