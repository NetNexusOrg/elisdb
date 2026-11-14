# Proposal — add-server-tui

## Why

Gerir o servidor exige hoje conhecer `docker compose`, variáveis de ambiente e
três comandos Python diferentes. Operações correntes (ver estado, arrancar,
emitir ou revogar uma chave, mudar um limite) obrigam a decorar flags e a editar
o `.env` à mão, sem validação nem rede de segurança. Isso torna a operação
propensa a erros — e um erro no `.env` pode apontar o servidor para outra base
de dados.

Uma TUI simples em stdlib, a correr no host, resolve isto: um único ponto de
entrada, com as mesmas regras em modo local e Docker, e um editor de
configuração que valida antes de gravar.

## What Changes

- Novo `elisdb-server/start.py`: menu numerado (sem dependências) com
  **Status**, **Iniciar** (local ou Docker), **Parar**, **Utilizadores**,
  **Configuração** e **Sair**.
- Novo `elisdb/status.py`: recolhe modo ativo, caminhos resolvidos, dono/uid,
  tamanhos, contagens de clientes/fotos/tombstones e chaves ativas.
- `elisdb/keys.py`: `list --json` e `revoke --id` (revogação por chave, não por
  comentário), mantendo os comandos atuais.
- Flags não interativas para automação: `--add-user`, `--list-users`,
  `--revoke`.
- Editor de `.env`: valida campo a campo, preserva comentários, grava
  atomicamente com backup e pergunta “aplicar agora?” antes de reiniciar.
- Guardas: uid do host tem de coincidir com `PUID`; nunca arrancar local com o
  contentor a correr (nem o inverso); nunca imprimir seeds; nunca usar `sudo`.

## Capabilities

### New Capabilities

- `server-management-tui`: estado, arranque/paragem, gestão de utilizadores e
  configuração através de um único menu no host.

### Modified Capabilities

Nenhuma. As capacidades existentes (`server-deployment`,
`server-key-provisioning`) descrevem o comportamento do servidor; a TUI é uma
camada de operação sobre elas.

## Impact

- Código: `elisdb-server/start.py` (novo), `elisdb/status.py` (novo),
  `elisdb/keys.py` (flags novas), `elisdb-server/docs/gestao-tui.md` (novo).
- Sem alterações ao servidor, API, esquema ou dados.
- Sem novas dependências Python.
