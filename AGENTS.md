# AGENTS.md — Regras do projeto ElisDB

Regras transversais que orientam o desenvolvimento assistido por IA neste
repositório. O contexto técnico específico está nas skills em `.opencode/skills/`
(começar por `project-context`); este ficheiro define as regras gerais.

## O que é este projeto

ElisDB — sistema de gestão de clientes **offline-first**: app Android (Kotlin +
Jetpack Compose) + servidor de sync (Python + FastAPI + SQLite). Monorepo:
`elisdb-android/`, `elisdb-server/`, `elisdb-memento/`, `openspec/`.

## Regras gerais

- Analisa o código e a arquitetura existentes **antes** de alterar qualquer coisa.
- Faz alterações mínimas e focadas; não refactorizar código não relacionado.
- Preserva a arquitetura existente (MVVM + StateFlow + Repository + DI manual no
  Android; FastAPI + SQLite no servidor).
- Não inventa componentes ou tecnologias que não existem no projeto.
- Não atualiza dependências nem versões (Gradle, AGP, Kotlin, SDK, libs) sem pedido explícito.
- Testa as alterações (`android-testing`, `api-backend`).
- Verifica `git diff` antes de qualquer commit.

## Git

- Commits pequenos e focados com Conventional Commits (`feat:`, `fix:`, `ci:`, `docs:`, `chore:`).
- **Nunca fazer `git commit` nem `git push` sem confirmação explícita do utilizador** — pergunta sempre antes.
- **Exceção:** ao arquivar uma change via `/opsx-archive`, faz `git commit` + `git push` automaticamente no final (sem pedir confirmação), com Conventional Commits e após verificar que não há secrets/dados.
- Não fazer force push, apagar branches/tags, nem `git reset --hard` sem confirmação explícita.
- Verificar que não há secrets/dados em `git status` antes de commit (ver Security).

## Security

Nunca fazer commit de:
- passwords, API keys, tokens, private keys, certificados privados;
- Android keystores (`.jks`, `.keystore`), `keystore.properties`, signing credentials;
- `.env` com secrets;
- `*.config.json` (contêm `private_seed` ed25519) e `*.db` (dados reais).

Ver `.opencode/skills/security-review/`.

## Android

- Seguir a arquitetura existente (MVVM + StateFlow + Repository, DI manual `AppContainer`).
- Não introduzir bibliotecas novas sem necessidade.
- Não alterar Gradle/SDK/dependencies sem justificação (ver `gradle-build`).

## Database

- Nunca executar operações destrutivas (DROP, DELETE massivo, ALTER destrutivo,
  reset de base de dados) sem confirmação explícita. Ver `.opencode/skills/database/`.

## OpenSpec

- Alterações significativas (novas features, API, database, auth, deployment,
  arquitetura) devem começar por uma OpenSpec change. Correções triviais podem ser
  feitas diretamente. Ver `.opencode/skills/openspec/`.

## Production

- Nunca fazer deployment para produção, criar releases, ou alterar produção sem
  confirmação explícita. Ver `.opencode/skills/android-release/` e `github-workflow`.
