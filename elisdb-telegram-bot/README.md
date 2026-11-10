# ElisDB Telegram bot

Anuncia cada release do APK no grupo Telegram da comunidade: envia o ficheiro
assinado como documento e as notas da versão (secções **Novidades** e
**Correções** do `elisdb-android/CHANGELOG.md`).

O script é chamado pelo passo `Announce release in Telegram` do job `release`
em `.github/workflows/android-ci.yml`, depois de o GitHub Release e a cópia para
o servidor existirem.

## Ficheiros

| Ficheiro | Papel |
|---|---|
| `publish_release.py` | Envia o APK e as notas via Bot API (só stdlib). |
| `tests/test_publish_release.py` | Testes unitários, sem rede. |
| `Telegram-Bot-Info.txt` | Token e id do grupo **locais**, ignorados pelo Git. |
| `CHANGELOG.md` | Histórico de versões deste componente. |

## Credenciais

O script lê o token e o destino do ambiente, nunca de ficheiros versionados:

- `TELEGRAM_BOT_TOKEN` — token do bot (`@BotFather`).
- `TELEGRAM_CHAT_ID` — id do grupo (supergroup: começa por `-100`).

No CI vivem em GitHub Secrets:

```bash
gh secret set TELEGRAM_BOT_TOKEN --repo NetNexusOrg/elisdb
gh secret set TELEGRAM_CHAT_ID --repo NetNexusOrg/elisdb --body "-100…"
```

Sem os dois secrets, o passo é ignorado com um aviso e a release continua.

## Uso local

```bash
cd elisdb-telegram-bot

# Ver o que seria enviado, sem chamar a API (não precisa de token):
python3 publish_release.py --version 1.13.0 \
  --notes-file ../elisdb-android/CHANGELOG.md --dry-run

# Enviar de verdade (as notas têm de ser a secção da versão, sem o cabeçalho):
TELEGRAM_BOT_TOKEN=... TELEGRAM_CHAT_ID=... \
  python3 publish_release.py --version 1.13.0 \
  --notes-file /tmp/release-notes.md \
  --apk ../elisdb-android/app/build/outputs/apk/release/elisdb-1.13.0-20.apk
```

O `--notes-file` espera o texto já extraído (o mesmo ficheiro que o CI gera em
`$RUNNER_TEMP/release-notes.md`); com o `CHANGELOG.md` completo o `--dry-run`
serve para ver o formato, mas o anúncio incluiria todas as versões.

## Limite de tamanho

O Bot API público aceita **50 MB** por ficheiro enviado por um bot. O script
mantém 1 MB de margem para o envelope multipart e a legenda; acima disso envia
só o anúncio e regista um aviso na run. O APK da v1.12.0 tem 47 MB, portanto a
margem é pequena — reduzir o APK ou alojar um Bot API local são follow-ups
separados.

## Testes

```bash
cd elisdb-telegram-bot
python3 -m pytest
```

Correm também no job `Server tests` do `server-ci.yml`.
