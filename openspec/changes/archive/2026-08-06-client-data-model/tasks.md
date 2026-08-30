## 1. Schema

- [x] 1.1 Change `Cliente.id` and `Foto.id` to `String` (UUID), generated on the device
- [x] 1.2 Add `updatedAt: Long` to `Cliente`, initialized to creation and refreshed on update
- [x] 1.3 Add pending/delete-pending boolean markers to `Cliente` (and delete-pending to `Foto`) 
- [x] 1.4 Add photo metadata (`mime`, `ordem`) needed by the server payload contract
- [x] 1.5 Bump Room version and add a `Long`→`String(UUID)` migration that preserves all rows

## 2. DAO

- [x] 2.1 Add DAO queries: pending clients and pending deletions
- [x] 2.2 Add DAO method to mark a client pending on save
- [x] 2.3 Add DAO method to clear pending after server ack
- [x] 2.4 Ensure list/read queries ignore records that are pending deletion

## 3. Repository & call sites

- [x] 3.1 Update `ClienteDraft`/`ClienteRepositoryImpl` to produce String ids and maintain `updatedAt` and pending markers on save
- [x] 3.2 Update any UI/ViewModel that reads client id as the old type to use its stable string

## 4. Tests & verification

- [x] 4.1 Room migration test from version 3 preserves existing clients/photos
- [x] 4.2 Entity/DAO tests: new client pending, edit refreshes `updatedAt` and keeps pending, ack clears pending
- [x] 4.3 Unit test: pending-deletion records are hidden from normal list reads
- [x] 4.4 Run Android unit tests