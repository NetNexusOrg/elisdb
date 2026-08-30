# ElisDB Server

O **servidor ElisDB** guarda os teus dados (clientes e fotos) e mantém a app
Android sincronizada. É um programa Python simples de correr no teu computador.

## Início rápido

Na pasta `elisdb-server/`:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --host 0.0.0.0
```

O servidor fica em `http://127.0.0.1:8000` (nesta máquina) — ou acessível na
rede com `--host 0.0.0.0`.

Para ligar um telemóvel, emite a chave dele:

```bash
python -m app.keys issue <nome-do-utilizador> --server-url <URL-do-servidor>
```

Isto cria o ficheiro `<nome>.config.json` — importa-o nas **Definições** da app
e sincroniza.

## Documentação

Guias práticos (instalação, execução, ligar telemóveis, resolução de problemas):
[`docs/`](docs/index.md).

## Testes

```bash
.venv/bin/python -m pytest
```

## Em Docker (alternativa)

O projeto inclui `Dockerfile` e `compose.yaml` — ver a secção "Docker" em
[`docs/execucao.md`](docs/execucao.md).