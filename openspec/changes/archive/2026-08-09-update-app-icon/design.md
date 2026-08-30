## Context

See proposal.md. Estado atual:

- Launcher legado: `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp` e `ic_launcher_round.webp`.
- Adaptativo (API 26+): `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` apontam para `@drawable/ic_launcher_background` (vector de exemplo, fundo `#3DDC84`) e `@drawable/ic_launcher_foreground` (vector de exemplo).
- Manifest referencia `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round`.

Assets fornecidos em `/home/ricardo/Transferências/AppIcons/android`:
- `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` — 48/72/96/144/192 px (densidades coincidem com os `.webp` atuais).
- `adaptive-foreground.png` — 1024×1024, conteúdo dentro da zona segura do ícone adaptável (alpha bbox 174..850).
- `appstore.png` (1024) e `playstore.png` (512) — destinados à Play Store, fora do APK.

## Goals / Non-Goals

**Goals:**
- Copiar e ligar os novos ícones em `res/` sem alterar o manifest nem a API.
- Garantir que API <26 (legado) e API ≥26 (adaptativo) mostram o novo ícone.

**Non-Goals:**
- Alterar `AndroidManifest.xml` (os nomes de recurso mantêm-se).
- Acrescentar `appstore.png`/`playstore.png` ao APK (são para a Play Store).
- Alterar cores de tema, comportamento ou specs.

## Decisions

**D1 — Copiar os PNG densidade-específicos como nome `ic_launcher.png` e usar o mesmo ficheiro para `ic_launcher_round`.** O recurso round continua a existir e aponta para o mesmo bitmap (Android também o exibe mascarado). Remover os `.webp` para evitar recursos duplicados com o mesmo nome base.

**D2 — Foreground adaptável via bitmap (não vector).** Copiar `adaptive-foreground.png` para `drawable-nodpi/ic_launcher_foreground.png` e apontar `mipmap-anydpi-v26/*.xml` para ele. O conteúdo já está dentro da zona segura (bbox 174..850), pelo que não é preciso reencadrar.
- *Alternativa:* gerar `drawable-mdpi/…` em várias densidades — o formato `nodpi` com 1024px é simples e o sistema escala; aceitável para o ícone de launcher.

**D3 — Background adaptável como vector de cor sólida `#F7F8FA`** (o fundo claro do ícone no `appstore.png`), substituindo `ic_launcher_background.xml`. Mantém-se o `monochrome` a apontar para o foreground.
- *Alternativa:* usar o `appstore.png` em `drawable-nodpi` como background — rejeitado por não ser um PNG com layout de background adaptável (contém o logótipo completo já com fundo; serviria como ícone estático, não como camada background).

**D4 — Manter o `ic_launcher_background.xml` e `ic_launcher_foreground.xml` como nomes de recurso válidos** para não quebrar referências, apenas mudando o conteúdo (background) e/ou substituindo o ficheiro (foreground).

## Risks / Trade-offs

- **Regressão no API <26**: os launchers antigos dependem dos PNGs legados. D1 cobre todas as densidades usadas, sem lacunas.
- **Monochrome/teematic**: `monochrome` ficará com o bitmap colorido; em Android 13 o sistema pode usar o alpha. Aceitável; risco baixo.
- **Fundo claro fixo no ícone**: o tema da app está em dark mode em `values-night`; o launcher usa o fundo do ícone independentemente do tema, pelo que um fundo claro é consistente com o `appstore`. Nenhum themeable icon envolvido.
- **Assinatura**: os ficheiros são binários copiados tal-qual; sem alteração de controlo de versão além dos novos assets.

## Migration Plan

- Substituir assets em `app/src/main/res`; remover `.webp`; compilar (`./gradlew assembleDebug`) para validar.
- Sem alterações de DB, rede, ou specs. Rollback: restaurar os ficheiros antigos do git (a change é só assets).

## Open Questions

- Nenhuma.