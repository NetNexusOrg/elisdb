# Design

## Context

Ver `proposal.md` — Why. `ClientFormViewModel.save()` corre em `viewModelScope`,
grava pelo repositório e emite `_saved` para o ecrã navegar de volta.
`SyncManager.sync()` é single-flight, corre em `runScope` (independente do scope
do chamador) e publica `SyncStatus`/`SyncFault` observáveis no ecrã principal.

## Goals / Non-Goals

**Goals:**

- Enviar as alterações logo após guardar, sem bloquear a navegação nem duplicar
  sincronizações.

**Non-Goals:**

- Sincronizar depois de apagar um cliente (comportamento atual mantém-se).
- Repetir automaticamente o sync em caso de falha ou adicionar retry próprio.
- Sincronizar ao abrir/fechar formulários ou por temporizador.

## Decisions

### Gatilho no view model, não no repositório

`save()` do repositório é usado em testes e noutros contextos; pôr lá o sync
obrigaria todos os guardares a falar com a rede e tornaria os testes
instrumentados dependentes do servidor. O view model é o ponto onde “guardar pelo
utilizador” é inequívoco.

### Disparar sem esperar (fire-and-forget)

Depois do `onSuccess`, lançar `repository.syncNow()` em `viewModelScope` e emitir
`saved` de imediato. O `SyncManager` corre o transfer em `runScope`, por isso a
navegação (que limpa o view model) não cancela o sync. Esperar pelo resultado
atrasaria a volta ao ecrã anterior e obrigaria a mostrar erros num formulário que
já não interessa.

### Falha não é silenciosa

O erro continua a chegar ao utilizador pelo estado de sync já existente no ecrã
principal (`SyncFault`), sem snackbar no formulário. Sem servidor configurado, o
`syncNow()` devolve o estado de configuração em falta que o ecrã principal já
mostra.

### Single-flight cobre guardares seguidos

`SyncManager.sync()` junta chamadas concorrentes; um segundo guardar durante o
sync não cria outro run. Se o run já passou da fase de push, o registo fica
pendente e segue no próximo sync — o requisito permite explicitamente.

## Risks / Trade-offs

- **Sync em cada guardar pode gastar dados** em edições frequentes → payloads são
  pequenos e o hash evita reenvio de fotos inalteradas.
- **Guardar offline mostra erro de sync** no ecrã principal → é o comportamento
  atual para sync manual; o cliente fica guardado e pendente.
- **Testes do view model passam a chamar `syncNow`** → o fake do repositório já
  tem `syncNow`; os testes podem contar chamadas sem rede.

## Migration Plan

Só código da app; sem migração de dados. Rollback é remover a chamada e repor o
requisito “manual only”.

## Open Questions

Nenhuma.
