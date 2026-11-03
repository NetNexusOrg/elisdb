# Design

## Contexto

Os filtros da lista e do mapa eram `MutableStateFlow` inicializados com valores fixos. Como os ViewModels são recriados em cada arranque, as escolhas do utilizador perdiam-se. Não existe necessidade de sincronizar estas preferências nem de as guardar na base de dados.

## Decisão

Criar `ClientViewPreferences`, uma interface pequena com implementação em `SharedPreferences` simples:

- ficheiro `client_view_preferences`;
- chaves `client_list_filter`, `client_sort_order` e `client_map_filter`;
- valores guardados pelo `name` do enum;
- leitura com fallback para os valores por omissão (`ALL`, `NAME`, `ACTIVE`) quando a chave não existe ou é inválida.

Os ViewModels recebem a interface por construtor, com uma implementação no-op por omissão para não quebrar testes existentes. `AppContainer` cria a implementação real e `Navigation` injeta-a nos dois ViewModels.

## Alternativas consideradas

- **DataStore:** mais moderno, mas exigiria uma dependência nova e não traz vantagem para três valores pequenos.
- **Base de dados:** desnecessário e sujeito a migrações para preferências de UI.
- **Guardar no ViewModel:** não sobrevive ao reinício da app.

## Riscos

- Preferências antigas ou inválidas: tratadas com fallback para os valores por omissão.
- Filtros independentes: a lista e o mapa usam chaves separadas, pelo que uma escolha não altera a outra.
- Pesquisa: permanece transitória de propósito; não é guardada.
