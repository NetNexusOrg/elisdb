# Proposal

## Why

A lista de clientes é boa para procurar um cliente, mas não dá uma visão
geográfica da carteira. Quem trabalha no terreno precisa de perceber rapidamente
onde estão os clientes e abrir o detalhe de um ponto no mapa.

## What Changes

- Adiciona um ecrã **Mapa de clientes**, aberto pelo ícone de mapa na barra
  superior da lista.
- Mostra **todos** os clientes com coordenadas, independentemente da pesquisa
  ou filtro ativos na lista.
- Pins vermelhos para clientes ativos e cinzas para inativos.
- Tocar num pin abre um cartão com nome, contrato e estado; o botão
  **Abrir cliente** navega para o detalhe.
- Clientes sem coordenadas são contados num aviso discreto no mapa.

## Capabilities

### New Capabilities

- `client-map`: visão geográfica de todos os clientes com pins, seleção e
  contagem de clientes sem localização.

### Modified Capabilities

Nenhuma.

## Impact

- `ui/cliente/ClientMapScreen.kt`, `ui/cliente/ClientMapViewModel.kt`
- `ui/map/OsmMapMarkers.kt`, `ui/map/OsmMap.kt`
- `ui/cliente/ClientListScreen.kt`, `Navigation.kt`, `NavigationKeys.kt`
- `res/drawable/ic_map_pin_inactive.xml`
- Testes unitários e instrumentados; documentação de uso.
