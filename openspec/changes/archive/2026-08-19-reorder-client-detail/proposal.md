## Why

No ecrã de detalhe do cliente, as notas aparecem entre o nome e o mapa; a leitura fica mais natural com o mapa logo a seguir ao cabeçalho (contrato + nome) e as notas depois do mapa, antes das fotos.

## What Changes

- Reordenar as secções do ecrã de detalhe do cliente para: contrato, nome, mapa (com ação "Navegar até aqui"), notas, fotos.
- Sem alterações de comportamento: os mesmos blocos, apenas em nova ordem. As notas continuam a aparecer apenas quando não vazias; o mapa e a navegação só aparecem quando há coordenadas.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-screens`: a requirement "Client detail screen" passa a especificar a ordem de apresentação das secções (contrato acima do nome; mapa após o nome; notas após o mapa; fotos no fim).

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailScreen.kt` — mover o bloco de notas para depois do bloco do mapa dentro de `ClientDetailContent`.