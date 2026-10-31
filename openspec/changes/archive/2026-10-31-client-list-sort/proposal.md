# Proposal

## Why

A lista de clientes só tem uma ordem fixa (alfabética). Quando o utilizador quer
retomar um cliente que acabou de criar, tem de o procurar na lista inteira, e não
tem forma de ver primeiro os mais recentes.

## What Changes

- O menu do ícone de filtro passa a ter, além do filtro por estado, três opções
  de ordenação: **ordem alfabética** (predefinição), **mais recentes primeiro**
  (pela data de criação) e **última edição primeiro** (pela data de modificação).
- A ordenação é aplicada depois do filtro e da pesquisa, sem alterar os dados
  guardados.
- A escolha é só de sessão (não persiste entre arranques).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-screens`: a lista de clientes passa a suportar ordenação alfabética ou
  por data de criação descendente.

## Impact

- `ui/cliente/ClientListViewModel.kt` e `ClientListScreen.kt`.
- Testes unitários e instrumentados da lista.
- Sem alteração de base de dados, servidor ou sincronização.
