# Proposal

## Why

Guardar um cliente deixa alterações pendentes que só chegam ao servidor quando o
utilizador se lembra de abrir a Configuração e tocar em “Sincronizar”. Esse passo
extra é fácil de esquecer e deixa dados por enviar sem qualquer sinal no ecrã onde
foram criados.

## What Changes

- Guardar um cliente (criação ou edição) passa a disparar uma sincronização em
  background, sem bloquear a navegação de volta.
- A sincronização continua a ser **acionada pelo utilizador** — nunca por
  agendamento ou temporizador. O que muda é que guardar conta como um gatilho.
- Falhas de sync não são mostradas no formulário (que já fechou): ficam visíveis
  no estado de sincronização do ecrã principal, como hoje.
- Vários guardares seguidos juntam-se ao sync em curso (single-flight já
  existente); não há filas nem pedidos duplicados.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-sync`: o requisito “Sync is manual only” passa a incluir guardar um
  cliente como gatilho de sincronização, mantendo a proibição de sincronização
  automática por tempo.

## Impact

- Android: `ClientFormViewModel.save` dispara `repository.syncNow()` depois de
  gravar, em `viewModelScope`, sem esperar pelo resultado.
- Sem alteração no servidor, no protocolo, na base de dados ou nas permissões.
- O comportamento offline mantém-se: sem rede, o cliente fica pendente e o estado
  de erro é o já existente.
