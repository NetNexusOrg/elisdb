## Context

O comando `python -m app.keys issue <nome>` (elisdb-server/app/keys.py, `main()`
linhas 72-85) imprime o seed com o label `private seed (import into app):` e regista
a chave na allowlist (linha 81). O servidor não conhece o seu próprio URL — só
`ELISDB_DB`/`ELISDB_PHOTOS` existem como env vars. Na app, `SettingsViewModel.kt`
importa a seed via `extractSeed`/`SEED_LABEL` (linhas 100-108/150) e grava
URL+chave juntos no `SettingsStore` (`save(baseUrl, signingKey)`, linhas 31-33) —
logo, gravar ambos a partir de um ficheiro reutiliza o store atual.

## Goals / Non-Goals

**Goals:**
- O comando `issue` emite um ficheiro de configuração único (URL + chave).
- A app importa só esse ficheiro e fica totalmente configurada (URL + chave) de uma
  só vez.
- Contrato de ficheiro explícito (JSON) em vez de extração por label de output.

**Non-Goals:**
- Não alterar o transporte de sync, o motor de sincronização nem a allowlist do
  servidor.
- Não alterar `SettingsStore`, a edição manual do URL (mantém-se) nem o ecrã de
  Definições para além dos labels/ajuda da importação.

## Decisions

**D1 — O comando `issue` passa a exigir o URL do servidor e a emitir o ficheiro.**
Nova flag `--server-url <url>` (fallback env `ELISDB_SERVER_URL`, obrigatório um
deles) e `--output <path>` (por omissão `<nome>.config.json`). O comando grava o
ficheiro, regista a chave na allowlist e imprime um resumo curto; o seed continua a
ser imprimido no terminal para referência do operador.
Alternativa considerada: adivinhar o URL a partir do pedido `Host` — rejeitada,
`issue` é um comando CLI fora do pedido HTTP e o servidor pode estar atrás de um
proxy.

**D2 — Formato do ficheiro de configuração: JSON.**
`{"server_url": "<url>", "private_seed": "<seed base64>"}`. JSON é inequívoco para
validar (ao contrário do parsing por label) e é um ficheiro real que o operador
transporta para a app.
Alternativa considerada: texto label a label (~output atual) — rejeitada, perpetua o
contrato frágil que esta mudança quer eliminar.

**D3 — A app importa o ficheiro de configuração no lugar do output do comando.**
`SettingsViewModel.importKey` passa a analisar o JSON, validar o seed existente
(`isValidSeed`) e o URL não vazio, e grava ambos em `settingsStore.save(...)`; depois
aplicar+`syncNow()`, como hoje. `extractSeed`/`SEED_LABEL` são removidos.
Alternativa considerada: manter a tolerância ao seed "nu"/output antigo — rejeitada,
o servidor deixa de reproduzir esse contrato e a proposta simplifica para "importar
apenas o ficheiro de configuração".

**D4 — Labels e ajuda das Definições atualizados.**
O texto "Importe o ficheiro com a seed emitida pelo servidor..." passa a referir o
ficheiro de configuração `.config.json` e o comando `python -m app.keys issue
--server-url <url> <nome>`; o campo URL continua visível/editável (pré-preenchido
pelo import).

**D5 — Backwards compatibility.**
Dispositivos já configurados mantêm os valores no `SettingsStore`; a mudança só
afeta novos imports. A seed mintada ("não atribuída") existente continua a funcionar
porque a allowlist e o `Signer` não mudam.

## Risks / Trade-offs

- [Operadores habituados ao output da seed] → A seed continua impressa no terminal
  como referência; o onboarding passa a usar o ficheiro.
- [Ficheiro antigo de importação deixa de funcionar] → Comportamento pretendido
  (importar só o ficheiro de configuração); documentado na migração.
- [URL desconhecido pelo servidor] → Resolvido com a flag/env obrigatória em D1.