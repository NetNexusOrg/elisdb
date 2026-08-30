## Why

O marcador de localização nos mapas usa o ícone por defeito da osmdroid, que não agrada. Pretende-se um pino vermelho simples e comum (tipo os pins clássicos), mantendo o comportamento do marcador (posição, arrasto, fixação às coordenadas) inalterado.

## What Changes

- Substituir o ícone por defeito do `Marker` da osmdroid por um pino vermelho simples (vector drawable), aplicado a todos os mapas da app (detalhe, viewer fullscreen, picker e preview do formulário).
- Manter o anchor do marcador (`CENTER`/`BOTTOM`) para que a ponta do pino fique sobre as coordenadas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A especificação `client-location` já exige "a marker SHALL be displayed at the client's coordinates"; a mudança só altera a aparência do ícone, sem mudança de comportamento. Declarada com `skip_specs: true`.

## Impact

- `app/src/main/res/drawable/ic_map_pin.xml`: novo vector drawable com o pino vermelho simples.
- `app/src/main/java/com/example/elisdb/ui/map/OsmMap.kt`: definir `marker.icon` a partir do novo drawable.
- Sem novas dependências; o resto do comportamento do marcador mantém-se.
