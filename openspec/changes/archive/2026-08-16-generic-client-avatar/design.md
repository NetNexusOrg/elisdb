## Context

The client list row (`ClientListScreen.ClientListItemRow`) renders a 48dp rounded-square thumbnail only when `primeiraFotoUri` is non-null; rows without a photo show just the name and no image. This makes the list visually inconsistent. The change fills the empty slot with a generic profile image.

## Goals / Non-Goals

**Goals:**
- Every client row shows a square thumbnail in the same position: the photo for clients with photos, a generic profile image otherwise.

**Non-Goals:**
- Changing the photo thumbnail rendering for clients that have a photo.
- Applying the generic image to other screens (detail, edit form).

## Decisions

**D1 — Use a Material icon for the generic avatar.**
- Render `Icons.Default.Person` inside the same 48dp rounded-square slot, tinted with `onSurfaceVariant` on a subtle `surfaceVariant` background (matching the app's neutral look).
- *Rationale*: the project already depends on `material-icons-extended`, so no new drawable assets or network resources are needed; the icon is available and consistent with the rest of the Material UI.
- *Alternative considered*: a bundled PNG drawable — rejected because the icon approach is lighter and theme-consistent.

**D2 — Restructure the row's leading slot.**
- Extract the leading 48dp box so it always renders: `AsyncImage` when `primeiraFotoUri != null`, else the `Person` icon. The existing `Spacer` after the image stays only when the leading slot is present (always now).
- *Rationale*: keeps row alignment identical for photo and no-photo clients.

**D3 — Files touched.**
- `ClientListScreen.kt` (`ClientListItemRow`). No view-model, server, or schema change.

## Risks / Trade-offs

- [Visual weight of the icon for a long list] → Minor; tinted neutral so it stays unobtrusive.

## Migration Plan

None — app-only change.

## Open Questions

None.
