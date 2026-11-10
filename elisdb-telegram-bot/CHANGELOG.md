# Histórico de versões — ElisDB Telegram Bot

Componente do bot Telegram que apoia a operação do ElisDB.

## [Unreleased]

### Notas

- Changelog inicial do componente bot Telegram. As alterações ao bot passam a
  ser registadas aqui.
- **Anúncio de releases.** `publish_release.py` envia o APK assinado e as notas
  da versão para o grupo, chamado pelo job `release` do `android-ci.yml`.
- **Testes do bot.** `tests/test_publish_release.py` corre no job `Server tests`.
