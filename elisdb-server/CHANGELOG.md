# Histórico de versões — ElisDB Server

Servidor de sincronização (FastAPI + SQLite) que guarda clientes e fotos e
mantém a app Android sincronizada. A versão de release vive em
`elisdb-server/VERSION`.

## [Unreleased]

### Notas

- Changelog inicial do componente servidor. As alterações ao servidor passam a
  ser registadas aqui.

### Changed

- O campo `tag` aceita até 10 tags separadas por `;` (100 caracteres por tag e
  500 no total), normalizadas em maiúsculas, sem duplicados e ordenadas.

## [1.4.0] - 2026-11-04

### Added

- Campo opcional `tag` nos clientes: persistido na base de dados, aceite no
  push, devolvido no pull, incluído na listagem e registado no histórico de
  edições.
- Migração automática e idempotente da coluna `clientes.tag` em bases de dados
  existentes.

### Changed

- O limite de 100 caracteres da tag é validado na API.
