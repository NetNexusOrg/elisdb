# Proposal

## Why

A app já sabe verificar atualizações no ecrã **Sobre**, mas só o faz quando o
utilizador abre esse ecrã. Quem usa a app todos os dias pode ficar muitos dias
com uma versão antiga sem reparar que há uma nova publicada. Um aviso ao abrir a
app torna a atualização visível sem obrigar a procurar.

## What Changes

- Ao abrir a app, é feita uma verificação silenciosa da versão publicada pelo
  servidor, apenas quando existe servidor e chave configurados.
- Se houver uma versão mais recente, aparece um diálogo com:
  - **Atualizar** — fecha o diálogo e abre o ecrã **Sobre**, onde o fluxo já
    existente de transferência, verificação e instalação continua disponível.
  - **Adiar** — fecha o diálogo e só volta a avisar no próximo arranque.
- O aviso nunca aparece quando não há versão nova, não há release publicada, a
  app não está configurada, está offline ou a verificação falha. Também não
  aparece enquanto o ecrã **Sobre** já está aberto.
- O ecrã **Sobre** e o cartão **Atualizações** mantêm o comportamento atual.

## Capabilities

### New Capabilities

(nenhuma)

### Modified Capabilities

- `client-update`: acrescenta o aviso automático ao abrir a app, com as ações
  Atualizar (navegar para Sobre) e Adiar (silenciar até ao próximo arranque).

## Impact

- Android: novo composable de aviso, reutilização do `UpdateViewModel` já
  existente e do ecrã **Sobre**; `Navigation.kt` passa a montar a verificação no
  arranque.
- Sem alterações ao servidor, à API (`/api/app/latest` e `/api/app/download` já
  existem), ao `ApiClient`, ao `ApkInstaller` ou à versão da app.
