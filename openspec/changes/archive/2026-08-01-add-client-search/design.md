## Context

The client list screen (`ClientListScreen.kt`) is backed by `ClientListViewModel`, which maps `repository.clientesComFotos` (a Room `Flow<List<ClienteComFotos>>`) into a `ClientListUiState.Success` of `ClientListItem`. The client record already carries all text fields (name, phone, contract, notes); nothing needs to change in the data layer. See proposal.md - Why for motivation.

## Goals / Non-Goals

**Goals:**
- Real-time filter of the client list as the user types, matching name, phone, contract and notes.
- Case-insensitive and diacritic-insensitive matching.
- Distinguishable empty states: no stored clients vs. a search with no matches.
- Keep the filtering logic pure and unit-testable.

**Non-Goals:**
- Search over non-text fields (coordinates, id, timestamps) or over photos.
- Fuzzy/ranking search, saved searches, or filtering by structured criteria (e.g., per-field filters).
- Backend/DAO-level search (the dataset is local and small).

## Decisions

**1. Filter in the ViewModel with a `query` StateFlow combined with the repository flow**
Combine `repository.clientesComFotos` with a `MutableStateFlow<String>` query via `combine`, then map to `Success(clientes, query)`. Rationale: reuses the existing reactive pipeline, keeps the DAO untouched, and the result stays reactive to both DB changes and typing. Alternative considered: adding a Room `@Query` with `LIKE` — rejected because it couples the DAO to search semantics and would not give diacritic-insensitive matching cheaply.

**2. Pure matching helper `matchesQuery(cliente, query)`**
Normalize both sides with `java.text.Normalizer` (NFD, strip combining marks) + lowercase, then substring-match over name, phone, contract and notes. Rationale: gives diacritic-insensitive behavior ("joao" matches "João") with a trivially testable function. Empty/blank query matches everything.

**3. Search field rendered above the list in `ClientListScreen`**
An `OutlinedTextField` with a search icon, placeholder "Pesquisar clientes…", and a clear (trailing X) action when non-empty. The field shows in both the populated and the empty-stored-clients states so filtering is always available.

**4. Empty-state messaging driven by `query`**
If the filtered list is empty and `query` is blank → "Ainda não há clientes guardados."; if `query` is non-blank → "Nenhum cliente corresponde à pesquisa." Rationale: preserves the existing message while adding the no-results case.

## Risks / Trade-offs

- Diacritic normalization allocates strings per keystroke → fine for a local list of typical size; if lists grow, the matcher can be moved into a `derivedStateOf`/deduped pipeline.
- `combine` recomputes the filter on every DB emission → mitigated by keeping the matcher pure and cheap.
- Text fields are searched via `orEmpty()` so `null` phone/contract/notes never crash matching.
