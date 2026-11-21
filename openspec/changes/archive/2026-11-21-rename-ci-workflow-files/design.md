# Design

## Context

Ver `proposal.md` — Why. Restrições que moldam a abordagem:

- `elisdb-server/scripts/deploy-server.sh` decide se pode publicar procurando um run
  com `workflowName == "Server CI"` via `gh run list`. O nome apresentado do
  workflow é, por isso, uma dependência funcional.
- `openspec/specs/release-pipeline/spec.md` exige que o workflow Android apareça
  como `Android CI`.
- O repositório é privado num plano sem branch protection, portanto não há
  required status checks a reconfigurar; os nomes dos jobs são apenas visíveis.
- `server-ci.yml` referencia-se a si próprio no filtro `paths`.

## Goals / Non-Goals

**Goals:**

- Um nome de ficheiro por workflow que corresponda 1:1 ao nome apresentado.
- Nomes de jobs legíveis na lista de checks, sem alterar o grafo de dependências
  (`needs`), os triggers ou o comportamento do CI.

**Non-Goals:**

- Separar o release num workflow próprio.
- Deduplicar o setup Android repetido em quatro jobs (composite action) — fica para
  uma change posterior, se houver necessidade.
- Alterar versões de actions, runners (`ubuntu-26.04`) ou secrets.

## Decisions

### Renomear o ficheiro em vez de o dividir

`android-release.yml` → `android-ci.yml`, com o conteúdo intacto.

- *Porque:* o ficheiro já é o CI completo do Android; o nome é que estava errado.
- *Alternativa rejeitada — dividir em `android-ci.yml` + `android-release.yml`:*
  obrigaria a passar o APK entre workflows por artifact, duplicaria o setup
  (Java/SDK/Gradle) e criaria três ficheiros para um pipeline pequeno.
- *Alternativa rejeitada — prefixo `ci-*` em ambos:* a UI do GitHub Actions ordena
  pelo `name:` e não pelo ficheiro, pelo que o prefixo só afetaria a árvore de
  ficheiros, sem ganho que justifique o churn.

### Manter os nomes apresentados (`Android CI` / `Server CI`)

- *Porque:* `deploy-server.sh` e a spec `release-pipeline` dependem deles. O
  problema identificado é o nome do ficheiro, não o nome visível.

### Renomear jobs por componente

- `Test` → `Android tests` (simétrico com `Server tests`).
- `Instrumented tests (full suite, non-blocking)` → `Instrumented suite (non-blocking)`
  (o "full suite" é redundante; o `non-blocking` é a informação útil).
- `Sync smoke (release gate)` → `Server sync smoke (release gate)` (está no
  workflow Android mas exercita o servidor).

## Risks / Trade-offs

- [O histórico de runs fica dividido] → O GitHub associa runs ao caminho do
  ficheiro; após o rename, as runs antigas continuam visíveis numa entrada separada
  do workflow no separador Actions. Não há perda de releases, tags ou artifacts.
  Mitigação: nenhuma ação necessária; é esperado e desaparece com o tempo.
- [Referências pendentes ao nome antigo] → Mitigação: `grep -rn "android-release"`
  excluindo `.git` e `openspec/changes/archive/` (histórico não se altera).
- [Filtro `paths` do servidor] → Não é tocado, porque `server-ci.yml` mantém o nome.
