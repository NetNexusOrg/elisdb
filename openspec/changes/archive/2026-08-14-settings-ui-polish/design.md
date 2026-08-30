## Context

- `SettingsScreen.kt` desenha as linhas URL/chave/utilizador como `Text` soltos num `Column` com `Arrangement.spacedBy(16.dp)`, misturando `titleMedium`, `labelMedium`, `bodyLarge`, `bodyMedium` e `bodySmall` sem agrupamento visual.
- O tema usa `Typography` Material3 por defeito (apenas `bodyLarge` é explicitamente definido em `Type.kt`); os restantes estilos caem nos defaults do Material3, o que pode causar perceção de tamanhos incoerentes entre estilos "sistemáticos".
- `SyncStatusBar` já existe como barra compacta; a estrutura lógica de estado (KeyIdentity, syncStatus, mensagens) está consolidada — ver change `settings-show-key-user` (arquivado).
- O ecrã é read-only: não há formulário, só informação + ações (sync, remover/importar chave).

## Goals / Non-Goals

**Goals:**
- Agrupar a informação da configuração do servidor em cartões (`Card`) com padding interno, melhorando a leitura e o look&feel.
- Estabelecer uma hierarquia tipográfica clara e uniforme no ecrã: título de secção, labels de campo e valores com tamanhos consistentes entre si.
- Manter a `Typography` global quase intocada: resolver a coerência localmente no `SettingsScreen` com estilos Material3 existentes, só ajustando `Type.kt` se se revelar estritamente necessário.

**Non-Goals:**
- Não alterar lógica de estado, sync ou identidade (`SettingsViewModel`, `SyncStatusBar` funcionalidade).
- Não redesenhar outros ecrãs da app.
- Não adicionar novas dependências de UI.

## Decisions

### 1. Agrupar a configuração em cartões Material3

Envolver a secção "Configuração do servidor" num `Card` (ou `ElevatedCard`) com `padding(16.dp)` e espaçamento vertical interno, agrupando as linhas label+valor por campo. A barra de sync e o bloco de importação/remoção de chave podem ficar fora dos cartões ou em cartões próprios.

- **Alternativa considerada:** manter textos soltos e apenas ajustar `sp`. Rejeitada porque o problema é também estrutural (falta de agrupamento), não só tipográfico.

### 2. Hierarquia tipográfica explícita e consistente no ecrã

Definir, no `SettingsScreen`, um padrão repetível de par label+valor:
- Título de secção: `titleMedium`.
- Label de campo: `labelLarge` (mais legível que `labelMedium` para separar do valor) — **ou** manter `labelMedium` se a consistência com o estado atual for preferida. Decisão: usar `labelLarge` para labels, `bodyLarge` para valores, `bodySmall` para texto de apoio/erros.
- Valor: `bodyLarge` (maior que o label, criando a distinção pedida no spec).

Este mapeamento garante que **todos** os labels têm o mesmo tamanho e **todos** os valores têm o mesmo tamanho.

- **Alternativa considerada:** alterar `Type.kt` para redefinir estilos globalmente. Rejeitada por impacto global desnecessário; a coerência local resolve o requisito sem efeitos colaterais noutros ecrãs.

### 3. Espaçamento vertical padronizado

Usar espaçamento fixo entre label e valor dentro de cada campo (ex.: 4.dp) e maior entre campos (ex.: 12.dp), em vez do `spacedBy(16.dp)` uniforme atual. Separar grupos (barra de sync, cartão de config, ações) com espaçamento ligeiramente maior (16–20.dp).

### 4. Extrair um composable `ServerInfoRow`

Extrair um composable privado (ex.: `ServerInfoRow(label, value, color?)`) para garantir o mesmo estilo em URL, chave e utilizador — elimina a duplicação atual e torna a consistência tipográfica estrutural, não manual.

- **Alternativa considerada:** repetir os `Text` por linha. Rejeitada por propiciar regressões de consistência (fonte do bug atual).

## Risks / Trade-offs

- **Mudança visual pode alterar os bounds/semântica capturados em testes manuais** → Mitigação: alterações de UI pura; verificar visualmente no emulador após implementação e confirmar que os estados (com/sem chave, resolução de identidade) continuam legíveis.
- **Uso de `labelLarge` pode parecer grande em ecrãs pequenos** → Mitigação: testar no emulador (1080×2400) e, se necessário, reverter para `labelMedium` mantendo o padrão único.
- **Extração do composable pode alterar levemente o layout** → Mitigação: manter `Modifier.fillMaxWidth()` e cores `onSurfaceVariant`/`onSurface` como hoje.

## Migration Plan

- Alterações de UI pura; sem migração de dados nem backend. Rollback: reverter `SettingsScreen.kt`.

## Open Questions

Nenhuma.