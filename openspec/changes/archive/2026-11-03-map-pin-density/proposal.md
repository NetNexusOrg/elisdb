# Proposal

## Why

O clustering atual coloca o cluster no centróide dos clientes e desenha um círculo de raio fixo. Quando os clientes estão espalhados, o cluster pode aparecer numa zona onde nenhum cliente está e não cobre a localização real dos membros, pelo que um pin “desaparece” para uma área errada. O mapa deve preservar sempre a localização real de cada cliente.

## What Changes

- Remover o clustering do source de clientes e as layers de cluster/contagem.
- Adaptar o tamanho dos pins em função da densidade local no ecrã:
  - pins afastados ficam completos e com etiqueta;
  - pins próximos encolhem progressivamente;
  - abaixo de um limiar passam a uma bola pequena.
- Manter o cliente selecionado sempre com pin completo e etiqueta.
- Omitir etiquetas quando não há espaço, mantendo o pin/bola visível.
- Separar em leque, a partir do zoom 14, os clientes com coordenadas exatamente iguais.
- Atualizar a spec `client-map` para o novo comportamento e remover o requisito de atribuição visível, que já não corresponde ao produto.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-map`: substituir o agrupamento por clustering por pins adaptativos à densidade; ajustar o requisito de etiquetas; remover o requisito de atribuição visível.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/map/ElisMapSupport.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/map/ElisMapMarkers.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/cliente/ClientMapScreen.kt`
- `elisdb-android/app/src/main/res/drawable/ic_map_dot_active.xml`
- `elisdb-android/app/src/main/res/drawable/ic_map_dot_inactive.xml`
- `elisdb-android/app/src/test/java/com/example/elisdb/ui/map/ElisMapSupportTest.kt`
- Sem alterações ao servidor, base de dados, sync ou API.
