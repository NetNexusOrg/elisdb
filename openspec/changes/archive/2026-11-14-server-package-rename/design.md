# Design

## Abordagem

Renomear com `git mv` para preservar histórico e atualizar referências de forma
mecânica. O interior do pacote usa imports relativos (`from .database import …`),
por isso o rename não propaga alterações aos módulos.

## Ordem

1. `git mv app elisdb`.
2. Substituir `from app` / `import app` / `-m app.` / `uvicorn app.` fora do
   pacote (testes, scripts, CI, Dockerfile, entrypoint, docs).
3. Atualizar o comando no spec `server-auth`.
4. Apagar `uv.lock`.
5. Correr `pytest`, `docker build` e o passo de import do CI.

## Porque não mover o `Dockerfile`

A `compose.yaml` de produção na VPS é host-local (excluída do rsync) e usa
`build: .`. Mover o `Dockerfile` nesta fase partiria o deploy. Essa mudança fica
para `server-shared-data-dir`, que já altera o compose e exige janela.

## Compatibilidade

- A imagem continua com `WORKDIR /app` e monta os dados em `/app/data`.
- O comando `python -m elisdb.keys` substitui `python -m app.keys`; os
  ficheiros `.config.json` e o formato não mudam.
- Nenhuma migração de SQLite é executada por esta change.

## Verificação

- `python -m pytest` verde (12 ficheiros de teste).
- `docker build` conclui e `docker run --rm … python -c "import elisdb.main"` ok.
- `grep -rn "app\." elisdb-server` não encontra referências ao pacote antigo
  (exceto caminhos `/app/...`).
