# Spec Delta

## ADDED Requirements

### Requirement: Credenciais do bot ficam locais

A integração Telegram SHALL manter o token do bot e os identificadores do grupo em ficheiros locais dentro de `elisdb-telegram-bot/`, ignorados pelo Git. As credenciais SHALL NOT ser commitadas, embutidas no código ou movidas para fora da pasta do projeto.

#### Scenario: Ficheiro de segredo ignorado
- **WHEN** existe um ficheiro `*.txt` com credenciais em `elisdb-telegram-bot/`
- **THEN** o Git SHALL ignorá-lo
- **AND** `git status` SHALL NOT listá-lo

#### Scenario: Código futuro continua versionável
- **WHEN** forem adicionados ficheiros de código ou `requirements.txt`
- **THEN** o Git SHALL poder acompanhá-los
- **AND** os ficheiros de credenciais locais SHALL continuar ignorados

#### Scenario: Token não hard-coded
- **WHEN** o bot for implementado
- **THEN** SHALL ler o token de um ficheiro local ignorado ou variável de ambiente
- **AND** o repositório SHALL NOT conter um token hard-coded
