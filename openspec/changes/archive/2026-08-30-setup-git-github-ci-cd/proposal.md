## Why

O projeto (app Android + servidor) está funcional mas não tem controlo de versões nem automatização de builds: não existe repositório Git, os APKs de release são construídos manualmente (via `scripts/build-release.sh`) e não há qualquer pipeline de CI/CD. Isto torna os lançamentos manuais, irreprodutíveis e propensos a erro. Este change estabelece a base de versionamento e release automático sem alterar o comportamento da aplicação.

## What Changes

- Inicializar um repositório Git na raiz do projeto com um `.gitignore` adequado às tecnologias reais (Gradle/Android, Python/FastAPI, Node/OpenCode).
- Criar um repositório GitHub **PRIVATE** e configurar `origin` por SSH.
- Criar um workflow GitHub Actions (`.github/workflows/android-release.yml`) que executa testes, constrói o APK de release assinado e publica uma GitHub Release anexada ao APK quando uma tag `v*` é criada.
- Definir a estratégia de versionamento (SemVer `MAJOR.MINOR.PATCH`), alinhada com o `versionName`/`versionCode` existentes.
- Definir os GitHub Secrets necessários para o signing do APK, sem nunca colocar keystore/passwords no repositório.
- Garantir que nenhum segredo (chave ed25519 de dispositivo, base de dados de produção, dados de migração) é versionado.

## Capabilities

### New Capabilities
- `release-pipeline`: pipeline de CI/CD do projeto — o sistema SHALL produzir releases APK assinadas de forma automática e reproduzível a partir de tags de versão, num repositório privado, sem expor segredos de signing ou de dados.

### Modified Capabilities

## Impact

- Git/GitHub: novo repositório, `.gitignore`, remote SSH, GitHub Actions, GitHub Releases.
- Android: sem alterações de código; o workflow usa o Gradle Wrapper existente e as env vars de signing já suportadas (`KEYSTORE_PATH`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`).
- Servidor/migração: sem alterações de código; apenas exclusão de dados e artefactos do controlo de versões.
- Sem alteração de comportamento, arquitetura, dependências ou versões da aplicação.
