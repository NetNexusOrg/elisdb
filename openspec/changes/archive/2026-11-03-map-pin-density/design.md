# Design

## Context

Ver `proposal.md` — Why. O source de clientes era um GeoJSON com clustering do MapLibre; os pins usavam uma única SymbolLayer com etiquetas sempre ativas. O mapa já tem seleção de cliente, localização atual e filtros.

## Goals / Non-Goals

**Goals:**
- Manter cada cliente na coordenada real.
- Reduzir sobreposição sem esconder clientes.
- Manter o cliente selecionado legível e com etiqueta.
- Manter o comportamento dos filtros, câmara e localização atual.

**Non-Goals:**
- Alterar o estilo do mapa, a atribuição ou a navegação.
- Implementar um algoritmo global de otimização de colisões.
- Garantir separação visual de coordenadas quase iguais abaixo do zoom 14.

## Decisions

### Remover clustering e usar densidade em screen space

O clustering do MapLibre é calculado no centróide e não representa a extensão geográfica. Em vez disso, o source passa a ter uma feature por cliente. No `onCameraIdle`, as coordenadas são projetadas para o ecrã e cada pin recebe uma escala com base na distância ao vizinho mais próximo e no número de vizinhos dentro de 34 dp. Alternativa rejeitada: convex hull dos clusters, que continuaria a esconder pins individuais e exigiria layers adicionais.

### Duas SymbolLayers: pins e bolas

A layer de pins usa `iconSize` e `iconImage` data-driven e ancora no fundo; a layer de bolas usa a mesma expressão de tamanho mas ancora ao centro. A separação permite âncoras corretas e evita etiquetas nas bolas. Alternativa rejeitada: uma única layer com âncora variável, que o MapLibre não suporta de forma data-driven de forma simples.

### Escala e limiares

`pinScale` combina a distância ao vizinho mais próximo com a contagem de vizinhos. A escala mínima é `0.36`; abaixo de `0.55` o pin passa a bola. As etiquetas aparecem apenas com escala `>= 0.85` ou no cliente selecionado. O cliente selecionado é forçado a tamanho `1.0` e etiqueta.

### Coordenadas exatamente iguais

A partir do zoom 14, grupos com a mesma coordenada são distribuídos em leque num raio de `10 dp + 2 dp` por elemento adicional. Abaixo do zoom 14 não há leque, porque o deslocamento geográfico seria excessivo; nesse caso os marcadores podem sobrepor-se.

### Atualização no camera idle

O recálculo ocorre quando a câmara estabiliza, não em cada frame, para evitar jitter e custo. O listener é criado por `DisposableEffect` e removido ao sair. A seleção também dispara recálculo.

## Risks / Trade-offs

- [Coordenadas quase iguais abaixo do zoom 14] → O marcador continua na coordenada real; ao aproximar, o pin cresce e o leque separa os duplicados exatos.
- [Custo O(n²) do cálculo] → Aceitável para o volume atual de clientes; pode ser otimizado com grelha espacial se necessário.
- [Etiquetas omitidas] → O pin continua visível e selecionável; a etiqueta aparece no cliente selecionado.
- [Atualização apenas no idle] → Durante o gesto os tamanhos ficam estáveis; após o gesto são recalculados.
- [Spec de atribuição removida] → A ausência de atribuição é uma decisão de produto explícita; a spec passa a refleti-la.

## Migration Plan

Não há migração de dados. A alteração é reversível por revert do commit. O clustering pode ser reintroduzido removendo as layers de bolas e voltando a ativar `GeoJsonOptions.withCluster`.
