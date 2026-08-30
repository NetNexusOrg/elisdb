## Context

See proposal.md - Why. The detail screen (`ClientDetailScreen.kt`) currently renders a `TopAppBar` with the client's name as the title and two direct `IconButton` actions: pencil (`Icons.Default.Edit` → `onEdit(id)`) and trash (`Icons.Default.Delete` → `viewModel.showDeleteConfirmation()`). The name is also rendered in the content body (`ClientDetailContent`), so it appears twice.

## Goals / Non-Goals

**Goals**
- Move edit/delete into a Material 3 overflow dropdown menu (⋮) in the top bar.
- Remove the client name from the top bar title.
- Keep back navigation and all existing behavior (edit form, delete confirmation dialog) unchanged.

**Non-Goals**
- No changes to the content layout, navigation, view models, or delete confirmation flow.

## Decisions

### D1: Single overflow menu in the top bar actions

Replace the two `IconButton`s with one `IconButton` showing `Icons.Default.MoreVert` that toggles a `DropdownMenu` with two `DropdownMenuItem`s ("Editar" with the pencil leading icon, "Apagar" with the trash leading icon).

```kotlin
actions = {
  val cliente = state.cliente
  if (cliente != null) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
      IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
      }
      DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
          text = { Text("Editar") },
          leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
          onClick = { menuOpen = false; onEdit(cliente.cliente.id) },
        )
        DropdownMenuItem(
          text = { Text("Apagar") },
          leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
          onClick = { menuOpen = false; viewModel.showDeleteConfirmation() },
        )
      }
    }
  }
}
```

Rationale: this is the standard Material 3 pattern for secondary actions on a screen top bar; it declutters the bar and scales if more actions appear later. Alternatives considered: keeping direct icons (rejected — that is the current cluttered state) and a `TopAppBar` overflow built via `MenuAnchor`/exposed dropdown (not needed — a simple `DropdownMenu` is sufficient and idiomatic for two static items).

### D2: Empty top bar title

Set `title = {}` in the `TopAppBar` so no client name is repeated. The content body remains the single source of the name display.

Rationale: removes the duplicated information the user called out. A neutral title (e.g., "Detalhes") was considered but adds no value since the back arrow already conveys the navigation context; an empty title is the cleanest result.

## Risks / Trade-offs

- [Discoverability of edit/delete decreases by one tap] → Accepted trade-off requested by the user; the menu icon is a universally understood affordance.
- [Menu state lost on recomposition] → `menuOpen` is local `remember` state scoped to the top bar actions; recompositions driven by `state.cliente` flows preserve it.

## Migration Plan

Single-file UI change, no migration or rollback concerns.

## Open Questions

None.
