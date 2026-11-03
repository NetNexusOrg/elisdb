# Design

## Contexto

A pasta `elisdb-telegram-bot/` guarda o ficheiro local `Telegram-Bot-Info.txt`. O requisito é manter o segredo dentro do projeto, mas impedir que seja versionado, sem bloquear o código futuro do bot.

## Decisão

Adicionar um `.gitignore` dentro de `elisdb-telegram-bot/` com:

- `*.txt`, com exceção de `requirements.txt`;
- `*.session`;
- `.env`.

O ficheiro de token continua na pasta, mas fica invisível para o Git. O código futuro pode ser adicionado e versionado normalmente; o token deve ser lido do ficheiro local ignorado ou de uma variável de ambiente.

## Riscos

- Se o ficheiro de segredo for renomeado para uma extensão não coberta, passa a poder ser commitado. Manter a extensão `.txt` ou adicionar a nova extensão ao `.gitignore`.
- Se o token já tiver sido alguma vez commitado/pushado, deve ser revogado no BotFather e substituído.
