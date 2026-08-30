## Context

Estado atual: `/home/ricardo/Projectos/elisdb` é simultaneamente a root do projeto e a root da app Android (contém `app/`, `gradle/`, `gradlew`, `build.gradle.kts`, `settings.gradle.kts`, `docs/`, `scripts/`, `.opencode/`, `openspec/`). O servidor vive em `/home/ricardo/Projectos/elisdb-server`. Nenhum dos dois é repositório git. Motivação completa em `proposal.md` — Why.

Restrições acordadas:
- `openspec/` e `.opencode/` ficam na root do projeto.
- Artefactos runtime do servidor movem-se como estão (`elisdb.db`, `photos/`, `.venv/`, caches).
- Não iniciar git na root.

## Goals / Non-Goals

**Goals:**
- Layout final: root = `/home/ricardo/Projectos/elisdb` com `elisdb-android/`, `elisdb-server/`, `openspec/`, `.opencode/`.
- Todas as referências em docs/scripts compatíveis com o novo layout.
- App e servidor continuam a compilar/testar a partir das novas localizações.

**Non-Goals:**
- Não migrar histórico git (nenhum existe hoje).
- Não criar nenhum repositor git na root.
- Não alterar comportamento, APIs ou semântica de sincronização.
- Não alterar specs principais (é refactoria pura; `skip_specs: true`).

## Decisions

**D1 — Movimento com `mv` simples (sem git).**
Como não há git, basta `mkdir -p elisdb-android elisdb-server` e mover entradas. Para evitar movimento recursivo: criar os dois diretórios-alvo primeiro, depois mover as restantes entradas da root para `elisdb-android/`, e mover o conteúdo de `../elisdb-server` para `elisdb-server/`.
Alternativa considerada: `git mv` — descartada, não há repos.
Alternativa considerada: manter `../elisdb-server` como está — descartada, é o problema que se quer resolver.

**D2 — Entradas da root que vão para `elisdb-android/`.**
`app/`, `build/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties`, `gradlew`, `gradlew.bat`, `local.properties`, `.gitignore`, `.gradle/`, `.kotlin/`, `README.md`, `docs/`, `scripts/`.
Permanecem na root: `openspec/`, `.opencode/` (e os novos `elisdb-android/`, `elisdb-server/`).

**D3 — Entradas do servidor que vão para `elisdb-server/`.**
`app/`, `docs/`, `test_main.py`, `requirements.txt`, `elisdb.db`, `photos/`, `.venv/`, `__pycache__`, `.pytest_cache`, `README.md`, `.gitignore`.

**D4 — Atualização de referências.**
- Do ponto de vista da app (`elisdb-android/`), `../elisdb-server` continua correto (irmãos sob a mesma root) — validar, não reescrever cegamente.
- Server docs: referências internas mantêm-se; validar relativos a `../../`.
- `docs/configuracao.md` e `docs/execucao.md` que mencionam `scripts/build-release.sh` ou caminhos de build: ajustar se necessário.
- Criar um `README.md` conciso na root do projeto a descrever a estrutura (app + servidor + openspec) e apontar para os READMEs de cada subprojeto (a app perde o README que estava na root).

**D5 — `.venv` pode ficar com shebangs obsoletos.**
Os wrappers em `.venv/bin/` têm `shebang` com caminho absoluto antigo (`/home/ricardo/Projectos/elisdb-server/.venv/bin/...`). O interpretador `.venv/bin/python` é um symlink para o python base e continua a funcionar. Nas tarefas de verificação usar sempre `.venv/bin/python -m pytest` / `-m uvicorn`; se o `.venv` falhar, recriar com `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt`.

## Risks / Trade-offs

- [Referências em docs/scripts esquecidas nos novos caminhos] → Tarefa de verificação faz `grep` por `../elisdb-server`, `scripts/`, `app/build` nos ficheiros movidos e corrige.
- [`.venv` com caminhos absolutos quebrado após o move] → Usar `python -m` (D5); recriar `.venv` se necessário.
- [Caches Gradle (`build/`, `.gradle/`) movidos em vão] → Aceitável; evita rebuild inicial. Se preferível, podem ser apagados — decisão do utilizador à implementação.
- [`openspec` / `.opencode` com caminhos hardcoded da root] → Verificar: `allowedEditRoots` da root passa a cobrir o servidor (melhoria, não regressão).