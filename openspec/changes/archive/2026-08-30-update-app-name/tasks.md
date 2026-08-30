## 1. Alterar o nome visível da app

- [x] 1.1 Alterar `app_name` em `elisdb-android/app/src/main/res/values/strings.xml` de `elisdb` para `ElisDB`
- [x] 1.2 Verificar que não há outras referências ao nome visível da app que devam ser atualizadas (o package name `com.example.elisdb` e diretórios permanecem inalterados)
- [x] 1.3 Correr `./gradlew :app:lintDebug` em `elisdb-android/` para confirmar que nada parte
