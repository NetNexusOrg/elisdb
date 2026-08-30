## Context

`network_security_config.xml` permite cleartext só para `10.0.2.2`/localhost; `ApiClient` não faz pinning. O caso de uso é auto-hospedado em LAN com HTTPS self-signed, que o sistema rejeita. Bloqueio de deployment para o utilizador alvo.

## Goals / Non-Goals

**Goals:**
- Permitir CA/certificado de confiança configurável por utilizador para o host do servidor.
- Manter validação normal quando não há CA configurado.

**Non-Goals:**
- Não relaxar TLS globalmente (apenas para o host configurado).
- Não implementar pinning estrito obrigatório (o utilizador opta por importar o CA).

## Decisions

**D1: Trust-anchor por host nas definições**
Adicionar às definições a importação de um certificado (PEM/DER). A app constrói um `TrustManager` que inclui o certificado do utilizador para o host configurado, via `network_security_config.xml` com `trust-anchors` (base64 do certificado) ou via OkHttp `SSLContext` personalizado.
- *Alternativa*: pinning por hash — mais forte mas frágil se o cert renovar; CA/configurável é mais flexível para LAN.

**D2: Escopo do anchor**
O anchor aplica-se apenas ao host do `server_url` configurado; outros hosts usam o trust store do sistema.

## Risks / Trade-offs
- [Utilizador importa CA errado] → a ligação falha; é o esperado, sem comprometer outros hosts.
- [Complexidade de NSC dinâmico] → NSC com `trust-anchors` em base64 ou `certificates` em `res/raw` funciona em runtime; documentar.

## Migration Plan
- Sem migração de dados. Nova definição de import de CA.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
