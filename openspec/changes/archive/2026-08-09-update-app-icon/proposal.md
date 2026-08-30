## Why

A app usa atualmente ícones de launcher genéricos (foreground/background vectoriais de exemplo, fundo verde `#3DDC84`) que não representam a marca. Existe um conjunto completo de ícones produzido (`/home/ricardo/Transferências/AppIcons`) com ícones densidade-específicos e foreground adaptável, pronto para ser usado.

## What Changes

- Substituir os ícones de launcher legados (`ic_launcher.webp`, `ic_launcher_round.webp`) por `ic_launcher.png` nas cinco densidades fornecidas.
- Substituir o foreground adaptável (`drawable/ic_launcher_foreground.xml`) pelo resultado de `android/adaptive-foreground.png`.
- Substituir o background adaptável (`drawable/ic_launcher_background.xml`) por um vector de cor sólida correspondente ao fundo claro do ícone (`#F7F8FA`).
- Manter os dois recursos `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round` referenciados no `AndroidManifest.xml` (o round continua a apontar para o mesmo ícone).
- **BREAKING**: remover os ficheiros `.webp` antigos e os vectoriais de exemplo; os recursos `@drawable/ic_launcher_*` deixam de ser vetores (mas o manifest não muda).

## Capabilities

Esta mudança é apenas de assets (sem alteração de comportamento, API ou specs). Declarada `skip_specs: true` no `.openspec.yaml`.

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma (sem mudanças a nível de requisitos).

## Impact

- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` (novos, copiados).
- `app/src/main/res/drawable/ic_launcher_background.xml` e `ic_launcher_foreground.xml` (substituídos).
- `app/src/main/res/mipmap-{dpi}/*.webp` (removidos).
- `AndroidManifest.xml` — sem alteração.
- Fonte dos ícones: `/home/ricardo/Transferências/AppIcons/android`.