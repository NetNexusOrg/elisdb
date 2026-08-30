## Why

A documentação atual descreve o projeto, mas não explica ao leitor como agir:
está escrita para técnicos (internals de stack, variáveis de ambiente, endpoints
HTTP) e não para quem quer simplesmente instalar, configurar e usar o ElisDB.
Um novo utilizador não consegue, a partir dos documentos, percorrer o fluxo
"correr o servidor → emitir o config → importar na app → sincronizar" sem já
saber o que são virtualenvs, chaves ed25519 ou uma allowlist.

## What Changes

Reescrita prática de toda a documentação (`README.md` raiz, `README.md` de cada
subprojeto e as guias em `docs/`), orientada a passos concretos:

- **Linguagem orientada ao utilizador**: menos jargão técnico (FastAPI, SQLite,
  ed25519, R8, allowlist); o leitor vê "o que faz" e "o que tem de clicar/correr",
  em vez de "como está implementado".
- **Fluxo principal explícito em primeiro lugar**: o caminho
  «correr o servidor → `python -m app.keys issue <nome> --server-url <url>` →
  transferir o `<nome>.config.json` → importar nas Definições → sincronizar»
  aparece no topo de cada guia, pronto a seguir.
- **Guias reorganizadas por objetivo prático**: o que instalar, como correr,
  como ligar a app, como resolver problemas comuns — mantendo a referência
  técnica (endpoints, variáveis de build) como secções de consulta e não como
  conteúdo de abertura.
- **"Resolução de problemas" reforçada**: tabelas de sintoma → causa → correção
  presentes nos guias de utilização de servidor e app, cobrindo os erros reais
  (401, relógio dessincronizado, servidor inacessível, config em falta).
- **Verificação da documentação durante a reescrita**: ler o estado atual do
  código (comandos `app.keys`, endpoints, variáveis de ambiente, passos de
  build) para garantir que cada exemplo documentado está correto e atualizado — a
  documentação tem de refletir o comportamento real do projeto.
- **Sem alteração de comportamento**: mudança exclusivamente de documentação.

## Capabilities

### New Capabilities

Nenhuma — mudança de documentação, sem alteração de comportamento do sistema.

### Modified Capabilities

Nenhuma — não há alterações de requisitos especlevel. O `.openspec.yaml` declara
`skip_specs: true`.

## Impact

- Ficheiros de documentação a reescrever:
  - Raiz: `README.md`.
  - Servidor: `elisdb-server/README.md`, `elisdb-server/docs/{index,instalacao,execucao,configuracao,uso}.md`.
  - App: `elisdb-android/README.md`, `elisdb-android/docs/{index,instalacao,execucao,configuracao,uso}.md`.
- Novo conteúdo quando ajudar a clareza prática (ex.: um guia único "ligar a app
  ao servidor" , diagramas de fluxo em ASCII).
- Sem alterações a código, API, dependências ou build.