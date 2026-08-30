## Why

O `SyncCoordinator.pull()` faz uma única chamada `api.pull()` (`SyncCoordinator.kt:237-240`) e ignora os cursores de paginação `next_since`/`nextSinceId` que a resposta transporta (`remote/RemoteDto.kt:73-74`). Pior: num full-pull (`watermark == null`) o código apaga qualquer cliente local não devolvido nessa única página (`SyncCoordinator.kt:269-276`).

Consequências:
- Se o servidor alguma vez paginar `/api/pull`, o cliente apaga silenciosamente registos que apenas estão ausentes da página 1 (perda de dados).
- Mesmo hoje, pulls incrementais só trazem uma página; registos além do `limit` nunca chegam ao dispositivo.

Isto viola o princípio offline-first de nunca perder dados locais por uma leitura parcial.

## What Changes

- O pull passa a iterar enquanto a resposta trouxer `next_since`/`nextSinceId`, acumulando registos de todas as páginas antes de reconciliar.
- O cliente NUNCA apaga registos locais com base numa página parcial do pull: o sweep de remoção de locais só ocorre após a página completa (ou, num full-pull, após receber a lista completa do servidor).
- O watermark é avançado com o cursor da última página.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-sync`: nova requirement "Pull exhausts pagination" — o app SHALL seguir o cursor `next_since` até receber a lista completa e SHALL NOT remover registos locais baseado numa página parcial do pull.
- `sync-incremental-pull`: a requirement "Pull watermark" passa a indicar que o cliente consome o cursor `next_since` devolvido pelo servidor em vez de parar na primeira página.

## Impact
- Android: `sync/SyncCoordinator.kt` (loop de paginação no `pull`), `remote/RemoteDto.kt` (já traz `nextSinceId`), `data/local` (sweep só após página completa).
- Servidor: sem alteração (já devolve `next_since`/`next_since_id`); o cliente passa a consumi-los.
- Testes: testar pull paginado sem perda de registos locais.
