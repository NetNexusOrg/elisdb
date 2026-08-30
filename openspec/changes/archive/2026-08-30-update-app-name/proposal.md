## Why

A app é apresentada no launcher com o nome em minúsculas "elisdb", inconsistente com a grafia oficial do projeto "ElisDB" usada em toda a documentação. Uniformizar o nome visível da app com a marca.

## What Changes

- Alterar o valor da string `app_name` em `elisdb-android/app/src/main/res/values/strings.xml` de `elisdb` para `ElisDB`, o nome visível no launcher.

## Capabilities

### New Capabilities

_Nenhuma_ — mudança cosmética, sem alteração de requisitos ao nível de especificação.

### Modified Capabilities

_Nenhuma_ — nenhum requisito de especificação descreve atualmente o nome visível da app.

## Impact

- `elisdb-android/app/src/main/res/values/strings.xml` (string `app_name`, usada via `android:label` no `AndroidManifest.xml`).
- Sem impacto no package name (`com.example.elisdb`), diretórios, servidor, base de dados ou API.
