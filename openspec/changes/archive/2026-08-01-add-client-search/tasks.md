## 1. Filtering Logic

- [x] 1.1 Add a pure `matchesQuery(cliente, query)` helper that matches a client when the query is contained in the name, phone, contract or notes (case-insensitive and diacritic-insensitive via `Normalizer` NFD + lowercase)
- [x] 1.2 Add unit tests for `matchesQuery`: matches on each field (name, phone, contract, notes), case-insensitive, diacritic-insensitive, blank query matches everything, and no-match returns false

## 2. ViewModel

- [x] 2.1 Add a `query` StateFlow and `onQueryChange` to `ClientListViewModel`; `combine` the query with `repository.clientesComFotos`, filter with `matchesQuery`, and carry the query in `ClientListUiState.Success`
- [x] 2.2 Extend `ClientListViewModelTest`: typing a query filters the list, a blank query returns all clients, and a query with no matches returns an empty list

## 3. UI

- [x] 3.1 Add a search `OutlinedTextField` above the list in `ClientListScreen` with a search icon, placeholder, and a clear action when non-empty, wired to `onQueryChange`
- [x] 3.2 Add the distinct no-results empty state ("Nenhum cliente corresponde à pesquisa.") shown when the filtered list is empty with a non-blank query, keeping the existing message when there are no stored clients
- [x] 3.3 Add an instrumented Compose test for the client list screen: typing a search term filters the shown rows and a non-matching term shows the no-results message

## 4. Verification

- [x] 4.1 Run `./gradlew assembleDebug lintDebug` and confirm a green build
- [x] 4.2 Run `./gradlew testDebugUnitTest` and confirm all unit tests pass
- [x] 4.3 Manual smoke-test on the emulator: type a search term matching a client's name/contract/notes and confirm the list filters; clear the search and confirm the full list returns; confirm the no-results message for a non-matching term
