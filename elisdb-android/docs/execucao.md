# Executar a app — construir e instalar no telemóvel

Com a preparação feita (ver [Instalação](instalacao.md)), aqui está como
construir a app e instalá-la no telemóvel ou emulador.

> Sem servidor configurado, a app continua a funcionar offline — apenas a
> sincronização fica desativada até a configurares (ver [Utilização](uso.md)).

## Construir a versão de desenvolvimento (debug)

A partir de `elisdb-android/`:

```bash
./gradlew :app:assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

Esta versão é a mais rápida de construir e instalar — usa a keystore de
desenvolvimento do SDK. Serve para testar.

## Instalar no telemóvel ou emulador

**No emulador** (com o emulador a correr) ou **no telemóvel** (ligado por USB,
com depuração USB ativada):

```bash
./gradlew :app:installDebug
```

Ou, à mão, com `adb`:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Depois abre a app **ElisDB** a partir do menu de aplicações do telemóvel.

## Construir a versão final assinada (release)

A versão final é compactada (R8) e **assinada** com as credenciais do projeto
(ver [Configuração](configuracao.md)). Sem essas credenciais, o build falha de
propósito — para não surgir um APK "final" não assinado:

```bash
./gradlew :app:assembleRelease
```

Resultado: `app/build/outputs/apk/release/app-release.apk`.

### Maneira mais simples: script de release

O script `scripts/build-release.sh` lê as credenciais do ficheiro
`~/.keystores/elisdb-release.env` (ou o ficheiro em `ELISDB_KEYSTORE_ENV`) e
corre o `assembleRelease` por ti:

```bash
scripts/build-release.sh
```

O ficheiro de credenciais tem de conter, no formato `NOME=valor`:

```text
KEYSTORE_PATH=/caminho/para/elisdb-release.keystore
KEYSTORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

## Emulador

Com o [Android CLI](https://developer.android.com/tools/android-cli-reference)
(instalado à mão, sem Android Studio):

1. Cria um dispositivo virtual (AVD) a partir de um perfil:
   ```bash
   android emulator create medium_phone
   ```
2. Lança o emulador (o comando bloqueia até o dispositivo estar pronto):
   ```bash
   android emulator start medium_phone
   ```
3. Instala a app como acima (`installDebug` ou `adb install`).

Outras opções: o *Device Manager* do Android Studio (ou `avdmanager`) e
`emulator -avd <nome-do-avd>`. Para parar: `android emulator stop <nome>`.

## Testar com o telemóvel físico e o servidor

Para sincronizar, o servidor tem de ser alcançável a partir do telemóvel: usa o
**IP da máquina do servidor**, nunca `127.0.0.1`. Veja o fluxo completo em
[Utilização](uso.md).