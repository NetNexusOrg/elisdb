## 1. Tipografia do bloco de configuração

- [x] 1.1 Em `SettingsScreen.kt`: converter o campo URL para o padrão rótulo+valor (rótulo `labelMedium`/`onSurfaceVariant`; valor `bodyLarge`/`onSurface`), mostrando "Não configurado" apenas quando vazio.
- [x] 1.2 Converter a chave de assinatura para rótulo "Chave de assinatura" (`labelMedium`/`onSurfaceVariant`) + valor mascarado (`bodyLarge`/`onSurface`), mantendo a ação "Remover chave".
- [x] 1.3 Uniformizar o estado vazio de chave como valor "Não configurada" no padrão rótulo+valor (em vez do texto corrido "Nenhuma chave de assinatura configurada."), cor `onSurfaceVariant`.
- [x] 1.4 Apresentar o utilizador associado como rótulo `labelMedium`/`onSurfaceVariant` + valor `bodyLarge`/`onSurface`; "Sem utilizador associado a esta chave." mantém o estilo de valor.

## 2. Feedback e mensagens

- [x] 2.1 Unificar `state.syncNotice` e `state.message` em `bodyMedium`, mantendo as cores de contexto (primary/erro) atuais.
- [x] 2.2 Confirmar que todos os textos do ecrã usam `fillMaxWidth` e que nenhuma linha composta mistura conteúdo no estilo de valor.

## 3. Verificação

- [x] 3.1 Atualizar `SettingsScreenTest` para os rótulos/estilos novos apenas onde verificar apresentação; validar conteúdo inalterado (URL, chave mascarada, utilizador, ação importar/remover).
- [x] 3.2 `testDebugUnitTest` e `compileDebugAndroidTestKotlin` verdes; instrumentados de Definições no emulador.
- [x] 3.3 No emulador, confirmar visualmente que o bloco "Configuração do servidor" está simétrico (rótulos uniformes, valores uniformes).