# Tasks — server-package-rename

## 1. Renomear o pacote

- [x] 1.1 `git mv elisdb-server/app elisdb-server/elisdb`
- [x] 1.2 Confirmar que não há `from app`/`import app` dentro do pacote (imports relativos)

## 2. Atualizar referências externas

- [x] 2.1 Testes: `from app.` → `from elisdb.` (12 ficheiros em `elisdb-server/tests/`)
- [x] 2.2 `Dockerfile`: `uvicorn app.main:app` → `uvicorn elisdb.main:app`
- [x] 2.3 `docker-entrypoint.sh`: `python -m app.provision` → `python -m elisdb.provision`
- [x] 2.4 `scripts/deploy-server.sh`: `python -m app.backup` → `python -m elisdb.backup`
- [x] 2.5 `scripts/sync_smoke.py`: imports/comandos `app.*` → `elisdb.*`
- [x] 2.6 `.github/workflows/server-ci.yml:90`: `import app.*` → `import elisdb.*`
- [x] 2.7 `elisdb-server/app` docstrings/usage em `keys.py` (linhas ~4, 5, 97, 102)

## 3. Documentação

- [x] 3.1 `elisdb-server/docs/*.md` (`backup`, `seguranca`, `upgrade-2.0.0`, `nginx-proxy-manager`, `execucao`, `uso`, `configuracao`)
- [x] 3.2 `elisdb-server/README.md` e `README.md` (raiz)
- [x] 3.3 `.opencode/skills/project-context/SKILL.md` e `references/commands.md`
- [x] 3.4 Não alterar caminhos do contentor (`/app/data`, `/app/photos`, `WORKDIR /app`)

## 4. Limpeza

- [x] 4.1 Apagar `elisdb-server/uv.lock` (stub inválido)
- [x] 4.2 Verificar que `pyproject.toml` não precisa de alterações (`pythonpath` já inclui `.`)

## 5. Verificação

- [x] 5.1 `python -m pytest` verde em `elisdb-server/`
- [ ] 5.2 `docker build` conclui e `import elisdb.main` funciona na imagem
- [x] 5.3 `grep -rn "app\\." elisdb-server` sem referências ao pacote antigo (exceto `/app/...`)
- [x] 5.4 `openspec validate server-package-rename`
