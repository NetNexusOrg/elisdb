# Proposal

## Why

O ficheiro `Telegram-Bot-Info.txt` contém o token do bot e informação do grupo. Está dentro do projeto, mas a pasta não está ignorada pelo Git; um `git add .` poderia commitá-lo acidentalmente.

## What Changes

- Adicionar `elisdb-telegram-bot/.gitignore` com regras locais para ficheiros de credenciais e estado (`*.txt` exceto `requirements.txt`, `*.session` e `.env`).
- Manter o segredo dentro da pasta do projeto, sem criar ficheiros fora dela.
- Documentar que o código futuro deve ler o token de um ficheiro local ignorado ou de uma variável de ambiente.

## Capabilities

### New Capabilities

- `telegram-integration`

### Modified Capabilities

- Nenhuma.

## Impact

- `elisdb-telegram-bot/.gitignore`
- `openspec/specs/telegram-integration/spec.md`
- Sem alterações ao servidor, base de dados, sync ou app Android.
