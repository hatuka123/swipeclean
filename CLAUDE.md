# CLAUDE.md

## Who works on this repo
The owner is not a programmer. Explain everything and ask questions in **simple Hebrew**.

## The app: SwipeClean (ניקוי בהחלקה)
Swipe through the photo/video gallery: right = keep, left = mark for deletion (goes to the in-app bin),
up = move to a folder. Daily cleanup plan with reminders. Personal use first, Google Play later, so
only Play-compliant storage APIs: MediaStore + createTrash/Delete/WriteRequest. **Never** add
MANAGE_EXTERNAL_STORAGE, INTERNET or exact-alarm permissions (CI checks the merged manifest).

Modules:
- `:core` – pure Kotlin logic (no Android): bucket aggregation, access resolution, and later deck
  building, undo stack, schedules, streaks. Put business logic here, never in Composables.
- `:app` – `com.hatuka.swipeclean`: `data/media` (MediaStore), `data/db` (Room), `data/settings`
  (DataStore), `permissions`, `ui/*` (Compose screens), `reminders`, `di` (Hilt).

Key rules:
- Swipes only write to Room. Files change only through a system confirmation dialog
  (trash/delete from the bin, batched moves).
- Every decision row is permanent (tombstone `DELETED` after deletion), so reviewed items never
  reappear unless the user resets a folder.
- Swipe directions are physical in every language (right = keep even in Hebrew/RTL).
- All strings in `res/values` + `res/values-iw`.

## How every change is tested (no Android SDK on the owner's PC)
1. Work on a branch (`phase-N` or a feature branch). `.github/workflows/verify.yml` runs build, lint,
   unit tests, Robolectric screenshots (artifact `screens`) and emulator tests on API 30/33/34
   (artifacts `emulator-apiNN`, script `.github/scripts/emulator-tests.sh`).
2. Merge to `main` only when verify is green. `android-build.yml` builds the signed release APK,
   creates a GitHub Release and sends it to the owner's Telegram.

## Pipeline rules
- Never delete or weaken the workflows without explicit approval.
- Never change `applicationId` (`com.hatuka.swipeclean`) or the signing key – updates would stop
  installing over the existing app. The key lives only in GitHub Secrets
  (`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`); it is also the future
  Play upload key, so it must never be committed.
- `versionCode` comes from `-PversionCode` (CI run number). Do not hard-code it.
- Builds use plain `gradle` pinned in the workflows (9.6.0, AGP 9.4.1). Keep them compatible.
- Never put tokens, passwords or chat IDs in code, commits or logs.
