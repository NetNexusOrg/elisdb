# ElisDB

Sistema de gestão de clientes **offline-first** para Android: uma app no
telemóvel que cria e edita clientes, tira fotos e marca localizações — e um
servidor que guarda os dados e mantém tudo sincronizado entre dispositivos.

Funciona mesmo sem servidor; quando o servidor está acessível, os dados
sincronizam automaticamente.

## De que é feito o projeto

| Diretório       | O que é                                     | Como começar                                             |
|-----------------|---------------------------------------------|----------------------------------------------------------|
| `elisdb-android/` | A app para o telemóvel (Kotlin + Compose)   | [Documentação da app](elisdb-android/docs/index.md)      |
| `elisdb-server/`  | O servidor de dados (Python + FastAPI)      | [Documentação do servidor](elisdb-server/docs/index.md)  |
| `openspec/`       | Especificações e histórico de mudanças      | _(apenas para quem desenvolve o projeto)_                |

## Como usar (visão geral)

1. **Instala e corre o servidor** — guia em [`elisdb-server/docs/`](elisdb-server/docs/index.md).
2. **Emite a chave de um telemóvel** no servidor:
   ```bash
   python -m app.keys issue <nome> --server-url <URL-do-servidor>
   ```
3. **Importa** o `<nome>.config.json` nas **Definições** da app e sincroniza.

Tudo isto está explicado passo a passo na [utilização da app](elisdb-android/docs/uso.md).

## Documentação

- **App Android:** [`elisdb-android/README.md`](elisdb-android/README.md) e guias em [`elisdb-android/docs/`](elisdb-android/docs/index.md).
- **Servidor:** [`elisdb-server/README.md`](elisdb-server/README.md) e guias em [`elisdb-server/docs/`](elisdb-server/docs/index.md).

## Desenvolvimento

O desenvolvimento é assistido por IA (OpenCode + OpenSpec) e gerido por Git/GitHub.

- **Regras do projeto:** [`AGENTS.md`](AGENTS.md) (regras gerais) e skills em `.opencode/skills/` (contexto técnico por área).
- **Especificações:** alterações significativas começam por uma OpenSpec change (`openspec/`); o contexto do projeto está em [`openspec/config.yaml`](openspec/config.yaml).
- **Build e testes:**
  ```bash
  cd elisdb-android && ./gradlew :app:testDebugUnitTest   # testes unitários
  cd elisdb-server && .venv/bin/python -m pytest           # testes do servidor
  ```
- **Release:** o CI (`.github/workflows/android-release.yml`) constrói o APK
  assinado e cria a GitHub Release ao fazer push de uma tag `vX.Y.Z`.
