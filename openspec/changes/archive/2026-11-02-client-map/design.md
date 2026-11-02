# Design

## Fluxo

1. `ClientListScreen` mostra um `IconButton` de mapa na `TopAppBar`.
2. A navegação abre `ClientMap`.
3. `ClientMapViewModel` observa `repository.clientes` e o filtro
   `ClientMapFilter` (predefinição `ACTIVE`), separando os clientes com
   coordenadas válidas dos restantes.
4. O ecrã pede acesso à localização, se necessário, e o `ClientMapViewModel`
   obtém a posição atual através do `LocationService`.
5. `ClientMapScreen` desenha um `OsmMapMarkers` com um pin por cliente visível,
   identificado pelo nome do cliente por cima, e um marcador azul na posição
   atual.
6. O menu (≡) da topbar permite mudar o filtro; a lista da página inicial não é
   afetada.
7. Tocar num pin seleciona o cliente; o cartão inferior permite abrir o detalhe.
8. Tocar no mapa ou no botão de fechar limpa a seleção.

## Dados

`ClientMapItem` contém `id`, `nome`, `contrato`, `ativo`, `latitude` e
`longitude`. Coordenadas nulas ou fora dos intervalos válidos são ignoradas e
contadas em `semLocalizacao`; `totalClientes` mantém o total para distinguir os
estados vazios. `ClientMapFilter` tem `ALL`, `ACTIVE` (predefinição) e
`INACTIVE`. A lista é ordenada por nome. `ClientMapViewModel.myLocation` contém
a localização atual devolvida pelo `LocationService`; é nula sem permissão ou
sem posição disponível.

## Mapa

`OsmMapMarkers` reutiliza o ciclo de vida do `MapView` já usado por `OsmMap`.
Mantém os markers entre recomposições, atualiza posição/cor/nome quando a
assinatura dos pins muda e desenha o nome do cliente num fundo claro acima do
pin. Quando há localização atual, centra o mapa nela com zoom 15 e desenha um
marcador azul; não volta a enquadrar os pins após o utilizador mover o mapa.
Sem localização, faz `zoomToBoundingBox` com margem; com um único pin usa zoom
16. Os botões de zoom do osmdroid continuam ocultos; o zoom é feito por pinça.

## Detalhe em ecrã inteiro

O mapa do detalhe reutiliza `OsmMap`, continua centrado no cliente e mostra a
localização atual com um ponto azul, sem recentrar quando a posição chega. Se a
localização estiver fora do ecrã inicial, o utilizador vê-a ao afastar o zoom.
Sem permissão ou localização, o mapa mantém apenas o pin do cliente.

## Estados

- **Loading:** indicador de progresso.
- **Sem clientes e sem localização:** “Ainda não há clientes.”
- **Sem coordenadas e sem localização:** “Nenhum cliente tem localização
  definida.”
- **Filtro sem resultados:** o mapa continua visível, centrado na localização
  atual quando existir, com “Nenhum cliente corresponde ao filtro.”
- **Normal:** mapa com os pins do filtro escolhido, marcador de localização e
  cartão de seleção.

## Fora de âmbito

- Clustering de pins.
- Pesquisa textual no mapa.
- Alteração de coordenadas diretamente no mapa.
