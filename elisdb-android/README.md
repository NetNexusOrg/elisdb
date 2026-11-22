# ElisDB — app Android

Caderno de clientes para o telemóvel: lista com pesquisa e filtros, ficha, mapa,
fotos com escolha da foto de perfil, histórico por cliente e página global de
alterações. Sincroniza com o servidor ElisDB; depois de configurada, funciona
offline.

## Funcionalidades

- Clientes com contrato, notas, tags e estado ativo/inativo.
- Pesquisa, filtros por estado e tags, e ordenação.
- Foto de perfil escolhida entre as fotos do cliente.
- Mapa com pins, filtros, localização atual e navegação.
- Histórico por cliente e **Logs** global.
- App offline-first, com sincronização entre dispositivos e atualizações.

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

No servidor, emite `configs/<nome>.config.json`; na app, menu ≡ →
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
