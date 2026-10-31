# Proposal

## Why

Hoje só há duas formas de tratar um cliente que já não está em serviço: manter tudo
igual (e a lista mistura clientes ativos com inativos) ou apagar o registo (perdendo
histórico, contratos e fotos). Falta um estado explícito que permita arquivar um
cliente sem o eliminar.

## What Changes

- Novo campo booleano `ativo` em cada cliente, com **default `true`** para que todos
  os registos existentes continuem ativos após a migração.
- Formulário de criação/edição passa a ter um `Switch` “Cliente ativo”.
- Detalhe do cliente mostra a etiqueta “Inativo” e a lista distingue visualmente os
  clientes inativos (sem toggle na lista).
- Servidor guarda a coluna `ativo`, valida-a e devolve-a no push/pull; o estado
  participa no last-write-wins como qualquer outro campo.
- Histórico de edições regista “Ativou o cliente” / “Desativou o cliente”.
- Migração Android **9→10** e `ALTER TABLE` idempotente no servidor.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-data`: o registo de cliente passa a incluir o estado ativo/inativo, com
  default ativo, e a criação/edição grava-o.
- `client-screens`: formulário com toggle, detalhe com etiqueta de inativo e lista
  com indicação visual do estado.
- `server-sync`: a coluna `ativo` é persistida, devolvida no push/pull e registada
  no histórico de edições.

## Impact

- Android: `Cliente` (Room), migração 9→10, `ClienteDraft`, repositório, DTOs do
  sync, formulário, detalhe, lista e histórico.
- Servidor: `models.py` (contrato), `database.py` (schema, migração, validação,
  histórico), `main.py` (push/pull).
- Sem alteração de autenticação, fotos ou formato de datas.
- Compatibilidade: clientes e servidores antigos não enviam o campo; ambos os lados
  assumem `true`, pelo que o deploy pode ser feito pela ordem habitual.
