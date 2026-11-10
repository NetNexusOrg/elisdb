# Proposal

## Why

Cada release do APK é publicada no GitHub e copiada para o servidor, mas o grupo
Telegram da comunidade só descobre a versão nova se alguém a anunciar à mão. As
notas já existem em `elisdb-android/CHANGELOG.md` (Novidades/Correções) e o job
`release` do `android-ci.yml` já as extrai — falta entregá-las, com o APK, ao
grupo no momento em que o release é criado.

## What Changes

- Novo script `elisdb-telegram-bot/publish_release.py` (stdlib apenas) que envia
  para o grupo o APK assinado como documento e as notas da versão em HTML do
  Telegram, com guarda para o limite de 50 MB do Bot API.
- Novo passo `Announce release in Telegram` no fim do job `release` do
  `android-ci.yml`, não bloqueante, alimentado por `TELEGRAM_BOT_TOKEN` e
  `TELEGRAM_CHAT_ID` (GitHub Secrets).
- Testes unitários do script em `elisdb-telegram-bot/tests/`, corridos no job
  `Server tests` (mesmo padrão dos testes do memento).
- Documentação do componente (README, CHANGELOG) e da pipeline
  (`.opencode/skills/github-actions`).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `telegram-integration`: passa a cobrir o anúncio automático de releases no
  grupo — entrega do APK, formato das notas, credenciais via secrets de CI e
  comportamento quando o ficheiro excede o limite do Bot API.

## Impact

- `elisdb-telegram-bot/publish_release.py`, `elisdb-telegram-bot/tests/`,
  `elisdb-telegram-bot/README.md`, `elisdb-telegram-bot/CHANGELOG.md`
- `.github/workflows/android-ci.yml` (novo passo no job `release`)
- `.github/workflows/server-ci.yml` (paths e passo de testes do bot)
- `.opencode/skills/github-actions/SKILL.md`
- Novos GitHub Secrets: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`
- Sem alterações à app Android, ao servidor, à base de dados ou ao sync.
