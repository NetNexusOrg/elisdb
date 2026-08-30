## Context

The sync action (an `IconButton` in `ui/settings/SyncStatusBar.kt`) sits in a full-width `Row`. When the sync state is `IDLE` the row contains only the button, so it packs to the left; as soon as a status message appears (`IN_PROGRESS`, `SUCCESS`, `ERROR`) a weighted `Text(weight(1f))` is inserted before the button and pushes it to the right (see proposal.md — Why). This is the reported position instability.

## Goals / Non-Goals

**Goals:**
- The sync action occupies the same position in the row in every sync state (IDLE, IN_PROGRESS, SUCCESS, ERROR).
- The status message and icon never displace the button.
- Behavior, messages, and sync semantics are unchanged.

**Non-Goals:**
- Changing the status messages or icons.
- Moving the sync action to a different screen or top bar.
- Fixing the stale sync-status scenarios in `client-screens/spec.md` (unrelated cleanup).

## Decisions

**D1 — Give the sync action a reserved, stable slot at the row's end.**
- Restructure the `Row` so the message area (icon + text) and the action have fixed roles that do not depend on the message being present:
  - Always reserve a trailing slot for the `IconButton` (the sync action), and place the message content in a separate leading segment.
  - The message segment, when present, SHALL be confined so it cannot push the action (e.g. its text uses a bounded width / weight within its own segment, not the whole row).
- *Rationale*: the action's slot is structurally fixed regardless of whether a message is rendered; there is no conditional insertion before it.
- *Alternative considered*: pin the button to the left always (button first, message after) — rejected because the message text could then grow and the button stays put but the visual order changes across states; the end-slot is the least surprising and matches the "action at the edge" convention.

**D2 — Keep the message area stable by always allocating its segment.**
- Render the leading segment unconditionally: when there is no message (IDLE) it is simply empty, occupying the remaining width, so the trailing action stays at the row end in every state.
- *Rationale*: an always-present segment makes the layout identical across states; the only change is whether the segment has content.
- *Alternative considered*: conditionally rendering only the button with a fixed-size container — rejected because fixed-size widths are brittle across text scales and font sizes.

**D3 — In-progress indicator stays in the action slot.**
- The `CircularProgressIndicator` already swaps into the `IconButton` content during sync; with D1 the slot is fixed, so the spinner appears exactly where the action icon is.
- *Rationale*: no extra work; the existing swap is sufficient once the slot is stable.

**D4 — Tests assert position, not just visibility.**
- Extend `SyncStatusBarTest.kt` to assert the sync action's horizontal position is the same across IDLE, IN_PROGRESS, SUCCESS and ERROR (e.g. via `assertIsDisplayed` plus a position/alignment check such as `getBoundsInRoot()` or `assertLeftPositionInRootIsEqualTo`).
- *Rationale*: the existing tests only assert visibility (see exploration), so the regression would pass today.

## Risks / Trade-offs

- [Fixed slot looks like the button is far from the message on wide rows] → Acceptable; the row is narrow on the settings screen and the trailing action placement is standard.
- [Text scaling makes the message push into the action slot] → The message text is placed in its own bounded segment (e.g. `weight(1f)` within the leading segment only), so the action never moves.
- [Instrumentation position assertions are flaky on small screens] → Use relative alignment checks (action to the right of the message, and unchanged left edge across states) rather than absolute pixel values.

## Migration Plan

1. Rework `SyncStatusBar.kt` row layout per D1/D2.
2. Extend `SyncStatusBarTest.kt` with position assertions across all four states.
3. Run instrumentation tests; verify on the emulator by opening Settings (IDLE), tapping sync (IN_PROGRESS/SUCCESS) and confirming the button does not move.
4. Rollback: revert `SyncStatusBar.kt` and the test additions — no data or API impact.

## Open Questions

None.