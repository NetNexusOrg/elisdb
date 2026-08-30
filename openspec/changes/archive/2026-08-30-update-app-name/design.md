## Context

Mudança cosmética: alterar o nome visível da app no launcher de `elisdb` para `ElisDB`. Ver `proposal.md` — Why.

## Goals / Non-Goals

- **Goal:** uniformizar a grafia do nome visível com a marca "ElisDB".
- **Non-Goals:** não alterar o package name (`com.example.elisdb`), diretórios, servidor, base de dados ou API.

## Decisions

- **Alterar apenas a string `app_name`** em `res/values/strings.xml`, mantendo `android:label="@string/app_name"` no manifest. É o ponto único que define o nome visível; não há literais duplicados a atualizar.

## Risks / Trade-offs

- Sem riscos relevantes — alteração de uma string sem impacto em lógica, sync ou dados.

## Migration Plan

Nenhum. A alteração aplica-se à próxima build; não requer migração nem rollback.

## Open Questions

Nenhuma.
