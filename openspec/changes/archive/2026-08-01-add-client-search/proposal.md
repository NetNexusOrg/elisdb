## Why

When many clients are stored, finding a specific one requires scrolling through the whole list. There is currently no way to search or filter the client list.

## What Changes

- Add a search field at the top of the client list screen.
- As the user types, the list filters in real time to clients whose text fields (name, phone, contract, notes) contain the search term.
- Matching is case-insensitive and diacritic-insensitive.
- When the search matches nothing, show a "no results" message; when there are no stored clients at all, keep the existing empty-list message.
- Clearing the search field restores the full list.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the "Client list screen" requirement gains search/filter behavior.

## Impact

- `ClientListScreen.kt` and `ClientListViewModel.kt` — add the search field UI and the filtering logic.
- `ClientListViewModelTest` — new unit tests for the query state and filter matching.
- `client-screens` spec — updated requirement with search/filter scenarios.
