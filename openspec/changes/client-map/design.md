# Design

## Fluxo

1. `ClientListScreen` mostra um `IconButton` de mapa na `TopAppBar`.
2. A navegação abre `ClientMap`.
3. `ClientMapViewModel` observa `repository.clientes` e separa os clientes com
   coordenadas válidas dos restantes.
4. `ClientMapScreen` desenha um `OsmMapMarkers` com um pin por cliente.
5. Tocar num pin seleciona o cliente; o cartão inferior permite abrir o detalhe.
6. Tocar no mapa ou no botão de fechar limpa a seleção.

## Dados

`ClientMapItem` contém `id`, `nome`, `contrato`, `ativo`, `latitude` e
`longitude`. Coordenadas nulas ou fora dos intervalos válidos são ignoradas e
contadas em `semLocalizacao`. A lista é ordenada por nome.

## Mapa

`OsmMapMarkers` reutiliza o ciclo de vida do `MapView` já usado por `OsmMap`.
Mantém os markers entre recomposições, atualiza posição/cor quando a assinatura
dos pins muda e faz `zoomToBoundingBox` com margem. Com um único pin usa zoom 16.
Os botões de zoom do osmdroid continuam ocultos; o zoom é feito por pinça.

## Estados

- **Loading:** indicador de progresso.
- **Sem clientes:** “Ainda não há clientes.”
- **Sem coordenadas:** “Nenhum cliente tem localização definida.” e contador.
- **Normal:** mapa, legenda ativo/inativo e contador de sem localização quando
  aplicável.

## Fora de âmbito

- Clustering de pins.
- Filtros/pesquisa no mapa.
- Alteração de coordenadas diretamente no mapa.
