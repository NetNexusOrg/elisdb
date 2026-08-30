## Why

`network_security_config.xml` permite cleartext apenas para `10.0.2.2`/`localhost`/`127.0.0.1`, e o `ApiClient` não faz pinning. O caso de uso documentado é auto-hospedado em LAN (servidor num Raspberry/NAS). Um dispositivo real a aceder a um servidor HTTPS LAN com certificado self-signed é rejeitado pelo trust manager do sistema, e HTTP para qualquer outro host é bloqueado pelo NSC. Ou seja, HTTPS self-signed em LAN está bloqueado — um bloqueio de deployment para o utilizador alvo.

## What Changes

- Suportar um CA fornecido pelo utilizador / pinning configurável (ex.: NSC `trust-anchors` com um certificado importado nas definições, ou pinning por host).
- O utilizador importa o certificado (ou CA) do seu servidor auto-hospedado nas definições; a app confia nesse CA para o host configurado.

## Capabilities

### New Capabilities
- `client-tls`: nova capability — a app SHALL permitir que o utilizador configure um certificado/CA de confiança para o servidor (self-signed LAN), para que HTTPS auto-hospedado funcione sem recorrer a cleartext.

### Modified Capabilities

## Impact
- Android: `network_security_config.xml` (trust-anchors configurável), `settings` (import de CA), `ApiClient`/OkHttp (TrustManager com o certificado do utilizador). Sem alteração de protocolo de assinatura.
- Testes: testar que um servidor HTTPS self-signed com o CA importado é aceite; sem CA continua a exigir confiança válida.
