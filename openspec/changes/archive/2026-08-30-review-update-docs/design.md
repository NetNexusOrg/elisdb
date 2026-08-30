## Context

Ver `proposal.md` — Why. Duas mudanças recentes desalinharam a documentação: a app passou a chamar-se "ElisDB" e o servidor tornou-se Docker-first. Os guias da app ainda ensinam a emitir chaves com execução manual do servidor.

## Goals / Non-Goals

**Goals:**
- Rever e atualizar toda a documentação de utilizador da app e do servidor (READMEs + `docs/`) e o `README.md` raiz.
- Garantir coerência do percurso completo (servidor Docker → emitir chave → importar na app) entre app e servidor.

**Non-Goals:**
- Não alterar código, comportamento, API, base de dados ou specs.
- Não rever as skills de agente (`.opencode/skills/`) — já alinhadas na change anterior; só se o review detetar um erro factual pontual.
- Não introduzir ficheiros novos sem necessidade.

## Decisions

- **Metodologia de review**: verificar cada comando/caminho/env var contra o comportamento real (código + `project-context`), em vez de reescrever especulativamente. Só alterar o que estiver desatualizado ou inconsistente.
- **Nome da app**: usar "ElisDB" (grafia da marca) para o nome visível; preservar identificadores técnicos (`com.example.elisdb`, `rootProject.name = "elisdb"`, pastas, `ELISDB_*`).
  - *Alternativa considerada:* alterar também `rootProject.name`/package para "ElisDB". Rejeitada — são identificadores técnicos, não o nome visível, e mudá-los tem impacto no build/signing.
- **Fluxo Docker-first**: nas instruções da app que emitem chave, usar `docker compose exec elisdb-server python -m app.keys issue ...` e apontar para os guias do servidor; não repetir o processo manual antigo.
- **Consistência de ligações cruzadas**: app remete para `../elisdb-server/docs/` (e vice-versa), garantindo que os passos referidos existem nos dois lados.

## Risks / Trade-offs

- [Documentação desatualizada noutros pontos não óbvios] → varrer os READMEs e `docs/` por referências a `venv`, `uvicorn`, `pytest`, execução manual e comandos antigos.
- [Mudar demasiado (scope creep)] → editar apenas o que estiver factualmente errado ou inconsistente; não reformular estilo.
- [Identificadores técnicos alterados por engano] → checklist explícita do que **não** muda (package, rootProject.name, env vars, paths).

## Migration Plan

Nenhum — alterações de texto. Aplicável diretamente na branch de trabalho.

## Open Questions

Nenhuma.
