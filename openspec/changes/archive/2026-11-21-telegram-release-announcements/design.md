# Design

## Context

Ver `proposal.md` — Why. Factos que moldam o desenho:

- `elisdb-telegram-bot/` só tem `CHANGELOG.md` e `.gitignore`; o token e o id do
  grupo vivem em `Telegram-Bot-Info.txt`, ignorado pelo Git.
- O job `release` já extrai as notas para `$RUNNER_TEMP/release-notes.md` e já
  tem o APK em `elisdb-android/app/build/outputs/apk/release/$APK_NAME`.
- O APK da v1.12.0 tem 47 MB; o Bot API público aceita 50 MB por upload de bot.
- O repositório é privado e `/api/app/download` exige autenticação, por isso não
  existe link público de fallback para o APK.

## Goals / Non-Goals

**Goals:**

- Anúncio automático, no grupo, com APK e notas, no fim de cada release por tag.
- Falhas do Telegram visíveis mas incapazes de estragar uma release já publicada.

**Non-Goals:**

- Bot interativo (comandos, webhook, polling de releases).
- Alojar um Bot API local para ficheiros acima de 50 MB.
- Reduzir o tamanho do APK (fica como change separada).

## Decisions

### Passo no job `release`, não serviço persistente

O `android-ci.yml` chama o script no fim do job `release`, depois de criar o
Release e de publicar no servidor.

- *Porque:* as notas e o APK já estão no runner; não há estado, polling, host
  extra, nem token GitHub para ler um repositório privado.
- *Alternativa rejeitada — workflow em `release: published`:* desacoplaria e
  cobriria releases manuais, mas acrescenta um terceiro workflow e obriga a
  descarregar o asset via API, duplicando o que o job já tem em disco.
- *Alternativa rejeitada — bot persistente no servidor:* permitiria comandos e
  retries, mas acrescenta um serviço, estado e mais superfície de operação para
  um anúncio que é, na prática, um efeito de um evento.

### Script Python stdlib, não `python-telegram-bot`

`urllib.request` + multipart construído à mão.

- *Porque:* duas chamadas HTTP (`sendDocument`, `sendMessage`) não justificam uma
  dependência, e o projeto evita bibliotecas novas sem necessidade. O runner traz
  `python3`, logo não é preciso `setup-python`.
- *Alternativa rejeitada — `curl` no YAML:* simples, mas sem testes unitários da
  formatação das notas nem da divisão em mensagens.

### HTML do Telegram, não Markdown

`parse_mode=HTML`, com `**bold**` → `<b>`, `### Título` → `<b>Título</b>`, `- ` → `• `
e escape de `<`, `>`, `&`.

- *Porque:* o Markdown do Telegram é frágil com o `**` do changelog e com
  caracteres soltos; HTML só precisa de escape. As notas são escritas em
  português com acentos, que passam sem alteração.

### Guarda de tamanho com degradação, não falha

Limite efetivo `50 MiB − 1 MiB` (margem para o envelope multipart e a legenda).
Acima disso o script envia só o anúncio e emite `::warning::`.

- *Porque:* a release já está publicada; ficar sem APK no grupo é melhor do que
  ficar sem anúncio, e o aviso torna o problema visível.

### Passo não bloqueante

`continue-on-error: true` no passo.

- *Porque:* o Release e a cópia para o servidor já aconteceram. Marcar a run como
  falhada não desfaz nada e esconde o que realmente importa (a release saiu).

## Risks / Trade-offs

- [Re-run do job `release` duplica a mensagem] → Aceite. Se passar a incomodar,
  acrescenta-se um marcador de idempotência (ex.: guardar o tag anunciado).
- [APK a crescer para perto dos 50 MB] → A guarda evita falhas; reduzir o APK
  (R8 full mode, resource shrinking) ou alojar Bot API local fica para uma change
  própria.
- [Bot fora do grupo ou sem permissão de envio] → A API devolve erro explícito
  (`chat not found`, `not enough rights`), visível como aviso na run.
- [Token em ficheiro local em texto simples] → Já era a decisão aceite em
  `telegram-integration`; no CI vive apenas em GitHub Secrets.
