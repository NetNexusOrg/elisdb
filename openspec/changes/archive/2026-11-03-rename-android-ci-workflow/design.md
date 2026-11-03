# Design

## Context

O workflow Android já contém os jobs de CI e o job de release; o nome atual apenas acrescenta `/ Release`.

## Decision

Alterar apenas o campo `name:` do workflow e a referência documental. O identificador do ficheiro, os triggers e os jobs permanecem inalterados.

## Risks

- Nenhum risco funcional: o nome é apenas apresentação na UI do GitHub Actions.
