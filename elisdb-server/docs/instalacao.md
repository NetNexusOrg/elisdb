# Instalação do servidor

Aqui está como preparar o servidor no teu computador. Depois de fazer isto uma
vez, segue para a [Execução](execucao.md) para o subir.

## O que precisas de ter

- **Docker** e o plugin **Docker Compose** instalados no computador.
- Acesso à internet, para descarregar a imagem na primeira vez.

O servidor corre **apenas** em Docker — não existe execução fora de contentor.

Confirma que o Docker está instalado:

```bash
docker --version
docker compose version
```

Se estes comandos não existirem, instala o Docker (e o Docker Compose) antes de
continuar.

## Passo 1 — obter o código do servidor

O servidor vive na pasta `elisdb-server/` do projeto ElisDB, junto à app Android
(`elisdb-android/`). Obtém o projeto a partir do repositório:

```bash
git clone <URL-do-repositorio-do-projeto>
cd elisdb/elisdb-server
```

## Passo 2 — subir o servidor

```bash
docker compose up -d
```

Isto constrói a imagem (na primeira vez) e sobe o contentor em segundo plano.
Os dados ficam guardados num volume Docker (`elisdb-data`), que sobrevive a
reinícios.

## Passo 3 — verificar que ficou tudo certo

```bash
curl http://localhost:8000/health
# → {"status":"ok","version":"...","api_version":...}
```

Se a resposta contiver `"status":"ok"`, o servidor está vivo e pronto.

## O que saber antes da primeira execução

- O servidor **não vem com nenhuma chave pré-registada**: até emitires a chave
  de um telemóvel, os pedidos da app são recusados. Isso é normal — é a medida
  de segurança.
- Os dados ficam guardados no volume `elisdb-data` (base de dados em
  `/app/data/elisdb.db` dentro do contentor).

## Passo seguinte

[Execução](execucao.md) — como subir e gerir o servidor em Docker.
