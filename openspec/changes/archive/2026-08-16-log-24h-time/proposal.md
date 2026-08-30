## Why

The client "Log" view formats timestamps with `DateFormat.getDateTimeInstance(MEDIUM, SHORT)`, which uses the device locale. On English-locale devices the time shows in 12-hour AM/PM format (e.g. "5:11 AM"), which is inconsistent with how the rest of the app presents time and can be ambiguous. The log timestamps should always use a 24-hour clock regardless of locale.

## What Changes

- The client edit-log view formats its entry timestamps with a 24-hour clock (e.g. "17:11") regardless of the device locale, while keeping a readable date.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the Log view presents entry timestamps in 24-hour time.

## Impact

- **App**: `ClientDetailScreen` (`LogEntries`) — replace the locale-default formatter with an explicit 24-hour `SimpleDateFormat` (e.g. `dd/MM/yyyy HH:mm`), localised only in the day/month names, not the clock.
- **Behavior**: log timestamps show 24-hour time on every device, independent of locale.
- **Tests**: app unit/instrumentation test asserting the formatted time uses 24h (no AM/PM).
