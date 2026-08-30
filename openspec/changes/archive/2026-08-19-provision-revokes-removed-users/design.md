## Context

Ver proposal.md - Why para a motivação e specs para o comportamento exigido. Estado atual: `provision.py::provision_users` só emite chaves para utilizadores sem chave (match por `comment`) e nunca revoga; `keys` table tem `id, public_key, comment, created_at`; tanto o CLI manual como o provisioning passam por `keys.issue_and_register`, que chama `db.add_key(..., comment=user)` sem qualquer marcador de origem.

## Goals / Non-Goals

**Goals:**
- Revogar chaves auto-provisionadas de utilizadores retirados de `ELISDB_USERS`, no arranque.
- Distinguir chaves provisionadas de chaves manuais, sem alterar o comportamento manual.

**Non-Goals:**
- Não mexer no comportamento de emissão manual via CLI.
- Não remover chaves manuais, mesmo que o `comment` não conste na lista.
- Não alterar a autenticação/verificação de assinaturas nem o formato dos configs.

## Decisions

**D1: Marcar origem com coluna `provisioned` na tabela `keys` (INTEGER NOT NULL DEFAULT 0) + migração idempotente**
`db.add_key` ganha `provisioned: bool = False`. O CLI manual mantém o default `False`; o provisioning passa `True`. A migração `_migrate_keys_provisioned` adiciona a coluna a bases existentes (padrão já usado para `comment`/`created_at`). `list_keys` passa a devolver o campo.
- *Alternativa*: distinguir por prefixo de `key_id` (ex. `prov-` vs `issue-`) — rejeitado, mistura semântica com identificador e obriga a inferir origem por convenção de string.

**D2: Revogação por `comment` restrita a chaves `provisioned=1`**
No `provision_users`, após emitir os que faltam, iterar `db.list_keys()`: para cada chave `provisioned` cujo `comment` não esteja no conjunto desejado, `db.remove_key(id)` e apagar `configs_dir/<comment>.config.json` (best-effort). Chaves manuais (`provisioned=0`) são ignoradas.
- *Racional*: é o `comment` (nome de utilizador) que liga a chave ao `ELISDB_USERS`; restringir a `provisioned=1` evita revogar chaves manuais de um utilizador que saiu da lista.

**D3: `provision_users` passa a devolver `(provisioned, revoked)`**
O `main()` imprime `revoked key for '<user>'` além do já existente. Atualiza os testes existentes que desempacotam o retorno.
- *Alternativa*: devolver só `provisioned` e revogar como efeito colateral — rejeitado, impede reportar/verificar revogações de forma limpa.

## Risks / Trade-offs

- [Um utilizador com chave manual e chave provisionada no mesmo `comment`] → só a provisionada é revogada; a manual permanece. Comportamento correto.
- [Apagar o `.config.json` ao revogar pode falhar se o ficheiro foi movido] → remoção best-effort (`FileNotFoundError` ignorado); a revogação da chave não depende do ficheiro.
- [Revogação inesperada se um operador renomear o utilizador no `ELISDB_USERS`] → é o comportamento pretendido: a lista é a fonte de verdade.

## Migration Plan

- A coluna `provisioned` é adicionada por migração idempotente; chaves pré-existentes ficam com `provisioned=0` (tratadas como manuais — não são revogadas). Deploy: rebuild da imagem + `docker compose up`; o entrypoint corre o provisioning no arranque.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.