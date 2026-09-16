## 1. Rate limiting

- [x] 1.1 Implementar rastreador de rate limiting em memória (por chave autenticada e por IP) e expor via middleware em `app/main.py`; verificar que pedidos normais passam e pedidos excessivos devolvem 429
- [x] 1.2 Ler limites de `ELISDB_RATE_LIMIT_REQUESTS` e `ELISDB_RATE_LIMIT_WINDOW_SECONDS` com defaults e fallback em valores inválidos; verificar com `docker compose up` e um pedido excessivo manual
- [x] 1.3 Confirmar que `/health` e `/api/time` (não autenticados) são limitados por IP e que os endpoints autenticados são limitados por chave; verificar manualmente com `curl`

## 2. Config file permissions

- [x] 2.1 Alterar `app/keys.py:issue_and_register` para criar o ficheiro de config com permissões `0600`; verificar com `python -m app.keys issue` que o ficheiro fica `0600`
- [x] 2.2 Confirmar que o caminho de provisioning (`app/provision.py`) herda a escrita `0600` via `issue_and_register`; verificar com `docker compose up` e `ls -l` na pasta de configs
