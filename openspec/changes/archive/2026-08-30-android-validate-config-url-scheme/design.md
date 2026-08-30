## Context

`parseServerConfig` (`settings/ServerConfigDto.kt:22-31`) não valida o esquema do `server_url`. Um `http://` para host remoto passa na importação mas falha depois em runtime (NSC bloqueia cleartext exceto localhost). O erro só aparece na sync.

## Goals / Non-Goals

**Goals:**
- Validar esquema/host na importação: `https` aceite; `http` só para localhost; rejeitar o resto.

**Non-Goals:**
- Não mudar o destino do armazenamento; apenas a validação.
- Não relaxar o NSC (o http local continua permitido).

## Decisions

**D1: Validação no DTO/import**
`parseServerConfig` valida o `server_url` após fazer parse: esquema `https` → OK; esquema `http` e host em {localhost, 127.0.0.1, 10.0.2.2} → OK; caso contrário → lançar/retornar erro de validação. A UI recusa com mensagem clara.

## Risks / Trade-offs
- [Bloqueia http para LAN não-local] → é intencional; o utilizador deve usar https (ver `android-lan-tls-support`) ou importar o CA. Consistente com o NSC.

## Migration Plan
- Sem migração. Configs já importados com http remoto continuam a falhar em runtime como hoje (não pior).

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
