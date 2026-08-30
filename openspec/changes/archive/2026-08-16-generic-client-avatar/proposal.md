## Why

In the client list, a row for a client without photos shows only the name — the row has no image at all, which makes clients with and without photos look inconsistent and harder to scan. A generic profile image shown for clients without a photo makes the list visually uniform and signals "this client has no photo yet" without needing to read the name.

## What Changes

- The client list row shows a generic profile image (e.g. a person icon on a neutral background) for clients that have no photo, in the same size and shape as the photo thumbnail, instead of leaving that slot empty.
- Clients with a photo keep showing their photo thumbnail.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the client list row shows a generic profile image for clients without a photo.

## Impact

- **App**: `ClientListScreen` (`ClientListItemRow`) — when `primeiraFotoUri` is null, render the generic avatar instead of nothing. Uses an existing Material icon (`Icons.Default.Person`) or a drawable resource.
- **Behavior**: every client row shows a square thumbnail in the same position; no-photo clients show the generic profile image.
- **Tests**: instrumentation test asserting a no-photo client row shows the generic avatar and a photo client shows the photo.
