## Context

O ecrã de Definições (`SettingsScreen.kt`) renderiza a secção "Configuração do servidor" com estilos mistos: o URL é um rótulo `labelMedium` seguido de valor `bodyLarge`, a chave é uma linha composta `bodyMedium` ("Chave de assinatura: <masked>"), o utilizador usa `bodySmall` e os estados vazios/feedbacks variam entre `bodyMedium` e `bodySmall` com cores diferentes. Ver proposal.md para motivação.

O tema atual do projeto são os estilos tipográficos do Material 3 (`MaterialTheme.typography.*`) já em uso; não existe um padrão próprio de definições. O ecrã usa `Column` simples com `spacedBy(16.dp)` e `fillMaxWidth` nos textos.

## Goals / Non-Goals

**Goals:**
- Um padrão simétrico "rótulo + valor" para o bloco de configuração do servidor.
- Todos os valores do bloco com o mesmo estilo tipográfico e a mesma cor base.
- Estados vazios e feedbacks unificados nos mesmos estilos, para que nada se destaque por peso/tamanho inconsistente.

**Non-Goals:**
- Alterar conteúdo, ordem ou lógica dos textos.
- Redesenhar layout/estrutura geral do ecrã (posições, espaçamentos de grelha, componentes), exceto o que for estritamente necessário para agrupar rótulo+valor.
- Mudanças de tipografia noutros ecrãs da aplicação.

## Decisions

**1. Padrão rótulo + valor para os três campos do bloco (URL, chave, utilizador).**
Cada informação passa a ser um rótulo (`labelMedium`, cor `onSurfaceVariant`) seguido do valor (`bodyLarge`, cor `onSurface`), ambos `fillMaxWidth`. Isto substitui a linha composta "Chave de assinatura: <masked>" por rótulo "Chave de assinatura" + valor mascarado, simétrico aos restantes.
- Alternativa: manter a linha composta e aumentar os outros para `bodyMedium` — mas isso mantém o caso assimétrico do rótulo `labelMedium` e não uniformiza a hierarquia visual. Rejeitada.

**2. Estados vazios com o mesmo estilo de valor (`bodyLarge`), cor `onSurfaceVariant`.**
"URL do servidor" vazio mostra "Não configurado"; quando não há chave, o bloco da chave mostra rótulo próprio e o valor "Não configurada" — em vez do texto corrido "Nenhuma chave de assinatura configurada." Assim o padrão rótulo+valor permanece intacto independentemente do estado.
- "Não configurado" continua a ser o placeholder do valor. `keyIdentityResolved` fica como está.

**3. Feedback de texto unificado: `bodyMedium`, cor conforme o contexto.**
O aviso de sync (`state.syncNotice`) e a mensagem pós-import (`state.message`) passam ambos a `bodyMedium` (mantendo cores `primary`/`error` conforme já ocorre). O texto de ajuda da importação e o utilizador-associado mantêm `bodySmall`/`bodyMedium` conforme o padrão acima. Isto elimina a mistura atual onde um aviso é `bodySmall` e a outra mensagem `bodyMedium`.
- O texto do estado de sync (`SyncStatusBar`) mantém `bodySmall`, pois é uma linha compacta com ícone e botão; não pertence ao bloco rótulo+valor.

**4. Extrair um composable pequeno (`ServerConfigRow`) opcional.**
Não exigido: as três linhas são poucas e o padrão é simples; repetir `labelMedium`+`bodyLarge` diretamente mantém o código legível sem novo nível de abstração. Decidido a favor da duplicação mínima local num helper (privado) se se revelar necessário; caso contrário, editar inline.

## Risks / Trade-offs

- **Testes de UI sensíveis a estilos** → Atualizar/validar `SettingsScreenTest` para refletir rótulos por campo; verificar que apenas asserções de apresentação mudam, não as de conteúdo.

## Open Questions

Nenhuma. (Os estilos escolhidos usam o MaterialTheme existente; sem dependências novas.)