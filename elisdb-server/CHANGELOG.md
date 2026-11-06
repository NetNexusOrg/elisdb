# Histórico de versões — ElisDB Server

Servidor de sincronização (FastAPI + SQLite) que guarda clientes e fotos e
mantém a app Android sincronizada. A versão de release vive em
`elisdb-server/VERSION`.

## [Unreleased]

## [1.5.0] - 2026-11-06

### Added

- Identidade estável da instância em `/health` e `/api/latest`, e limite de
  registos por push anunciado ao cliente.
- Idempotência opcional do push por `Idempotency-Key`, com resposta repetida
  sem voltar a gravar e conflito `409` quando o conteúdo muda.
- Histórico de clientes paginado (`limit`, `after_id`, `next_after_id`) e
  registo de eliminações no histórico.
- Comandos de backup, verificação e restauro com manifesto, checksums e
  verificação de integridade.
- Quotas de armazenamento de fotos por chave e por instância, e limpeza
  periódica de fotos órfãs mesmo sem pushes.
- Upload de fotos apenas para fotos referenciadas por um cliente.
- O servidor recusa eliminações com data anterior ou igual à última edição
  (last-write-wins) e valida datas negativas ou demasiado futuras.

### Changed

- O pull lê clientes e fotos em lotes numa única ligação SQLite; hashes de
  fotos em falta são calculados uma vez e reutilizados.
- A seed privada deixa de ser impressa por omissão no provisionamento; está
  disponível apenas com `--print-seed`.
- Caminhos de configuração de utilizadores são validados e confinados.
- Os valores por omissão do `compose.yaml` não criam contas nem expõem o
  servidor fora de localhost; o arranque valida utilizadores e URL.

## [1.4.0] - 2026-11-04

### Added

- Campo opcional `tag` nos clientes: persistido na base de dados, aceite no
  push, devolvido no pull, incluído na listagem e registado no histórico de
  edições.
- Migração automática e idempotente da coluna `clientes.tag` em bases de dados
  existentes.

### Changed

- O limite de 100 caracteres da tag é validado na API.
