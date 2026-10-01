# Play Console answers: Data safety, permissions, content rating

Answers for the forms in Play Console → App content. Based on the merged release manifest checked
by CI (`.github/scripts/check-manifest.sh`): no INTERNET, no MANAGE_EXTERNAL_STORAGE, no exact
alarms, `allowBackup="false"`.

## Data safety

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (nothing is collected or transmitted) |
| Do you provide a way for users to request that their data is deleted? | Not applicable (no data leaves the device; uninstalling removes everything) |

Why "No": photos and videos are read on the device only to display them; decisions, plan, settings
and stats stay in app-private storage. Google's definition of "collected" is data transmitted off
the device, and the app has no network access at all.

## Sensitive permissions

### Photo and video permissions (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO)
Play requires a declaration that broad media access is part of the app's core functionality.

- **Core functionality**: SwipeClean is a gallery cleaner. It shows every photo and video in the
  folders the user picks, one after another, so the user can keep, delete or move each one, and it
  tracks which items were already reviewed so they do not come back. This needs persistent access to
  the whole library; a one-time photo picker cannot list folders, show counts and sizes, or resume
  where the user stopped.
- **Partial access**: on Android 14+ the app also works with "selected photos only" access
  (READ_MEDIA_VISUAL_USER_SELECTED) and offers to pick more.
- A short screen recording is usually requested: onboarding → allow access → folder list → swipe.

### Notifications (POST_NOTIFICATIONS)
Asked for only when the user turns on the daily cleanup plan. Used for the reminder only.

### Not requested
No exact alarms, no foreground service, no all-files access, no location, no contacts, no camera.

## Ads and in-app purchases
- Ads: **No**. The app shows no ads.
- In-app purchases: **Yes** – optional donations (consumable tips) through Google Play Billing.
  Payment data is handled by Google Play, not by the app, and the app keeps no purchase records
  (each tip is consumed right away).

## Content rating (IARC questionnaire)
- Category: **Utility, productivity, communication or other**.
- Violence, sexuality, language, controlled substances, gambling: **No** to all.
- Does the app allow users to interact or exchange content? **No**.
- Does the app share the user's location? **No**.
- Does the app allow purchases of digital goods? **Yes** (optional donations through Google Play).
- Expected rating: **Everyone / PEGI 3**.

## Target audience and content
- Target age groups: **18 and over** (simplest; the app is not designed for children).
- Ads: **The app does not contain ads**.
- In-app purchases: optional donations only; nothing in the app is locked behind them.
- Government app, financial features, health, news: **No**.
