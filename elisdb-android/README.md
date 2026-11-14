# ElisDB — app Android

Caderno de clientes para o telemóvel: lista, ficha, mapa e fotos, com
sincronização com o servidor ElisDB quando está configurado. Funciona offline.

## Início rápido

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release assinado (precisa de credenciais — ver
[Assinatura](../docs/desenvolver/assinatura.md)):

```bash
scripts/build-release.sh
# → app/build/outputs/apk/release/app-release.apk
```

## Ligar ao servidor

No servidor, emite `configs/<nome>.config.json`; na app, menu ⋮ →
**Configuração** → **Importar credencial do utilizador**. Passo a passo em
[Ligar um telemóvel](../docs/usar/ligar-telemovel.md).

## Mapa

Tiles vetoriais do OpenStreetMap via OpenFreeMap (estilo Liberty) com MapLibre
Native. Sem rede, só aparecem zonas já em cache; os dados dos clientes continuam
disponíveis.

## Documentação

- [Desenvolver a app](../docs/desenvolver/app.md) — SDK, build, emulador, testes.
- [Usar a app](../docs/usar/app.md) · [Problemas](../docs/operar/problemas.md).
- Índice completo: [`../docs/index.md`](../docs/index.md).
