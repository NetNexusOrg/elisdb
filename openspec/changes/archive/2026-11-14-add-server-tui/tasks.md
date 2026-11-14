# Tasks — add-server-tui

## 1. Estado (`elisdb/status.py`)

- [x] 1.1 Criar `collect(data_dir, db_path, configs_dir)` com modo, caminhos,
      dono/uid, gravabilidade, tamanhos, contagens e chaves ativas
- [x] 1.2 Ler contagens por SQL read-only (`file:...?mode=ro`), sem migrar
- [x] 1.3 Detetar modo ativo (contentor via `docker inspect`, pid local)
- [x] 1.4 Testes unitários com BD temporária e diretórios falsos

## 2. Chaves (`elisdb/keys.py`)

- [x] 2.1 `list --json` (id, comentário, criada, revogada, provisioned)
- [x] 2.2 `revoke --id` (revoga uma chave, preserva a linha)
- [x] 2.3 Manter `revoke <user>` e o menu interativo atuais
- [x] 2.4 Testes das duas flags e dos erros (id inválido, já revogada)

## 3. `.env` (`start.py`)

- [x] 3.1 Parser que preserva comentários e chaves desconhecidas
- [x] 3.2 Escrita atómica, 0600, `.env.bak-<ts>`
- [x] 3.3 Validação por campo (inteiros, URL, utilizadores, PUID/PGID, data dir)
- [x] 3.4 Testes de round-trip e de cada regra de validação

## 4. Arranque e paragem (`start.py`)

- [x] 4.1 Modo local: subprocesso uvicorn com env traduzido, pid em `.run/`
- [x] 4.2 Modo Docker: `docker compose up -d` / `stop`
- [x] 4.3 Guardas de exclusão mútua e de uid vs `PUID`
- [x] 4.4 Verificação de saúde após arranque

## 5. Menu

- [x] 5.1 Status
- [x] 5.2 Iniciar (local / Docker) e Parar
- [x] 5.3 Utilizadores (listar, adicionar, revogar, detalhe)
- [x] 5.4 Configuração (ver, editar campos, aplicar)
- [x] 5.5 Flags `--add-user`, `--list-users`, `--revoke`

## 6. Documentação

- [x] 6.1 `docs/gestao-tui.md` (operação, paridade local/Docker, recuperação)
- [x] 6.2 Atualizar `docs/execucao.md` e `README.md` para apontar à TUI

## 7. Verificação

- [x] 7.1 `pytest` verde
- [x] 7.2 `openspec validate add-server-tui`
- [x] 7.3 Teste manual: Status, arranque local, paragem, Docker, adicionar e
      revogar um utilizador de teste, editar e aplicar um limite
