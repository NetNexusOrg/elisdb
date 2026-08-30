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

Para sincronizar, importa o ficheiro de configuração nas **Definições**:
1. No servidor: `python -m app.keys issue <nome> --server-url <URL>` (cria o
   `<nome>.config.json`).
2. Na app: **Definições** → **Importar configuração do servidor** → escolhe o
   ficheiro e sincroniza.

## Documentação

Instalação, execução, configuração e utilização — em português em [`docs/`](docs/index.md).

O servidor tem a sua própria documentação em `../elisdb-server/docs/`.