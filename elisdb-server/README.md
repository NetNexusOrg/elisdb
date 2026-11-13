# ElisDB Server

O **servidor ElisDB** guarda os teus dados (clientes e fotos) e mantém a app
Android sincronizada. Corre **apenas** em Docker (Docker-only).

## Início rápido

Na pasta `elisdb-server/`:

```bash
docker compose up -d
```

O servidor fica em `http://localhost:8000`. Confirma que está vivo:

```bash
curl http://localhost:8000/health
# → {"status":"ok","version":"...","api_version":...}
```

Para ligar um telemóvel, emite a chave dele dentro do contentor:

```bash
docker compose exec elisdb-server python -m elisdb.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Isto cria o ficheiro `<nome>.config.json` — importa-o no ecrã **Configuração** da
app e carrega em **Sincronizar** (a app também sincroniza ao abrir e ao guardar
um cliente). Ver [Configuração](docs/configuracao.md).

## Documentação

Guias práticos (instalação, execução, ligar telemóveis, resolução de problemas,
[cópia de segurança e restauro](docs/backup.md)): [`docs/`](docs/index.md).

Antes de partilhares o servidor com outras pessoas, lê
[Segurança e modelo de confiança](docs/seguranca.md): o dataset é partilhado e
qualquer chave registada pode ler, editar e apagar todos os clientes e fotos.
