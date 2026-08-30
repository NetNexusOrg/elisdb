## Context

Três falhas confirmadas no review. Ver proposal.md - Why para a motivação e specs para o comportamento exigido.

- **Assinatura**: `auth.build_message` assina `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`; `main.py:require_auth` passa `request.url.path` (sem query) e o cliente `ApiClient` assina só o `path`. O contrato de assinatura vive em `server-auth` e no `Signer.kt`.
- **Fotos**: `fotos.id` é PK global e o ficheiro em disco usa `fotos_dir/<id>` como caminho. O upsert `ON CONFLICT(id) DO UPDATE SET cliente_id=excluded.cliente_id` re-atribui a foto.
- **Paginação**: `pull_all` ordena `ORDER BY updated_at, id` mas o cursor guarda só `updated_at`; o filtro seguinte é `updated_at > since`, saltando empates.

## Goals / Non-Goals

**Goals:**
- Assinar a query string de forma idêntica nos dois lados.
- Impedir re-atribuição de fotos entre clientes.
- Paginação correta com empates de `updated_at`.

**Non-Goals:**
- Não introduzir nonce/replay-binding adicional (mantém-se a janela de timestamp).
- Não migrar o esquema de `fotos` (a PK global mantém-se; o id continua a ser o nome do ficheiro).
- Não implementar paginação no cliente Android (hoje não usa `limit`).

## Decisions

**D1: O valor assinado passa a ser `path` + `?query` quando a query existe; mensagem mantém `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`**
O servidor usa `request.url.path` e `request.url.query` para reconstruir `path?query`; o cliente constrói a mesma string antes de assinar. `build_message`/`buildMessage` continuam a receber uma string `path` única — não muda a assinatura, só o que lhe é passado.
- *Alternativa*: assinar o URL completo (com host) — rejeitado, quebra compatibilidade e é redundante (o host não muda em trânsito).
- *Risco de ordenação da query*: como o cliente assina a query exata que envia (e só usa `since`, `deleted_at`), não há reordenação pelo servidor; o `request.url.query` do ASGI preserva a string crua.

**D2: Rejeitar colisão cross-cliente de foto antes de escrever ficheiros**
No início de `_replace_fotos`, verificar `SELECT cliente_id FROM fotos WHERE id = ?` para cada id recebido com `dataB64`; se pertencer a outro cliente, `raise ValueError`. Isto propaga para `push` → 400, antes de qualquer escrita em disco. O caso normal (mesmo cliente re-envia o mesmo id) mantém o upsert.
- *Alternativa*: tornar o id composto `(cliente_id, id)` — rejeitado, colide com o nome de ficheiro global e exige migração.

**D3: Cursor composto `(since, since_id)` na paginação**
Adicionar o parâmetro opcional `since_id` ao pull; quando presente, filtrar `updated_at > since OR (updated_at = since AND id > since_id)`. A resposta passa a incluir `next_since_id` (o `id` do último registo da página) além de `next_since`. Sem `since_id`, mantém-se `updated_at > since` (compatível). O cliente lê com `ignoreUnknownKeys`, por isso o campo novo não quebra versões antigas.
- *Alternativa*: cursor opaco único `updated_at:id` — rejeitado, obriga a codificar/decodificar e muda a semântica de `since` existente.

## Risks / Trade-offs

- [Assinar a query quebra clientes antigos que assinam só o path] → Servidor e app são publicados juntos; não há versões intermédias. Documentado como breaking implícito.
- [Rejeição de colisão de foto pode recusar um push legítimo de dois dispositivos a editar o mesmo cliente] → Os ids são UUID gerados por dispositivo; a colisão cross-cliente só ocorre com dados corrompidos, pelo que rejeitar é o comportamento seguro.
- [Empates de `updated_at` exigem que o cliente envie `since_id` para paginar] → Hoje o cliente não pagina (`limit` não é usado); o servidor fica correto e o cliente pode adotar o cursor composto depois sem nova migração.

## Migration Plan

- Sem migração de dados/esquema. Deploy conjunto servidor+app (assinatura). Rollback: reverter ambos os lados em conjunto.
- O campo `next_since_id` é aditivo na resposta do pull; clientes antigos ignoram-no.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.