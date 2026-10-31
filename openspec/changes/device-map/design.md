# Design

## Context

The client record already has a binary attachment pipeline that works: files on
disk named by id, rows in `fotos` holding `mime`/`ordem`, content hash computed
on read, push/pull lists with `missing_photos`, atomic writes, and a garbage
collector that removes files without a row. The new field needs the same
guarantees for two new file types — images (already handled) and PDFs (which the
image encoder cannot touch).

The app's photo pipeline is image-specific in exactly two places: `PhotoEncoder`
(decodes and re-encodes to WebP) and `PhotoViewerScreen` (renders with
`AsyncImage`). Everything else — storage, hashing, upload, download,
materialization, sweep — is content-agnostic.

## Decisions

### One attachment store, two kinds

Documents are rows in the existing `fotos` table with a new `kind` column
(`'photo' | 'document'`, default `'photo'`). The table becomes the attachment
table in practice, but keeps its name so nothing that already works has to
change.

A separate `documentos` table would duplicate `write_photo`, `get_photo`,
`_foto`, `_remove_file`, the path containment check, the garbage collector, the
cascade delete and the hash-on-read logic — six opportunities to diverge from
the photo path for no gain. With `kind`, the great majority of the server code
stays untouched; only the "which list does this belong to" split is new, and the
garbage collector keeps working without knowing that kinds exist.

### Two lists on the wire, never one mixed list

`push` and `pull` carry `fotos` and `documentos` separately, each item with
`id`, `mime`, `ordem` (+ `nome` and `hash` on pull). The app's gallery, pager
and thumbnail widgets keep seeing only images; the document list never reaches
`PhotoViewerScreen`.

### Images are re-encoded, PDFs are copied verbatim

An image added to the map goes through the existing `PhotoEncoder` (max 1800 px,
WebP quality 82). This keeps uploads small on mobile data and reuses code that
already rotates by EXIF and never leaves a half-written file. A PDF cannot be
decoded, so it is copied byte for byte — a re-encode would only corrupt it.

### The same 20 MB ceiling as photos

`require_auth` rejects any request body over `ELISDB_MAX_PHOTO_BYTES` (20 MB)
before it is read, so a larger document limit would need a second code path in
the authentication dependency for no practical gain: a device map or a datasheet
fits well under 20 MB. The app rejects the file before staging it and says so,
instead of failing later during the sync.

### Mime allowlist

The server accepts `image/jpeg`, `image/png`, `image/webp` and `application/pdf`
for documents (and only the image types for photos). Anything else rejects the
record with `status="error"`. Without this, `GET /api/documents/{id}` would
serve whatever type a client chose — the stored-XSS path already noted in the
photo endpoint.

### Same directory, same sweep, one FileProvider entry

Document files live in the same `filesDir/photos` directory as photos (opaque
names, now prefixed `file_`), so the sweeper, the guard mutex and the "file
written before the row" ordering stay exactly as they are. `file_paths.xml`
gains a `<files-path>` for that directory so a PDF can be handed to an external
reader with a temporary read grant; without it there is no way to open a PDF,
since the app does not render them.

### Sync reuses the content-identity rules

An attachment is uploaded only when its SHA-256 differs from the last uploaded
hash for that id (the existing `PhotoPushState`, keyed by id — ids are globally
unique). A document the server could not find is reported back as
`missing_documents` and its hash is not recorded, so it is retried on the next
sync. On pull, a document whose hash, mime and order match is kept without a
download; otherwise it is fetched and stored atomically.

### History records document changes

A push that only adds a document would otherwise produce no audit entry
(`build_audit_details` returns nothing when nothing it knows about changed), so
the details JSON gains a `documents` object with the names added and removed,
and the client's history screen shows that line. It is additive: entries written
before this change simply have no `documents` key.

### Server before app

If the app reached a server that predates this change, that server would drop
the unknown `documentos` key and answer without `missing_documents` — the app
would take the ack, clear `pending`, and the documents would silently never
upload. The deploy order therefore matters: server first (as with `delivery_ts`
in 1.6.1), app afterwards. The docs say so explicitly.

## Risks / trade-offs

- **One table, two meanings.** `fotos` now holds documents; a query that forgets
  the `kind` filter would show a PDF in the gallery. Mitigated by keeping the
  filter in the two places that build the lists (`_canonical_from_conn`,
  `_replace_fotos`) and by tests that assert the two lists never mix.
- **PDFs are never validated as PDFs.** The server trusts the declared mime and
  the magic bytes are not checked; a renamed file would open as a broken PDF in
  the external reader. Acceptable: the user picked the file and only reads it.
- **No in-app PDF preview.** Opening with an external app means the experience
  depends on what is installed; without a reader the app shows a message
  instead of a viewer.
- **Order is insertion order.** Reordering would need a UI and a `ordem` bump;
  deferred until someone asks.
- **Older clients** simply do not see the documents (unknown keys ignored); an
  edit from an old client does not delete them, because its push carries no
  `documentos` list.

## Alternatives considered

- **A `documentos` table with its own endpoints.** Cleaner names, but duplicates
  the storage, hashing, GC and cascade logic described above.
- **A single generic `anexos` table replacing `fotos`.** The right end state,
  but it forces a rewrite of every photo path (and its tests) to deliver a
  feature that only adds a second kind.
- **Base64 inside the push body.** Simple to wire, but it inflates the payload by
  a third, buffers whole files in memory, and gives up the existing
  upload/download endpoints with their per-file retry.
- **A `notas`-style text field holding a path or URL.** No sync, no offline
  copy, and the file would not travel to the other devices.
- **Rendering PDFs in a WebView.** Google's PDF viewer is not part of the
  platform API, and a WebView cannot display PDFs reliably across devices.
