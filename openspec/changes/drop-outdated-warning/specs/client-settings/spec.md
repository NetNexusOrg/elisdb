# Spec Delta — client-settings

## REMOVED Requirements

### Requirement: Outdated server warning

**Reason**: o aviso era dirigido ao utilizador da app, que não pode atualizar o
servidor; é informação técnica de operação e não uma ação disponível no ecrã.

**Migration**: nenhuma ação do utilizador. A versão de API continua disponível em
`/health` e `/api/latest` para diagnóstico e a app continua a mapeá-la.
