## 1. Dependency and storage plumbing

- [x] 1.1 Add `androidx.exifinterface:exifinterface` to `app/build.gradle.kts` dependencies
- [x] 1.2 Change `Foto.mime` default from `image/jpeg` to `image/webp` in `data/local/Foto.kt`
- [x] 1.3 Update `PhotoStore.storeBytes` (interface in `sync/SyncStatus.kt`, impl `sync/FilePhotoStore.kt`) to accept a MIME argument and derive the file extension (`.webp`/`.jpg`) from it instead of always writing `.jpg`
- [x] 1.4 Update `PhotoMaterializer.materialize` to pass `server.mime` through to `storeBytes`

## 2. Shared photo encoder

- [x] 2.1 Create a `PhotoEncoder` component that, given a `Uri` or `File`, decodes with `inSampleSize`, applies EXIF rotation, downscales to a 1800px maximum side (no upscaling), and encodes WebP lossy (~82)
- [x] 2.2 Make encoding failures non-destructive: recycle bitmaps, delete partial output, and leave the source intact (throw so no broken photo is attached)

## 3. Gallery path

- [x] 3.1 Replace the inline `decodeScaledBitmap` logic in `ClienteRepositoryImpl.stagePhoto` with `PhotoEncoder`, writing `.webp` and `mime = image/webp`

## 4. Camera path

- [x] 4.1 Route the captured file through `PhotoEncoder` in `ClientFormViewModel.onPhotoCaptured` (or the repository) before attaching it, so the camera no longer stores the full-resolution JPEG

## 5. Tests and verification

- [x] 5.1 Add unit tests for `PhotoEncoder`: downscaling, no upscaling, EXIF rotation, WebP output, and non-destructive failure
- [x] 5.2 Update existing tests/fixtures that assume `image/jpeg` (e.g. `PhotoMaterializerTest`, `SyncCoordinatorTest`, `ApiClientTest`) to the new MIME expectations
- [x] 5.3 Run the Android build, lint, and unit tests; confirm the server is unaffected (no server changes)
