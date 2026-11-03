# Tasks

## 1. Implementação

- [x] 1.1 Criar `ClientViewPreferences` com implementação em `SharedPreferences` e no-op para testes.
- [x] 1.2 Expor as preferências no `AppContainer`.
- [x] 1.3 Inicializar e guardar o filtro e a ordenação no `ClientListViewModel`.
- [x] 1.4 Inicializar e guardar o filtro no `ClientMapViewModel`.
- [x] 1.5 Injetar as preferências nos ViewModels através da `Navigation`.
- [x] 1.6 Adicionar testes unitários para restauro e gravação das preferências.

## 2. Verificação

- [x] 2.1 Correr testes unitários, lint e `assembleDebug`.
- [x] 2.2 Verificar manualmente que os três valores sobrevivem a force-stop/relaunch.
- [x] 2.3 Validar e sincronizar a spec, e arquivar a change.
