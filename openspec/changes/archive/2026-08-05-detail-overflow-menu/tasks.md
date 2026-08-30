## 1. Implement the overflow menu

- [x] 1.1 In `app/src/main/java/com/example/elisdb/ui/detalhe/ClientDetailScreen.kt`, add imports: `androidx.compose.material3.DropdownMenu`, `androidx.compose.material3.DropdownMenuItem`, `androidx.compose.material.icons.filled.MoreVert`, `androidx.compose.runtime.mutableStateOf`, `androidx.compose.runtime.setValue`.
- [x] 1.2 Replace the two `IconButton`s (Editar/Apagar) in the `TopAppBar` `actions` block with a single `IconButton` (`Icons.Default.MoreVert`, contentDescription "Mais opções") that opens a `DropdownMenu` with "Editar" and "Apagar" `DropdownMenuItem`s wired to `onEdit(cliente.cliente.id)` and `viewModel.showDeleteConfirmation()` respectively.
- [x] 1.3 Change the `TopAppBar` `title` to no longer show the client name (empty title). Keep the back navigation icon and the delete confirmation dialog unchanged.

## 2. Verification

- [x] 2.1 Run `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` and confirm BUILD SUCCESSFUL.
- [x] 2.2 Run `./gradlew :app:connectedDebugAndroidTest` on the emulator and confirm all instrumented tests pass.
- [x] 2.3 Manual check on the emulator: open a client's detail screen — the top bar shows no client name and a "⋮" menu; the menu offers "Editar" (opens the edit form) and "Apagar" (opens the delete confirmation); back navigation still works.
