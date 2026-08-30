## 1. Remover a confiança de certificado customizado

- [x] 1.1 Em `SettingsStore.kt`, remover `caCertPem` de `ServerSettings`, os métodos `caCertPem()`/`saveCaCert()` e a chave `KEY_CA_CERT`
- [x] 1.2 Em `ApiClient.kt`, remover o parâmetro `caCertPem` de `create`, o `buildTrustingSslSocketFactory()` e os imports SSL/X.509 agora não usados
- [x] 1.3 Em `ApiClientProvider.kt` e `AppContainer.kt`, deixar de propagar `caCertPem` (remover da assinatura de cache)
- [x] 1.4 Em `SettingsViewModel.kt`, remover `hasCaCert`, `importCaCert()`, `clearCaCert()` e o campo `message` associado
- [x] 1.5 Em `SettingsScreen.kt`, remover o cartão "Certificado do servidor (HTTPS auto-hospedado)", o launcher de importação de certificado e o bloco `state.message`

## 2. Permitir HTTP para qualquer host

- [x] 2.1 Em `ServerConfigDto.kt`, alterar `isAllowedServerUrl` para aceitar `http` e `https` para qualquer host e rejeitar outros esquemas
- [x] 2.2 Em `network_security_config.xml`, permitir cleartext para todos os hosts (base-config)

## 3. Testes

- [x] 3.1 Atualizar `FakeSettingsStore` (unit e androidTest) removendo `caCertPem`/`saveCaCert`
- [x] 3.2 Ajustar/adicionar testes de importação para `http` com domínio/IP aceite e esquema não suportado rejeitado
- [x] 3.3 Remover quaisquer testes/asserções que cubram o cartão de certificado na UI

## 4. Verificação

- [x] 4.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 4.2 Confirmar testes instrumentados de settings (`SettingsScreenTest`, `SettingsStoreTest`, `SettingsViewModelTest`) no emulador
- [x] 4.3 Smoke-test manual: importar configuração com `http://<domínio>` e `http://<ip>` funciona; o ecrã de definições já não mostra o cartão de certificado
