# Histórico de versões — ElisDB Memento

Scripts de migração do export Memento (CSV) para o formato de dados do ElisDB.

## [Unreleased]

### Added

- Comando interativo `./elisdb-memento/importar` e `importar_memento.py`:
  importa os clientes novos do export Memento para o servidor ElisDB através da
  API, com chave ed25519 dedicada, comparação por nome normalizado, relatório,
  exclusões opcionais e confirmação cliente a cliente.
- Safeguard do CSV antes de contactar o servidor: colunas obrigatórias, limites
  de tamanho do servidor e validação de coordenadas; erros estruturais abortam
  sem enviar nem escrever relatório.
- Testes do importador com HTTP/Docker mockados.

### Changed

- `migrar_memento.py` usa a mesma normalização de nomes do servidor (trim,
  colapsar espaços internos, maiúsculas).
- Linhas com erro de parsing no campo `Coordenadas` deixam de gerar clientes;
  continuam a ser reportadas.
- TIPO B só cria cliente se pelo menos uma linha tiver coordenadas válidas ou
  vazias; grupos só com erros são apenas reportados.
- TIPO A/B guardam notas/contratos originais para revisão interativa.

### Notes

- O relatório e o ficheiro de exclusões contêm dados de clientes e são
  ignorados pelo Git.
- Fotos e `Rota` continuam fora do âmbito da importação.
