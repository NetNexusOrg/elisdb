## Context

`PhotoEncoder.encodeToWebp` delega a descodificação em `decode()`, que abre o `InputStream` da origem duas vezes (`resolver.openInputStream(source)`). Quando o ficheiro não existe, `openInputStream` lança `FileNotFoundException` (subclasse de `IOException`), que não é capturada e escapa de `encodeToWebp`. O teste espera `IllegalStateException` (produzido por `error("Não foi possível ler a imagem.")` no caminho em que `decode()` devolve `null`).

## Goals / Non-Goals

**Goals:**
- Uma origem em falta/ilegível passa a ser tratada como falha de descodificação: `decode()` devolve `null` e `encodeToWebp` lança `IllegalStateException` com mensagem clara, sem deixar ficheiro WebP partido.

**Non-Goals:**
- Não alterar downscaling, EXIF nem a codificação WebP.
- Não introduzir novos tipos de erro nem dependências.

## Decisions

- **Tratar `IOException` como "origem ilegível".** Em vez de `resolver.openInputStream(source)?.use { ... }` (que só trata `null`), abrir o stream dentro de `runCatching`/`try-catch` que devolve `null` em caso de `IOException`. Alternativa considerada: mudar o teste para esperar `FileNotFoundException` — rejeitada, porque a intenção documentada (docstring e mensagem "Não foi possível ler a imagem.") é um erro claro e consistente, e o caminho de limpeza do destino já está preparado para `null`.
- **Reaproveitar o caminho `decode() == null` já existente** em `encodeToWebp` (`?: run { dest.delete(); error(...) }`), sem nova lógica de limpeza.

## Risks / Trade-offs

- [Capturar demasiado ao usar `runCatching`] → Preferir capturar apenas `IOException` (ou `FileNotFoundException`), para não mascarar erros inesperados como `OutOfMemoryError`.

## Migration Plan

Sem migração. Mudança local ao `PhotoEncoder`; rollback trivial.

## Open Questions

Nenhuma.
