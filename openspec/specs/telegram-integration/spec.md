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
