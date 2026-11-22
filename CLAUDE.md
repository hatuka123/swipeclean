# CLAUDE.md

## Who works on this repo
The owner works **only from an Android phone** and is not a programmer. Explain in **simple Hebrew**.
The app is for a woman friend of the owner (feminine wording in the UI).
This repo used to hold SwipeClean (ניקוי בהחלקה); the owner retired it. Its last state is tag `swipeclean-final`.

## Build and test (no Android SDK locally – CI is the only build machine)
- Work on `dev`: `.github/workflows/verify.yml` builds, lints and runs unit tests. Merge to `main` only when green.
- Push to `main`: `android-build.yml` (Gradle 8.11.1, JDK 17, no wrapper) builds the release APK with
  `-PversionCode=<run_number>`, signs it with the key from GitHub Secrets (`SIGNING_KEYSTORE_BASE64`,
  `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` – the old SwipeClean key), creates Release `build-N`
  and sends the APK to the owner's Telegram. Failures go to Telegram with the last 40 log lines.
- Never change `applicationId` (`com.hatuka.income`) or the signing secrets; versionCode only from `-PversionCode`.
- `notify.yml` sends a text message to the owner's Telegram (`gh workflow run notify.yml -f message=...`).

## The app: ההכנסות שלי
Kotlin + Compose, Hebrew only, forced RTL, light cream/green theme (no purple). No network, no account.
State is JSON in SharedPreferences (`data/Store`). Crashes go to `CrashLog` and show with a copy button.

Tabs (bottom bar): עבודות · רישום · סיכום · מס.
- `ui/JobsScreen`: workplaces – name, שכירה/עצמאית, payroll-system link (employee only, opens the browser), colour.
- `ui/EntriesScreen`: work entries (job, from–to dates, days – follow the dates until typed, amount ₪, note)
  and one-off incomes (title, date, amount, optional job – e.g. a bonus counts as that job's salary). Grouped by month.
  A work entry's income belongs to the month of its end date.
- `ui/SummaryScreen`: month / year / all-time totals, per job (days, ₪/day average), one-offs, by month.
- `ui/TaxScreen`: tax-coordination alert computed from the data (`Totals.multiEmployerMonths`), monthly
  deductions calculator (`data/Tax`, 2026 rules: brackets 10/14/20/31/35/47/50 with the widened 20% ≤19,000 and
  31% ≤25,100, credit point 242, BL+health 4.27% ≤7,703 / 12.17% ≤51,910), with/without-coordination comparison
  (second employer 47%), self-employed rough estimate, FAQ. Credit points (default 2.75) and pension % are settings.
  **Update `data/Tax` every January** with the new year's numbers.
