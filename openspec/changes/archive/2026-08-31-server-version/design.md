## Context

O servidor ElisDB atualmente tem a versão hardcoded como `"1.0.0"` no construtor FastAPI, sem forma de a alterar em tempo de build ou exposição via endpoint. O Dockerfile não suporta build args para versão, e o compose.yaml não propagate versão. O endpoint `/health` retorna apenas `{"status": "ok"}`.

## Goals / Non-Goals

**Goals:**
- Disponibilizar versão do servidor via endpoint `/health`
- Permitir configuração de versão via build arg no Docker
- Expor versão via variável de ambiente para scripting/monitorização
- Manter compatibilidade com deployments existentes

**Non-Goals:**
- Alterar o mecanismo de versionamento do Android
- Adicionar endpoint dedicado `/version` (usar `/health` existente)
- Implementar versionamento de API ourottrottling por versão

## Decisions

### 1. Ficheiro VERSION como fonte de verdade

**Decisão:** Criar `elisdb-server/VERSION` com a versão SemVer.

**Alternativas consideradas:**
- Hardcoded no Python: difícil de manter, requer alteração de código para cada release
- pyproject.toml: não existe no projeto, seria overkill para este caso
- Variável de ambiente pura: fonte de verdade fora do repo, difícil de rastrear

**Razão:** Ficheiro VERSION é simples, editável, e pode ser lido pelo Python e Docker. Segue padrão similar ao Android (`gradle.properties`).

### 2. Build arg VERSION no Dockerfile

**Decisão:** Adicionar `ARG VERSION=0.0.0-dev` no Dockerfile e copiar ficheiro VERSION.

**Alternativas consideradas:**
- Variável de ambiente apenas: não fica baked na imagem
- Multi-stage com leitura de ficheiro: mais complexo, desnecessário

**Razão:** Build arg permite fixar versão na imagem durante build, mantendo fallback para dev.

### 3. Endpoint /health expandido

**Decisão:** Modificar resposta de `{"status": "ok"}` para `{"status": "ok", "version": "..."}`.

**Alternativas consideradas:**
- Novo endpoint `/version`: quebra compatibilidade, requer mudança no cliente
- Header HTTP: menos visível, difícil de consumir

**Razão:** Expandir `/health` é não-breaking e mantém ponto de verificação único.

### 4. Variável de ambiente ELISDB_VERSION

**Decisão:** Expor versão via `ELISDB_VERSION` para scripting.

**Razão:** Permite que scripts de deploy/monitorização acedam à versão sem parsing de JSON.

## Risks / Trade-offs

- **Risk:** Versão hardcoded em múltiplos sítios → **Mitigation:** Ficheiro VERSION como fonte de verdade, Python lê desse ficheiro
- **Risk:** Deployments existentes sem VERSION → **Mitigation:** Default `0.0.0-dev` no Dockerfile
- **Trade-off:** Mais um ficheiro para manter vs. configuração centralizada → Aceitável para clareza