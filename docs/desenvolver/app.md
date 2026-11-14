# Desenvolver a app — preparar, construir e instalar

A app é um projeto Android construído pela linha de comandos; o Android Studio é
opcional.

## O que precisas

- **JDK 17** (`java -version`).
- **Android SDK** com `platforms/android-37.0`, `build-tools/37.0.0` e
  `platform-tools`.

Sem Android Studio, o [Android CLI](https://developer.android.com/tools/android-cli-reference)
instala o SDK:

```bash
android sdk install platforms/android-37.0 build-tools/37.0.0 platform-tools
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```

Diz ao projeto onde está o SDK, criando `elisdb-android/local.properties`
(ignorado pelo git):

```properties
sdk.dir=/caminho/para/o/android-sdk
```

O Android Studio cria este ficheiro sozinho. Sem ele, o build falha a avisar que
não encontrou o SDK.

## Construir (debug)

```bash
cd elisdb-android
./gradlew :app:assembleDebug     # → app/build/outputs/apk/debug/app-debug.apk
```

O primeiro build descarrega as dependências do Gradle e pode demorar.

## Instalar

Com o telemóvel ligado por USB (depuração USB ativa) ou o emulador a correr:

```bash
./gradlew :app:installDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk   # alternativa
```

## Emulador

```bash
android emulator create medium_phone
android emulator start medium_phone    # bloqueia até estar pronto
android emulator stop medium_phone
```

Também podes usar o *Device Manager* do Android Studio ou
`emulator -avd <nome>`.

## Release assinada

O build de release aplica R8 e exige credenciais de assinatura — sem elas falha
de propósito, para não produzir um APK "final" não assinado. Ver
[Assinatura e configuração](assinatura.md).

```bash
./gradlew :app:assembleRelease     # → app/build/outputs/apk/release/app-release.apk
scripts/build-release.sh           # lê ~/.keystores/elisdb-release.env e faz o mesmo
```

## Testar

```bash
./gradlew :app:testDebugUnitTest              # unitários (JVM)
./gradlew :app:connectedDebugAndroidTest      # instrumentados (emulador/dispositivo)
```

O CI corre os dois num emulador (`.github/workflows/android-ci.yml`).

## Sincronizar com um servidor local

O telemóvel tem de alcançar a máquina do servidor: usa o **IP da rede local**
(por exemplo `http://192.168.1.10:8000`), nunca `127.0.0.1` — esse endereço é o
próprio telemóvel. O fluxo de credenciais está em
[Ligar um telemóvel](../usar/ligar-telemovel.md).
