## Context

The client edit-log view (`ClientDetailScreen`'s `LogEntries`) formats entry timestamps with `DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)`. That factory produces a locale-dependent format: on English locales the time is 12-hour with an AM/PM marker (e.g. "5:11 AM"). The change makes the log always use a 24-hour clock.

## Goals / Non-Goals

**Goals:**
- Log entry timestamps show 24-hour time on every device, independent of locale.

**Non-Goals:**
- Changing the date portion's ordering or the day/month naming beyond what a fixed pattern implies.
- Changing any other screen's time formatting (none currently exists).

## Decisions

**D1 — Use an explicit 24-hour pattern.**
- Replace `DateFormat.getDateTimeInstance(MEDIUM, SHORT)` with an explicit `SimpleDateFormat` using a 24-hour pattern, e.g. `dd/MM/yyyy HH:mm`.
- *Rationale*: a fixed pattern guarantees `HH` (24-hour) regardless of locale; the factory cannot guarantee that.
- *Alternative considered*: `DateFormat.getTimeInstance(DateFormat.SHORT, Locale...)` with a forced 24-hour locale — rejected as more convoluted than a literal pattern.

**D2 — Locale-aware day/month names.**
- The date pattern keeps `dd/MM/yyyy` (numeric day/month) so no locale-dependent names are involved; if names were used, they would come from `Locale.getDefault()` for the day/month while the clock stays `HH:mm`.
- *Rationale*: numeric day/month avoids ambiguity and keeps the change minimal.

**D3 — Files touched.**
- `ClientDetailScreen.kt` (`LogEntries` formatter). No server or schema change.

## Risks / Trade-offs

- [Fixed numeric date ignores locale ordering conventions] → Acceptable for this scope; the request is specifically about the clock format.

## Migration Plan

None — app-only change.

## Open Questions

None.
