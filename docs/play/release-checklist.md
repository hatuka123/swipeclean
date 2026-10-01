# Google Play release checklist

Status legend: ✅ done in the app / CI · ⬜ to do in Play Console (owner) · ❓ decision needed

## App and build
- ✅ `applicationId` `com.hatuka.swipeclean`, fixed forever.
- ✅ targetSdk 37 (Play requires 36+ for new apps from Aug 2026).
- ✅ Release build shrunk with R8 (`isMinifyEnabled`, `isShrinkResources`); CI installs the shrunk
  build (`releaseCheck`, same code, debug key) on API 30/33/34 and checks that it starts, opens the
  swipe screen from a reminder link and does not crash.
- ✅ Signed with the upload key from GitHub Secrets; `versionCode` = CI run number.
- ✅ Merged manifest checked by CI: no INTERNET, no all-files access, no exact alarms.
- ✅ English and Hebrew (RTL) strings; screenshots in both languages in CI.
- ✅ `versionName` is `1.0.<build>` (only the name shown to users; updates still install).
- ✅ Every build on `main` attaches the **Android App Bundle** (`SwipeClean-build-N.aab`, what Play
  accepts) to its GitHub Release, next to the APK; Telegram still gets the APK.

## Play Console (owner)
1. ⬜ Developer account (one-time US$25). New personal accounts must run a **closed test with at
   least 12 testers for 14 days** before production access.
2. ⬜ Create the app: name, default language, "App", "Free".
3. ⬜ **Play App Signing**: keep Google-managed signing and register our key as the **upload key**
   (the key in GitHub Secrets). Never lose it; if lost, Google can reset the upload key.
4. ⬜ Monetization → Products → In-app products: create three **consumable** products with the IDs
   `tip_small`, `tip_medium`, `tip_large` (suggested ₪5.90 / ₪14.90 / ₪29.90) and activate them.
   Requires a payments (merchant) profile. Donations can only be tested from a Play-installed build
   (internal or closed testing track with a license tester); the Telegram APK shows them as unavailable.
5. ⬜ App content (answers in `data-safety.md`): privacy policy URL, ads (none), app access (no
   login), content rating, target audience, data safety, **photo and video permissions
   declaration**.
6. ⬜ Store listing (texts in `store-listing.md`), icon 512×512, feature graphic, screenshots.
7. ⬜ Upload the `.aab` to the closed testing track, add testers, wait 14 days, then apply for
   production.

## Privacy policy hosting
❓ Play needs a **public URL**. The repository is private, so the file in `privacy-policy.md` must be
published somewhere public, for example a free Google Sites page or a public GitHub Gist.
