## Context

O cliente Android tem hoje duas coisas relacionadas a TLS que se removem/simplificam (ver proposal.md - Why):

- **Confiança customizada**: `ApiClient.create(caCertPem)` constrói um `SSLSocketFactory` que junta as CAs do sistema com um certificado importado pelo utilizador (`SettingsStore.caCertPem`). A UI das definições tem um cartão dedicado e o `SettingsViewModel` importa/limpa o PEM. O `ApiClientProvider` e o `AppContainer` propagam o valor, e a assinatura de cache do cliente inclui o `caCertPem`.
- **Validação de URL**: `ServerConfigDto.isAllowedServerUrl` só aceita `http` para hosts locais (`localhost`, `127.0.0.1`, `10.0.2.2`) e `https` para qualquer host; o resto é rejeitado. A `network_security_config.xml` só permite cleartext para esses mesmos hosts locais.

## Goals / Non-Goals

**Goals:**
- Eliminar por completo a importação/confiança de certificado HTTPS auto-hospedado (UI, estado, persistência, trust manager).
- Permitir alcançar o servidor por HTTP simples, usando domínio ou endereço IP.

**Non-Goals:**
- Não alterar a emissão/validação de certificados no servidor (a app passa a validar apenas contra o sistema).
- Não alterar o onboarding por ficheiro de configuração (mantém-se `server_url` + `private_seed`).
- Não adicionar confiança de CA embutida ou pinning.

## Decisions

- **Remover o trust manager customizado em vez de o manter inerte.** `ApiClient.create` volta a não receber `caCertPem`; sem `sslFactory`, o Ktor/Android usa o `TrustManager` do sistema. Alternativa rejeitada: manter o parâmetro com valor sempre vazio — deixaria código morto e superfície de erro.

- **Eliminar o estado de certificado do `SettingsStore`.** Remove-se `caCertPem` de `ServerSettings`, os métodos `caCertPem()`/`saveCaCert()` e a chave persistida `KEY_CA_CERT`. Como o certificado nunca foi lido fora da confiança TLS, não há migração: a chave órfã em prefs é simplesmente ignorada.

- **Aceitar `http`/`https` para qualquer host na importação.** `isAllowedServerUrl` passa a aceitar os dois esquemas para qualquer host e a rejeitar qualquer outro esquema. A mensagem de erro na importação mantém-se, agora só para conteúdo inválido ou esquema não suportado.

- **Permitir cleartext globalmente na `network_security_config.xml`.** Substitui-se o `domain-config` (só local) por um `base-config cleartextTrafficPermitted="true"`. Alternativa considerada: `android:usesCleartextTraffic="true"` no manifest — rejeitada para manter o mecanismo no ficheiro de configuração de rede, que já existe e é o ponto único de política de rede.

## Risks / Trade-offs

- [Cleartext global enfraquece a privacidade do transporte] → É exatamente o pretendido pelo utilizador (HTTP simples para domínio/IP); a confiança fica limitada a domínio/IP escolhidos pelo utilizador na configuração. A autenticação das requests continua assinada (ed25519), independente do transporte.
- [Certificado órfão persistido] → A chave `ca_cert_pem_override` fica em prefs sem uso; inofensivo, não é migrada nem lida.
- [Regressão na validação de URL] → Mantém-se a rejeição de esquemas não suportados; testes de importação são atualizados para `http` de domínio/IP.

## Migration Plan

Sem migração. Remoção localizada; rollback trivial (reverter as alterações de código e do ficheiro de configuração de rede).

## Open Questions

Nenhuma.
