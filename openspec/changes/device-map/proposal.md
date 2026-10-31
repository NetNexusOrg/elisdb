# Proposal

## Why

A ficha de um cliente já guarda fotos, mas não tem onde arrumar o **mapa de
dispositivos** — o esquema/planta que o técnico desenha ou fotografa no local e
o PDF do equipamento (datasheet, planta do edifício). Hoje essas informações
vivem fora da app (galeria do telemóvel, email, papel), pelo que não chegam aos
outros telemóveis nem sobrevivem à troca de dispositivo.

As fotos existentes não servem: a galeria é uma sequência de imagens e o
visualizador assume imagens — um PDF lá dentro apareceria como ficheiro
quebrado, e uma imagem de mapa misturada com as fotos do cliente perde o
significado.

## What Changes

- O formulário do cliente ganha a secção **"Mapa de dispositivos"**, com
  **Adicionar imagem** (galeria ou câmara) e **Adicionar PDF** (seletor de
  ficheiros), a lista dos anexos já escolhidos e a possibilidade de os remover.
- O ecrã de detalhe do cliente ganha um cartão com os anexos: tocar numa imagem
  abre o visualizador; tocar num PDF abre-o na aplicação externa que o leitor
  tiver instalada.
- Os anexos viajam com o cliente: entram no push, no pull e no merge de
  duplicados, com a mesma regra de identidade por conteúdo das fotos — um
  anexo inalterado não é reenviado nem reescrito.
- O servidor guarda-os ao lado das fotos (mesma pasta, mesma limpeza de
  órfãos), com endpoints autenticados próprios e uma lista de tipos aceites.
- O histórico do cliente passa a registar que anexos foram adicionados ou
  removidos.

## Capabilities

### New Capabilities

- `client-device-map`: escolher, guardar, mostrar e sincronizar os anexos
  (imagens e PDF) do mapa de dispositivos de um cliente.
- `server-document-storage`: guardar e servir os anexos de um cliente, com
  validação de tipo e tamanho, e mantê-los no ciclo de vida do registo.

### Modified Capabilities

<!-- none: as fotos continuam com o comportamento atual; os anexos são uma lista à parte -->

## Impact

- **Servidor**: `app/database.py` (coluna `kind` na tabela de anexos, migração,
  `save_client` com a lista de documentos, histórico), `app/models.py`
  (`DocumentoIn`/`DocumentoOut`, `kind`), `app/main.py`
  (`PUT`/`GET /api/documents/{id}`, push e pull).
- **Android**: `data/local/Documento.kt` + DAO + migração 9→10 (BD v10), `ClienteComFotos`
  com a lista de documentos, `data/ClienteRepositoryImpl.kt` (staging e gravação
  dos anexos), `remote/RemoteDto.kt` e `remote/ApiClient.kt`, `sync/*`
  (push, pull, merge, sweep), `ui/form/*` e `ui/detalhe/*`, `res/xml/file_paths.xml`.
- **Compatibilidade**: o formato do push/pull ganha campos opcionais; um
  servidor antigo ignora-os, por isso **o servidor tem de ser atualizado antes
  da app**.
- **Não afetado**: autenticação, resolução de conflitos, paginação por
  `delivery_ts`, formato das fotos.
- **Fora de âmbito**: reordenar os anexos (ficam pela ordem em que foram
  adicionados), pré-visualizar PDFs dentro da app, migrar a coluna `Plantas` do
  memento.
