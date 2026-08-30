## 1. Fix

- [x] 1.1 Add `<uses-permission android:name="android.permission.INTERNET" />` to `app/src/main/AndroidManifest.xml`

## 2. Verification

- [x] 2.1 Run `./gradlew assembleDebug lintDebug` and confirm a green build
- [x] 2.2 Install on the emulator and confirm the maps on the form, fullscreen picker and detail screen load map tiles instead of rendering blank
