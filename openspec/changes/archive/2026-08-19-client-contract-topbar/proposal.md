## Why

O número de contrato é um identificador rápido; exibi-lo na top bar do detalhe, logo a seguir ao botão voltar, torna-o mais visível e liberta o corpo do ecrã (deixa de haver uma linha de contrato acima do nome).

## What Changes

- No ecrã de detalhe do cliente, o número de contrato passa a ser apresentado na top bar, à direita do botão/ícone de voltar atrás.
- O bloco de contrato acima do nome no corpo do ecrã deixa de ser exibido.
- Sem contrato → a top bar não mostra número de contrato (só o botão voltar, como hoje).

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-screens`: a requirement "Client detail screen" muda as cenas "Contract shown above the name" / "No contract on the client" — o contrato passa a ser mostrado na top bar ao lado do voltar, em vez de acima do nome.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailScreen.kt` — usar o slot `title` da `TopAppBar` (ou elemento equivalente a seguir ao `navigationIcon`) para mostrar o contrato quando existir; remover o bloco de contrato do `ClientDetailContent`.