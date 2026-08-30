## 1. Estrutura de diretórios

- [x] 1.1 Registar estado atual: listar conteúdo da root (`/home/ricardo/Projectos/elisdb`) e de `../elisdb-server` antes de mover.
- [x] 1.2 Criar na root os diretórios `elisdb-android/` e `elisdb-server/`.
- [x] 1.3 Mover para `elisdb-android/` as entradas da app: `app/`, `build/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties`, `gradlew`, `gradlew.bat`, `local.properties`, `.gitignore`, `.gradle/`, `.kotlin/`, `README.md`, `docs/`, `scripts/`.
- [x] 1.4 Mover o conteúdo de `../elisdb-server` para `elisdb-server/` (`app/`, `docs/`, `test_main.py`, `requirements.txt`, `elisdb.db`, `photos/`, `.venv/`, `.pytest_cache/`, `__pycache__/`, `README.md`, `.gitignore`).
- [x] 1.5 Confirmar que a root contém apenas `elisdb-android/`, `elisdb-server/`, `openspec/` e `.opencode/`.

## 2. Documentação e referências de caminhos

- [x] 2.1 Criar `README.md` na root do projeto a descrever a estrutura (app em `elisdb-android/`, servidor em `elisdb-server/`, `openspec/` na root) e a apontar para os READMEs de cada subprojeto.
- [x] 2.2 Verificar referências em `elisdb-android/README.md` e `elisdb-android/docs/*.md`: `../elisdb-server` deve continuar a apontar para o servidor irmão; corrigir qualquer referência que assuma a layout antiga.
- [x] 2.3 Verificar referências em `elisdb-server/README.md` e `elisdb-server/docs/*.md` após o movimento.
- [x] 2.4 Correr `grep` de referências mortas (p.ex. `../../elisdb-server`, `scripts/` na root, `app/` na root) e corrigir as encontradas.
- [x] 2.5 Confirmar que as specs principais em `openspec/specs/` não referenciam caminhos de ficheiro (não requerem alteração: refactoria pura, `skip_specs: true`).

## 3. Verificação

- [x] 3.1 Compilar a app a partir de `elisdb-android/` (`./gradlew :app:compileDebugKotlin`).
- [x] 3.2 Correr os testes do servidor a partir de `elisdb-server/` (`.venv/bin/python -m pytest`; se o `.venv` tiver shebangs obsoletos, recriar com `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt`).
- [x] 3.3 Confirmar que `openspec list`/`openspec status` funcionam da root do projeto.