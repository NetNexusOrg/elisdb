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
# → {"status":"ok"}
```

Para ligar um telemóvel, emite a chave dele dentro do contentor:

```bash
docker compose exec elisdb-server python -m app.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Isto cria o ficheiro `<nome>.config.json` — importa-o nas **Definições** da app
e sincroniza. Ver [Configuração](docs/configuracao.md).

## Documentação

Guias práticos (instalação, execução, ligar telemóveis, resolução de problemas):
[`docs/`](docs/index.md).
