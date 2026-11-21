# Tasks

## 1. Script do bot

- [x] 1.1 Criar `elisdb-telegram-bot/publish_release.py` com `format_notes`, `build_announcement`, `split_message`, `api_call` e `main`.
- [x] 1.2 Enviar o APK como documento (`sendDocument`) com legenda por versão.
- [x] 1.3 Enviar as notas como mensagens HTML (`sendMessage`), divididas a 4096 caracteres.
- [x] 1.4 Guarda de tamanho: acima de `50 MiB − 1 MiB` enviar só o anúncio e avisar.
- [x] 1.5 Ler `TELEGRAM_BOT_TOKEN` / `TELEGRAM_CHAT_ID` do ambiente e suportar `--dry-run`.

## 2. Testes

- [x] 2.1 Criar `elisdb-telegram-bot/tests/test_publish_release.py` (formatação, escape, chunking, guarda de tamanho, multipart).
- [x] 2.2 Correr `python -m pytest` no componente e garantir tudo verde.

## 3. Pipeline

- [x] 3.1 Acrescentar o passo `Announce release in Telegram` ao job `release` do `android-ci.yml`.
- [x] 3.2 Acrescentar `elisdb-telegram-bot/**` aos filtros `paths` do `server-ci.yml`.
- [x] 3.3 Acrescentar o passo `Run telegram bot tests` ao job `Server tests`.
- [x] 3.4 Validar o YAML dos dois workflows.

## 4. Documentação

- [x] 4.1 Criar `elisdb-telegram-bot/README.md` (uso local, `--dry-run`, secrets, limite de 50 MB).
- [x] 4.2 Registar a alteração em `elisdb-telegram-bot/CHANGELOG.md` (`[Unreleased]`).
- [x] 4.3 Atualizar `.opencode/skills/github-actions/SKILL.md` (passo no release, secrets, limite).

## 5. OpenSpec

- [x] 5.1 `openspec validate telegram-release-announcements --strict`.

## Workflow follow-up

- Criar os secrets `TELEGRAM_BOT_TOKEN` e `TELEGRAM_CHAT_ID` no repositório.
- Testar o anúncio com `--dry-run` local e depois com uma tag real.
- Sincronizar o delta para `openspec/specs/telegram-integration/spec.md` e arquivar a change.
