# Instalação da app

A app é um projeto Android construído a partir da linha de comandos (o Android
Studio é opcional). Este guia prepara o computador; o próximo
([Execução](execucao.md)) mostra como construir e instalar a app no telemóvel.

## O que precisas de ter

- **Java 17 (JDK)** — o projeto compila com esta versão.
- **Android SDK** — com as plataformas de build e `build-tools` necessárias.
- (Opcional) **Android Studio** — mais fácil para editar código, gerir o
  emulador e instalar a app.

### Dizer ao projeto onde está o SDK

O projeto precisa de saber onde o Android SDK está instalado. Cria (ou edita) o
ficheiro `local.properties` na raiz de `elisdb-android/` (é ignorado pelo git,
nunca é partilhado):

```properties
sdk.dir=/caminho/para/o/android-sdk
```

> Se usares o Android Studio, ele costuma criar/povoar este ficheiro
> automaticamente. Se estiver em falta, o build falha a avisar que não encontrou
> o SDK.

## Passo a passo

1. Obtém o projeto (a app está em `elisdb-android/`, ao lado de
   `elisdb-server/`):
   ```bash
   git clone <URL-do-repositorio-do-projeto>
   cd elisdb/elisdb-android
   ```
2. Confirma a versão do Java: `java -version` (deve ser Java 17).
3. Garante o `local.properties` a apontar para o SDK (ver acima).
4. Corre o primeiro build de teste (debug):
   ```bash
   ./gradlew :app:assembleDebug
   ```
   Na primeira vez, o Gradle descarrega as dependências (pode demorar alguns
   minutos).

## Como saber se ficou tudo certo

No fim, deve existir o ficheiro `app/build/outputs/apk/debug/app-debug.apk`.
Existe? A preparação está concluída.

## Passo seguinte

[Execução](execucao.md) — construir e instalar a app no telemóvel ou emulador.