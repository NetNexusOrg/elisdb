# Tasks

## 1. Servidor

- [x] 1.1 Add a `kind` column (`'photo' | 'document'`, default `'photo'`) to the
      attachment table through an idempotent migration, plus an index on
      `(cliente_id, kind)`.
- [x] 1.2 Add `DocumentoIn`/`DocumentoOut`, the `documentos` and
      `missing_documents` fields on the push/pull models, and a mime allowlist
      (images + `application/pdf`) enforced per record.
- [x] 1.3 Extend `save_client` and `_replace_fotos` with the document list,
      keeping the two kinds in separate lists in `_canonical_from_conn`.
- [x] 1.4 Record added/removed document names in the audit `details`
      (`documents` object) so a documents-only edit still produces a history
      entry.
- [x] 1.5 Add authenticated `PUT`/`GET /api/documents/{id}` (204/200/404, 413
      over the size ceiling, `kind` checked so a photo cannot be fetched as a
      document).
- [x] 1.6 Confirm the client delete path removes document files after the
      commit (same list as photos) and that the garbage collector keeps
      documents that are referenced.
- [x] 1.7 Server tests: push/pull roundtrip with an image and a PDF, kinds never
      mix, mime allowlist rejects a record, missing file reported, size ceiling,
      401 without signature, 404 for a wrong kind, delete removes the files,
      audit records the document diff.

## 2. Android — dados

- [x] 2.1 Add `data/local/Documento.kt` (`id`, `clienteId`, `nome`, `mime`,
      `uriLocal`, `ordem`, `deletePending`) with the cascade FK and the
      `clienteId` index, a `DocumentoDao`, and `MIGRATION_9_10` (table + index);
      bump the database to 10 and export the schema.
- [x] 2.2 Extend `ClienteComFotos` with the `documentos` relation and cover it
      in the existing observe/get queries.
- [x] 2.3 Repository: `stageDocument(uri, nome)` — images through
      `PhotoEncoder`, PDFs copied byte for byte — and include the document list
      in `save`, with files deleted only after the commit.

## 3. Android — sincronização

- [x] 3.1 Add `DocumentoInDto`/`DocumentoOutDto` and the `documentos` /
      `missing_documents` fields on `RecordDto`, `ClienteComFotosDto` and
      `PushResultDto`.
- [x] 3.2 Add `uploadDocument`/`downloadDocument` to `SyncApi` and `ApiClient`
      (signed, same shape as the photo calls).
- [x] 3.3 Upload only documents whose content hash changed, keep the hashes of
      the ones the server reported missing, and clear `pending` as today.
- [x] 3.4 Materialize documents on pull (keep when hash, mime and order match),
      upsert the rows in the same transaction, and reassign them on a duplicate
      merge.
- [x] 3.5 Extend the orphan sweep to document files (same store, same guard).

## 4. Android — interface

- [x] 4.1 Add the "Mapa de dispositivos" section to the client form: **Ficheiro**
      (`OpenDocument` with `image/*` and `application/pdf`) and **Câmara**, a
      list of the chosen attachments with name and type, remove, and an
      "unsupported/too large" message.
- [x] 4.2 Add the "Mapa de dispositivos" card to the client detail: images open
      the existing viewer, PDFs are handed to an external reader, and a file
      that is no longer on disk is reported instead of opening nothing.
- [x] 4.3 Expose the attachment directory through `file_paths.xml` so the PDF
      can be shared with a read grant; explain the choice in a comment.
- [x] 4.4 Show the document line (added/removed) in the client history screen.

## 5. Verificação

- [x] 5.1 `pytest` green, including the new document tests.
- [x] 5.2 Unit tests: repository staging (image vs PDF), push skip/missing,
      pull materialization, merge reassignment, form view model add/remove.
- [x] 5.3 Instrumented tests: `MIGRATION_9_10` keeps existing clients and photos;
      the form and detail sections render a PDF and an image.
- [x] 5.4 `:app:testDebugUnitTest`, `:app:lintDebug` and the instrumented run
      green.
- [ ] 5.5 End-to-end on the emulator against a local server: add an image and a
      PDF, save, sync, confirm both arrive in the database and in the files
      directory, reopen the client, open the PDF with an external reader.
- [x] 5.6 `openspec validate device-map --strict`.

## 6. Documentação

- [x] 6.1 `elisdb-android/docs/uso.md`: what the field is for, how to add and
      remove an attachment, what happens when the file type is not accepted.
- [x] 6.2 `elisdb-server/docs/configuracao.md`: the `documentos` contract, the
      endpoints, the allowlist and the 20 MB ceiling, and the warning that the
      server must be updated before the app.
- [x] 6.3 `CHANGELOG.md`: section for the next version at release time (this is
      a feature, so a minor bump).
