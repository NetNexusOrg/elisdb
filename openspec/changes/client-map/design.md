# Design

## Fluxo

1. `ClientListScreen` mostra um `IconButton` de mapa na `TopAppBar`.
2. A navegação abre `ClientMap`.
3. `ClientMapViewModel` observa `repository.clientes` e o filtro
   `ClientMapFilter` (predefinição `ACTIVE`), separando os clientes com
   coordenadas válidas dos restantes.
4. `ClientMapScreen` desenha um `OsmMapMarkers` com um pin por cliente visível.
5. O menu (≡) da topbar permite mudar o filtro; a lista da página inicial não é
   afetada.
6. Tocar num pin seleciona o cliente; o cartão inferior permite abrir o detalhe.
7. Tocar no mapa ou no botão de fechar limpa a seleção.

## Dados

`ClientMapItem` contém `id`, `nome`, `contrato`, `ativo`, `latitude` e
`longitude`. Coordenadas nulas ou fora dos intervalos válidos são ignoradas e
contadas em `semLocalizacao`; `totalClientes` mantém o total para distinguir os
estados vazios. `ClientMapFilter` tem `ALL`, `ACTIVE` (predefinição) e
`INACTIVE`. A lista é ordenada por nome.

## Mapa

`OsmMapMarkers` reutiliza o ciclo de vida do `MapView` já usado por `OsmMap`.
Mantém os markers entre recomposições, atualiza posição/cor quando a assinatura
dos pins muda e faz `zoomToBoundingBox` com margem. Com um único pin usa zoom 16.
Os botões de zoom do osmdroid continuam ocultos; o zoom é feito por pinça.

## Estados

- **Loading:** indicador de progresso.
- **Sem clientes:** “Ainda não há clientes.”
- **Sem coordenadas:** “Nenhum cliente tem localização definida.”
- **Filtro sem resultados:** “Nenhum cliente corresponde ao filtro.”
- **Normal:** mapa com os pins do filtro escolhido e cartão de seleção.

## Fora de âmbito

- Clustering de pins.
- Pesquisa textual no mapa.
- Alteração de coordenadas diretamente no mapa.
