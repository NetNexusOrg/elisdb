# ElisDB Server

Servidor de sincronização do ElisDB: guarda os clientes e as fotos, com
autenticação por chave ed25519, e serve a app Android.

## Início rápido

```bash
# com Docker
docker compose up -d
curl http://localhost:8000/health

# TUI de operação (estado, arrancar/parar, utilizadores, configuração)
python start.py
```

Emitir a chave de um telemóvel:

```bash
docker compose exec elisdb-server \
  python -m elisdb.keys issue <nome> --server-url <URL>
```

O ficheiro `configs/<nome>.config.json` aparece no host; importa-o na app.

## Documentação

Toda a documentação vive na raiz do repositório, em [`../docs/`](../docs/index.md):

- [Instalar](../docs/usar/instalar-servidor.md) · [Ligar um telemóvel](../docs/usar/ligar-telemovel.md)
- [Operar](../docs/operar/servidor.md) · [TUI](../docs/operar/tui.md) · [Configuração](../docs/operar/configuracao.md)
- [Backups](../docs/operar/backup.md) · [Segurança](../docs/operar/seguranca.md) · [Problemas](../docs/operar/problemas.md)

O dataset é partilhado: qualquer chave registada lê, edita e apaga todos os
clientes. Antes de partilhares o servidor, lê [Segurança](../docs/operar/seguranca.md).
