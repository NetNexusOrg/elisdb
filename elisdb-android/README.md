# ElisDB — app Android

A app **ElisDB** é um caderno de clientes para o telemóvel: lista, formulário,
mapa e fotos, com sincronização com o servidor ElisDB quando está acessível.
Funciona offline — só contacta o servidor quando configurada (URL + chave).

## Início rápido (debug)

A partir de `elisdb-android/`:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release assinado (ver [Configuração](docs/configuracao.md) para as credenciais):

```bash
scripts/build-release.sh
```

O APK sai em `app/build/outputs/apk/release/app-release.apk`.

## Ligar ao servidor

Para sincronizar, importa o ficheiro de configuração no ecrã **Configuração**:
1. No servidor (pasta `elisdb-server/`):
   `docker compose exec elisdb-server python -m app.keys issue <nome> --server-url <URL>`
   (cria o `<nome>.config.json`).
2. Na app: menu (⋮) → **Configuração** → **Importar configuração do servidor** → escolhe o
   ficheiro e carrega em **Sincronizar** (a app também sincroniza ao abrir e ao
   guardar um cliente; não há sincronização periódica em segundo plano).

## Mapa e ligação à rede

Os dados dos clientes são offline-first: continuam acessíveis sem servidor nem
rede. O mapa usa *tiles* do OpenStreetMap através do osmdroid, por isso precisa
de rede (ou de *tiles* já em cache) para os desenhar; sem ligação podem faltar
zonas do mapa até os *tiles* serem carregados.

## Documentação

Instalação, execução, configuração e utilização — em português em [`docs/`](docs/index.md).

O servidor tem a sua própria documentação em `../elisdb-server/docs/`.