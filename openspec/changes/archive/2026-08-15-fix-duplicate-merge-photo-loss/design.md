# Design: Fix duplicate merge photo loss

## Context

O `mergeDuplicate` (SyncCoordinator) usa `clienteDao.insert` (REPLACE) sobre o `existing_id`. Quando o canónico já existe localmente, o DELETE+INSERT do REPLACE dispara a FK CASCADE de `fotos` e apaga as fotos canónicas localmente; depois o re-push reenvia só as fotos do registo local, e `_replace_fotos` no servidor apaga as canónicas — permanentemente, e sem a app as ter visto. O problema tem três componentes interligados (cascade local, dados canónicos indisponíveis, re-push amputado). Ver `proposal.md` — Why.

## Goals / Non-Goals

**Goals**
- Nenhuma foto se perde num merge de duplicado, em qualquer dos dois cenários (canónico local presente/ausente).
- A resposta `duplicate` torna o merge auto-suficiente (uma viagem).
- O merge mantém-se automático e transparente (sem novo passo de confirmação UX).

**Non-Goals**
- Mudar a ordem do sync (push-before-pull mantém-se).
- Introduzir confirmação do utilizador antes do merge.
- Alterar a regra de negócio "nome normalizado = mesmo cliente".
- Resolver perdas de fotos fora do caminho de duplicate (ex. push normal de um registo editado com cache desatualizada).

## Decisions

### 1. O servidor devolve o canónico na resposta `duplicate` (não pull-before-push)

A alternativa mais atraente era reordenar o sync para puxar antes de pushar, de modo a que as fotos canónicas chegassem antes do re-push. Foi rejeitada porque muda a ordem global do sync (decisão deliberada, com dezenas de testes sobre a sequência push/pull) e depende do raciocínio do watermark para provar que o canónico vem num pull de delta. Devolver o canónico na resposta do push é contido: reutiliza `ClienteOutDto`/`FotoOutDto` já existentes dos dois lados, mantém a ordem do sync intacta e faz o merge atómico num round-trip.

### 2. `mergeDuplicate` usa `upsert` em vez de `insert` (REPLACE)

Reutiliza o `@Upsert` já introduzido em `ClienteDao` para corrigir o bug de cascade no `applyServerItem`. Elimina o DELETE+INSERT que dispara a FK CASCADE de `fotos` quando o canónico já existe localmente. É a mesma correção, aplicada ao segundo ponto de `insert` com id já existente.

### 3. Merge por união de fotos, materializando as canónicas

O conjunto final = fotos do canónico (da resposta) + fotos do registo local. As fotos canónicas passam pelo mesmo `PhotoMaterializer` já usado no pull (materializa se conteúdo diferir, mantém se igual), de modo a reaproveitar a lógica de dois-fases (ficheiro antes da row; ficheiro antigo apagado só após o commit).

### 4. Re-push do conjunto fundido na sync seguinte (sem mudança de timing)

O `pushEdits()` já correu quando o merge é detetado, pelo que o re-push do registo fundido acontece na sync seguinte — comportamento atual mantido. Como o servidor não persiste o registo rejeitado e não altera o canónico ao responder `duplicate`, o estado do servidor fica intacto nessa janela; nada se perde.

## Wire format

```
POST /api/push  →  PushResultDto(id, status, existing_id,
                                  client?, fotos? )     // novos campos, só no duplicate
```

`client` e `fotos` são `ClienteOutDto`/`FotoOutDto` opcionais, presentes apenas quando `status == "duplicate"`. Clientes antigos ignoram os campos extra (desserialização tolerante).

## Risks / Trade-offs

- **[Payload] A resposta `duplicate` carrega fotos base64** → tráfego simétrico ao push; o cliente precisa delas de qualquer forma. Impacto só no caminho raro de duplicate.
- **[Compatibilidade] Clientes antigos sem os campos novos** → os campos são opcionais; um cliente antigo continua a receber `existing_id` e a fazer o merge amputado (perde fotos canónicas, como hoje). A correção beneficia apenas clientes novos; aceitável para uma app com um único store.
- **[Conflito de ids de foto] Foto do canónico com id igual a foto local** → id é UUID, colisão improvável; na eventualidade, o `upsert` de foto (REPLACE por id) mantém a do re-push, sem perda de ficheiro não referenciado (sweep gere órfãos).
- **[`_replace_fotos` incondicional no push normal]** → fora de âmbito (Non-Goals); a janela de perda entre "editar registo com cache antiga" e "re-push" persiste. Documentado como limitação conhecida.

## Migration Plan

Servidor e app evoluem em conjunto. O servidor pode ser deployado antes (os campos novos são adicionais; clientes antigos inalterados). A app nova só precisa dos campos quando o servidor os devolve. Sem migração de dados.

## Open Questions

Nenhum — as decisões que mudariam specs, abordagem ou tarefas foram resolvidas acima.