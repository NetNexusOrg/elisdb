## 1. Componente de linha de informação

- [x] 1.1 Extrair `ServerInfoRow(label, value, color?)` privado em `SettingsScreen.kt` com label (`labelLarge`, `onSurfaceVariant`) e valor (`bodyLarge`, `onSurface`/`onSurfaceVariant`), `fillMaxWidth`, espaçamento label→valor fixo (4.dp)
- [x] 1.2 Substituir as três linhas URL, chave e utilizador pelo `ServerInfoRow`, garantindo o mesmo mapeamento de cores/estado atual (utilizador: Resolving/Resolved/Unavailable/Unresolved)

## 2. Cartões e agrupamento

- [x] 2.1 Envolver a secção "Configuração do servidor" num `Card` com `padding(16.dp)` e espaçamento vertical entre campos (12.dp), mantendo o `titleMedium` como título do cartão
- [x] 2.2 Ajustar o `SettingsScreen` para usar espaçamento entre grupos (barra de sync, cartão de config, ações) de 16–20.dp, substituindo o `spacedBy(16.dp)` uniforme; confirmar que erros (`signingKeyError`) e avisos (`syncNotice`/`message`) usam `bodySmall`/`bodyMedium` coerentes

## 3. Verificação

- [x] 3.1 Correr `./gradlew :app:testDebugUnitTest` e garantir que os testes existentes passam (sem mudanças de lógica)
- [x] 3.2 Instalar a app no emulador e confirmar visualmente: tamanhos de fonte consistentes entre labels e entre valores; campos agrupados; estados com/sem chave e de identidade legíveis
- [x] 3.3 Confirmar que a funcionalidade (sync manual, importar/remover chave) continua a funcionar no ecrã