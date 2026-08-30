## Why

A proteção anti-replay (`auth.py:23`, `auth.py:51`) aceita qualquer assinatura dentro de ±5 minutos (`TIMESTAMP_WINDOW_SECONDS = 300`) sem nonce nem cache de replay. Um `PUT`/`DELETE`/`POST /api/push` assinado e capturado pode ser repetido dentro dessa janela e ser aceite novamente — numa API de sincronização isto permite re-executar uma eliminação ou push por um atacante que intercepte o tráfego (mesmo sobre HTTPS, um proxy ou logging pode expor).

## What Changes

- Introduzir um nonce de curto TTL (ou cache de assinaturas já vistas) para rejeitar replay dentro da janela de timestamp.
- Manter a janela de timestamp como primeira barreira; o nonce fecha a janela de replay.

## Capabilities

### New Capabilities

### Modified Capabilities
- `server-auth`: a requirement "Anti-replay protection" passa a exigir que assinaturas não possam ser repetidas dentro da janela — via nonce de curto TTL ou cache de assinaturas consumidas.

## Impact
- Servidor: `app/auth.py` (nonce/cache), `app/main.py` (passa nonce verificado). O cliente Android já envia timestamp; pode (opcionalmente) incluir um nonce nos pedidos para reforço.
- Testes: `test_main.py` (replay do mesmo pedido assinado dentro da janela é rejeitado).
