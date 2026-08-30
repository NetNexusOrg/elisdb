## Context

Monorepo na raiz `/home/ricardo/Projectos/elisdb` com três componentes (app Android em `elisdb-android/`, servidor FastAPI em `elisdb-server/`, migração em `elisdb-memento/`) e a infraestrutura OpenSpec/OpenCode. Não existe ainda Git. O build de release Android já está automatizado localmente por `elisdb-android/scripts/build-release.sh`, que lê credenciais de `~/.keystores/elisdb-release.env`; o `app/build.gradle.kts` resolve o signing a partir de `keystore.properties` ou das env vars `KEYSTORE_PATH`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` e falha rapidamente na ausência das mesmas.

## Goals / Non-Goals

**Goals:**
- Repositório Git único na raiz do monorepo com `.gitignore` correto.
- Repositório GitHub PRIVATE com `origin` por SSH.
- Workflow único (`android-release.yml`) que testa, constrói e publica releases.
- Signing do APK exclusivamente via GitHub Secrets.

**Non-Goals:**
- Não alterar código, comportamento, arquitetura, dependências ou versões da app.
- Não publicar dados reais (chaves de dispositivo, bases de dados, dados de migração).
- Não criar credenciais/keystores fictícios nem inventar passwords.

## Decisions

1. **Repositório único na raiz do monorepo** (e não sub-módulos por componente). O projeto já partilha `openspec/`, `README.md` e histórico de mudanças na raiz; separar quebraria a coerência do OpenSpec.

2. **Repositório GitHub chamado `elisdb`** (nome da pasta/`rootProject.name`), PRIVATE, criado com `gh` já autenticado como `NetNexusOrg`, remote SSH `git@github.com:NetNexusOrg/elisdb.git`.

3. **Workflow único** `android-release.yml` com dois jobs:
   - `test`: corre `testDebugUnitTest` + `lintDebug` em push/PR/dispatch (sem segredos).
   - `release`: só em tags `v*` — decodifica o keystore de um secret base64, corre `assembleRelease`, faz upload do APK e cria a GitHub Release.
   Alternativa considerada: dois workflows separados (CI vs release) — rejeitada para manter um único ponto de configuração, como pedido.

4. **Toolchain no CI**: `actions/setup-java` (Temurin 17, igual ao `jvmToolchain(17)` do projeto), `android-actions/setup-android@v3` (instala/aceita licenças do SDK; o AGP descarrega a platform 36), `gradle/actions/setup-gradle` para caching. Rejeitado instalar versões arbitrárias sem corresponder ao projeto.

5. **Signing via secrets**: secret `ANDROID_KEYSTORE_BASE64` (conteúdo do `.jks` em base64) decodificado para `$RUNNER_TEMP/elisdb-release.jks`; secrets `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` mapeados para as env vars que o `build.gradle.kts` já lê. Reutiliza-se o mecanismo de signing existente em vez de o substituir.

6. **Versionamento**: SemVer `MAJOR.MINOR.PATCH`; a tag `vX.Y.Z` dispara o release e deve corresponder ao `versionName` (hoje `1.4`; a introdução do patch não altera a versão atual — ver Open Questions). `versionCode` continua incrementado manualmente em `gradle.properties`.

7. **Segredos a excluir** (via `.gitignore`): `Maria.config.json` (chave real de dispositivo), `*.config.json` (já ignorado no servidor), bases `.db`, `ELIS.csv`/`memento-import.db`, `.venv`, `__pycache__`, `.gradle`, `build/`, `local.properties`, `.idea`, `keystore.properties`, `*.jks`/`*.keystore`.

## Risks / Trade-offs

- **[Keystore existente pode não estar em base64]** → A instrução de configuração dos secrets documenta como gerar o base64 do `.jks` real (`base64 -w0`), sem inventar valores.
- **[Secrets ausentes fazem o release falhar]** → É o comportamento desejado (fail-fast já existente); documentado nos secrets necessários.
- **[`versionName` atual (`1.4`) não tem patch]** → A estratégia SemVer não exige bump imediato; a primeira tag real (`v1.4.0`) será criada manualmente quando o utilizador decidir.
- **[SDK 36 ainda não instalado no runner]** → `setup-android` + AGP descarregam a platform automaticamente; pode aumentar o tempo do primeiro run.
