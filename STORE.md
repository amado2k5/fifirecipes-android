# Google Play submission — FiFi Recipes 1.0 (new app)

Everything Play Console asks for, in the order it asks. Files referenced here
are in this repo unless noted.

## 0. Before you start

| Need | Notes |
|------|-------|
| Play Console developer account | https://play.google.com/console/signup — US$25 one-time, identity verification (and a D-U-N-S number for an **organization** account). |
| Testing requirement | **Personal** accounts created after 13 Nov 2023 must run a **closed test with at least 12 opted-in testers for 14 continuous days** before Production access unlocks. Organization accounts are exempt. Plan for this two-week window. |
| Developer contact email | Shown publicly on the listing. The site uses **android@fifi.cooking** — create that mailbox (or change it in `site/support.html` + `site/privacy.html`). |
| Upload key | `fifi-upload.jks` + `keystore.properties` in the repo root (gitignored, **back them up**). SHA-256 `EC:E5:11:84:2E:DC:25:B7:F6:7E:94:FF:55:8B:37:16:50:8C:09:A5:CD:DE:BB:93:64:BA:F7:BC:21:5C:4C:78` |

## 1. Create app

| Field | Value |
|-------|-------|
| App name | **FiFi Recipes** |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | Developer Program Policies ✔ · US export laws ✔ |

## 2. App bundle & signing

- Upload **`app-release.aab`** (Play only accepts App Bundles for new apps;
  the `.apk` is for sideloading/testing). Build: `./gradlew :app:bundleRelease`
  → `app/build/outputs/bundle/release/app-release.aab`.
- Package `cooking.fifi.android` · versionCode 1 · versionName 1.0 ·
  minSdk 26 · targetSdk 37 (meets and exceeds Play's target-API requirement).
- Accept **Play App Signing** (default). Google generates the app signing key;
  you keep the upload key.
- **Then copy the App signing key certificate SHA-256** (Test and release ›
  App integrity › App signing) into `ANDROID_CERT_SHA256` in
  `fifirecipes/scripts/generate-public-index.ts` and merge — otherwise
  fifi.cooking links won't auto-open in Play-installed copies of the app.
- Release notes (1.0):
  > First release: 2,300+ family recipes from Egypt and around the world from Dr. Fatma in 25 languages, a Kids cooking mode, recipe videos, and layouts for phones, foldables, tablets and Chromebooks.

## 3. Main store listing

| Field | Where |
|-------|-------|
| Short description (≤ 80) | `store/listing/en-US.txt` (73 chars) |
| Full description (≤ 4000) | `store/listing/en-US.txt` |
| Arabic translation | Add translation › Arabic – ar: `store/listing/ar.txt` |
| App icon 512×512 PNG | `store/icon-512.png` |
| Feature graphic 1024×500 | `store/feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | `store/screenshots/phone/*.jpg` — 8 × 1080×1920 (9:16) |
| 7-inch tablet screenshots | `store/screenshots/tablet-7/*.jpg` — 8 × 1080×1920 |
| 10-inch tablet screenshots | `store/screenshots/tablet-10/*.jpg` — 8 × 2560×1440 (16:9) |
| Chromebook (optional) | reuse the 10-inch set |
| Video (optional) | leave empty (or a YouTube URL of an app walkthrough) |

**Store settings**

| Field | Value |
|-------|-------|
| App category | **Food & Drink** |
| Tags | Recipes, Cooking, Food & Drink (pick the closest 5 offered) |
| Email | android@fifi.cooking |
| Website | https://android.fifi.cooking |
| Phone | optional — leave blank |
| External marketing | allowed |

## 4. App content (Policy › App content)

| Section | Answer |
|---------|--------|
| **Privacy policy** | https://android.fifi.cooking/privacy.html |
| **App access** | All functionality is available without special access (no login). |
| **Ads** | No, the app does not contain ads. |
| **Content rating** (IARC questionnaire) | Category: *Reference, News, or Educational*. Violence, sexuality, language, controlled substances, gambling: **No** to all. User interaction / sharing user content: **No** (the Share button only hands a public link to the system share sheet). Shares user location: **No**. Digital purchases: **No**. Unrestricted internet / web browsing: **No** — content is curated JSON from fifi.cooking plus YouTube embeds. Expected result: **Everyone / PEGI 3 / USK 0**. |
| **Target audience and content** | Recommended: **18 and over** (optionally also 13–15 and 16–17). The app is a family cookbook for adults; Kids mode is a feature for cooking *with* a grown-up. When asked whether the app could unintentionally appeal to children, answer honestly: the Kids-mode illustrations are child-friendly. If Google's review decides it is child-appealing, it will ask you to join the **Families program** — the app already meets its core rules (no ads, no data collection, no third-party SDKs, no account, Everyone rating). |
| **News app** | No |
| **COVID-19 contact tracing/status** | No |
| **Data safety** | See below |
| **Government app** | No |
| **Financial features** | None |
| **Health apps** | Not a health app (nutrition figures are recipe estimates) |
| **Advertising ID** | No — the app does not use the advertising ID (the manifest declares no `AD_ID` permission). |

### Data safety form

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | (not asked when nothing is collected; all traffic is HTTPS) |
| Do you provide a way for users to request that their data is deleted? | (not asked when nothing is collected) |

Result on the listing: **"No data collected · No data shared with third parties"**.
Basis: the only permission is `INTERNET`; no analytics/crash/ads SDKs; the
only on-device storage is the language preference and HTTP/image caches. The
optional in-app YouTube player is Google's own embed loaded from
youtube-nocookie.com at the user's request — disclosed in the privacy policy.

## 5. Testing tracks

1. **Internal testing** (instant, up to 100 testers): upload the AAB, add your
   own Google account, install from the opt-in link, sanity-check.
2. **Closed testing** (required for new personal accounts): create a track,
   add ≥ 12 testers (a Google Group or email list), keep them opted in for 14
   days. Pre-launch report runs automatically on Google's device lab.
3. **Production**: after the 14 days, *Apply for production* (a short
   questionnaire about your testing), then promote the release. Review usually
   takes a few days for a new app.

Countries/regions: all available (recipe content is global; 25 languages).

## 6. After approval

- [ ] App signing SHA-256 added to fifirecipes `ANDROID_CERT_SHA256`, deployed.
- [ ] Verify links on a Play-installed device:
      `adb shell pm get-app-links cooking.fifi.android` → `fifi.cooking: verified`.
- [ ] Update https://android.fifi.cooking "Coming soon" section with the Play
      badge/link: `https://play.google.com/store/apps/details?id=cooking.fifi.android`.
- [ ] Optional: add a "Get it on Google Play" link on fifi.cooking.

## Technical compliance (done in-repo)

- [x] Native Kotlin/Compose app — not a WebView wrapper (Minimum Functionality policy)
- [x] targetSdk 37, AAB, Play App Signing ready, R8 + resource shrinking
- [x] 16 KB page-size compatible (tested on the Android 17 16 KB emulator)
- [x] Edge-to-edge, predictive back, SplashScreen API, themed icon
- [x] Adaptive phone / foldable / tablet / ChromeOS UI, all orientations,
      resizable, keyboard + mouse — Large-screen app quality guidelines
- [x] Accessibility: TalkBack, 48dp targets, font scaling, contrast (automated tests)
- [x] Only the INTERNET permission; no data collection; privacy policy live
- [x] Per-app language (`locales_config`) for all 25 languages
- [x] Android App Links (assetlinks.json served by fifi.cooking)
