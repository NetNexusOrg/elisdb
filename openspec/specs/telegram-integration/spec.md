# Telegram integration

## Purpose

Integrar o ElisDB com um bot Telegram sem expor credenciais no repositório.

## Requirements

### Requirement: Credenciais do bot ficam locais

A integração Telegram SHALL manter o token do bot e os identificadores do grupo em ficheiros locais dentro de `elisdb-telegram-bot/`, ignorados pelo Git. As credenciais SHALL NOT ser commitadas, embutidas no código ou movidas para fora da pasta do projeto.

#### Scenario: Ficheiro de segredo ignorado

- **WHEN** existe um ficheiro `*.txt` com credenciais em `elisdb-telegram-bot/`
- **THEN** o Git SHALL ignorá-lo
- **AND** `git status` SHALL NOT listá-lo

#### Scenario: Código futuro continua versionável

- **WHEN** forem adicionados código do bot ou `requirements.txt` à pasta
- **THEN** esses ficheiros SHALL poder ser versionados
- **AND** os ficheiros locais de credenciais SHALL continuar ignorados

#### Scenario: Token não hard-coded

- **WHEN** o bot for iniciado
- **THEN** SHALL ler o token de um ficheiro local ignorado ou de uma variável de ambiente
- **AND** o repositório SHALL NOT conter o token em código

### Requirement: Release announcement in the group

When a GitHub Release is created from a `v*` tag, the bot SHALL post the version and the release notes to the configured Telegram group.

#### Scenario: Tag release is announced

- **WHEN** the release job publishes a GitHub Release for tag `v1.13.0`
- **THEN** the bot SHALL post a message identifying `ElisDB v1.13.0` in the group
- **AND** the message SHALL contain the changelog sections `Novidades` and `Correções` for that version

#### Scenario: Non-tag push stays silent

- **WHEN** a commit is pushed to main without a version tag
- **THEN** the bot SHALL NOT post anything to the group

#### Scenario: Message respects Telegram limits

- **WHEN** the release notes exceed the Telegram message size
- **THEN** the bot SHALL split them into consecutive messages that each fit the limit
- **AND** no message SHALL be truncated mid-line

### Requirement: APK delivered with the announcement

The bot SHALL send the signed release APK to the group as a document, unless the file exceeds the Telegram Bot API upload limit.

#### Scenario: APK fits the limit

- **WHEN** the release APK is smaller than the Bot API upload limit
- **THEN** the bot SHALL upload it as a document named after the release asset
- **AND** the document caption SHALL identify the app version

#### Scenario: APK exceeds the limit

- **WHEN** the release APK is at or above the Bot API upload limit
- **THEN** the bot SHALL still post the announcement
- **AND** the workflow SHALL record a warning that the APK was not attached

### Requirement: Announcement credentials come from CI secrets

The bot SHALL read the bot token and the target group from the CI environment, never from version-controlled files.

#### Scenario: Missing secrets skip the announcement

- **WHEN** the release job runs without the Telegram secrets configured
- **THEN** the announcement step SHALL be skipped with a warning
- **AND** the release SHALL still be published

### Requirement: Announcement failures do not block the release

A failure while posting to Telegram SHALL NOT revert, delete or mark as failed an already published release.

#### Scenario: Telegram API error after publication

- **WHEN** the GitHub Release and the server publication succeeded and the Telegram call fails
- **THEN** the release SHALL remain published
- **AND** the workflow SHALL surface the failure as a warning
