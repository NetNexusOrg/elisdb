## Why

O servidor ElisDB não expõe informação de versão, tornando difícil verificar qual versão está a correr em produção. O endpoint `/health` apenas retorna `{"status": "ok"}` sem contexto de versão, e o Dockerfile não propagou versão alguma. Isto é importante para diagnóstico, suporte e alinhamento entre cliente/servidor.

## What Changes

- Criar ficheiro `elisdb-server/VERSION` com a versão SemVer do servidor
- Modificar o endpoint `/health` para incluir a versão na resposta: `{"status": "ok", "version": "1.0.0"}`
- Propagar versão via build arg no Dockerfile para estar disponível em runtime
- Adicionar variável de ambiente `ELISDB_VERSION` para exposição via compose

## Capabilities

### New Capabilities

- `server-version`: Exposição da versão do servidor via endpoint `/health` e variáveis de ambiente

### Modified Capabilities

- `server-observability`: Adicionar versão à resposta de health para diagnóstico operacional

## Impact

- `elisdb-server/app/main.py`: Modificar endpoint `/health` para incluir versão
- `elisdb-server/VERSION`: Novo ficheiro com versão SemVer
- `elisdb-server/Dockerfile`: Adicionar build arg `VERSION` e copiar ficheiro VERSION
- `elisdb-server/compose.yaml`: Adicionar variável de ambiente `ELISDB_VERSION`
- Compatibilidade: resposta `/health` expande de `{"status": "ok"}` para `{"status": "ok", "version": "..."}` (adição não-breaking)