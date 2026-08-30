## Context

Ver proposal.md - Why para a motivação e specs para o comportamento exigido. Estado atual: `FotoIn.dataB64`/`FotoOut.dataB64` transportam os bytes no push/pull e na resposta `duplicate`; `_canonical_from_conn` lê cada ficheiro e faz `base64.b64encode`; `_replace_fotos` descodifica e escreve. A app (`FilePhotoStore.storeBase64`/`loadBase64`, `PhotoMaterializer`) lê/grava via base64. O hash de conteúdo já existe na app (`PhotoHashes.sha256`, hex lowercase) e é usado no push (`photoPushState`).

## Goals / Non-Goals

**Goals:**
- Fotos viajam em binário nos dois sentidos (download e upload), sem base64 na API.
- Pull e push com metadados + hash; sem picos de memória (download/upload por foto).

**Non-Goals:**
- Sem `multipart` nem streaming complexo: uma foto (~1.4 MB) cabe num `ByteArray` no cliente e num body do servidor.
- Sem alterar a identidade de conteúdo existente (SHA-256 hex lowercase).

## Decisions

**D1: `FotoOut` passa a `{id, mime, ordem, hash}`; `FotoIn` passa a `{id, mime, ordem}` (sem `dataB64`)**
O conteúdo deixa de viajar no JSON em qualquer direção. O push envia só metadados; o pull devolve metadados + hash. A resposta `duplicate` também devolve hash, e o merge canónico descarrega pelo endpoint.
- *Alternativa*: manter `dataB64` num dos lados — rejeitado, cria assimetria e dois caminhos de materialização.

**D2: Endpoints `GET /api/photos/{id}` (download) e `PUT /api/photos/{id}` (upload), autenticados**
`GET` devolve `Response(content=bytes, media_type=mime)` (mime lido da BD). `PUT` escreve o body no ficheiro `fotos_dir/<id>` (id é o identificador global gerado pelo cliente) e devolve 204; é idempotente. O mime não precisa de ir no `PUT` — vai no push (metadados). Sem risco de path traversal: o caminho é sempre `_foto(id)`, com a validação existente.
- *Alternativa*: mime por `Content-Type`/query no PUT — rejeitado, o mime já é enviado no push.

**D3: Hash calculado no servidor no `_canonical_from_conn` (SHA-256 hex lowercase), sem coluna nova**
O servidor lê o ficheiro e faz `hashlib.sha256(data).hexdigest()`. Formato igual ao cliente (`%02x`). Sem migração.

**D4: Push em duas fases no cliente: `PUT` das fotos alteradas, depois `POST /api/push` só com metadados**
O skip por conteúdo (`photoPushState`) decide quais fotos sofrem `PUT` (hash diferente do último push) e quais são só referenciadas. `_replace_fotos` passa a registar as linhas a apontar para `_foto(id)`; mantém/verifica o ficheiro (linhas sem ficheiro são ignoradas, como hoje com "reference without stored copy").
- *Trade-off*: se o push falhar depois do upload, os ficheiros ficam órfãos no servidor; o sweep/limpeza de fotos mitiga, e o `PUT` é idempotente para retry.

**D5: `PhotoStore` ganha `storeBytes(bytes)`, `readBytes(uri)` e `contentHashMatches(uri, hash)`; remove `storeBase64`/`contentMatches`/`loadBase64`**
O push passa a ler `readBytes` e a fazer `PUT`; o pull compara por hash e faz `GET` + `storeBytes`. Sem caminhos base64 mortos.

## Risks / Trade-offs

- [N pedidos por sync (GET/PUT por foto alterada)] → Só para fotos novas/alteradas; steady-state é zero com skip por hash. Cada pedido é autenticado/signed.
- [Ficheiros órfãos no servidor se o push falhar após o PUT] → `PUT` idempotente + reutilização por id; limpeza futura.
- [Mudança de contrato quebra clientes antigos] → Servidor e app publicados juntos (como nas mudanças de assinatura).
- [Hash calculado a cada pull lê os ficheiros] → Já acontecia (base64 lia os ficheiros); mantém o IO, com menos rede e menos memória.

## Migration Plan

- Sem migração de dados/esquema. Deploy conjunto servidor+app. Rollback: reverter ambos.
- Clientes antigos (que esperam/`dataB64`) quebram — assumido, dado o deploy coordenado.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.