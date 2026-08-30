## 1. Legado (API < 26)

- [x] 1.1 Copiar `/home/ricardo/Transferências/AppIcons/android/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` → `app/src/main/res/mipmap-{dpi}/ic_launcher.png`.
- [x] 1.2 Copiar o mesmo `ic_launcher.png` de cada densidade para `app/src/main/res/mipmap-{dpi}/ic_launcher_round.png`.
- [x] 1.3 Remover `app/src/main/res/mipmap-{dpi}/ic_launcher.webp` e `ic_launcher_round.webp`.

## 2. Adaptativo (API ≥ 26)

- [x] 2.1 Copiar `android/adaptive-foreground.png` → `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`.
- [x] 2.2 Remover o vector antigo `app/src/main/res/drawable/ic_launcher_foreground.xml`.
- [x] 2.3 Substituir `app/src/main/res/drawable/ic_launcher_background.xml` por um vector de cor sólida `#F7F8FA` (mantendo `108dp` × `108dp`/viewport 108).

## 3. Verificação

- [x] 3.1 Confirmar que `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` continuam a referenciar `@drawable/ic_launcher_background` e `@drawable/ic_launcher_foreground` (nomes preservados).
- [x] 3.2 Confirmar que `AndroidManifest.xml` mantém `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round` (sem alteração).
- [x] 3.3 Compilar `./gradlew assembleDebug` e resolver erros de resource relacionados.
- [x] 3.4 Verificar que não ficou nenhum `.webp` nem vector antigo por apagar.