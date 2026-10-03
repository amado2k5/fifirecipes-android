# FiFi Recipes — Android

Native Android companion to [fifi.cooking](https://fifi.cooking), the recipe
site by Dr. Fatma / FiFi. Kotlin + Jetpack Compose, **not a WebView wrapper**:
every screen is native Compose consuming the static JSON API at
`https://fifi.cooking/data/` — the same contract as the Fire TV app
(`fifirecipes-amazonfire`), the iPhone app (`fifirecipes-ios`) and the iPad app
(`fifirecipes-ipadosapp`); see `docs/tv-api.md` in the `fifirecipes` repo.

- **Package:** `cooking.fifi.android` · **minSdk** 26 (Android 8.0) ·
  **target/compileSdk** 37 (Android 17)
- **Devices:** phones, foldables, tablets, Chromebooks — one adaptive UI
- **Dependencies:** Compose (Material 3), Coil 3, OkHttp, kotlinx.serialization.
  No analytics, crash, ads or Google Play Services SDKs.
- **Site:** `site/` → https://android.fifi.cooking (landing, privacy, support)

## Build & test

Prerequisites: JDK 17, Android SDK platform 37 + build-tools 37.

```bash
./gradlew :app:assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest             # JVM unit tests (localization, search, links, strings)
./gradlew :app:connectedDebugAndroidTest     # Compose UI tests on a device/emulator (live API)
./gradlew :app:lintRelease                   # Android lint — must be clean
./gradlew :app:bundleRelease :app:assembleRelease   # Play AAB + sideload APK
```

UI tests (`app/src/androidTest`) start from a clean first run each time and
cover: language picker → home, recipe checklist + back, search, chapters,
the full Kids flow to the celebration screen, Arabic RTL, and a 48dp
touch-target / labelled-control audit. They pass on a Pixel 9 (API 36),
Pixel Tablet (API 36) and Pixel 9 Pro Fold on Android 17 with 16 KB pages.

## Architecture

```
app/src/main/java/cooking/fifi/android/
├── MainActivity        splash, edge-to-edge, App Links, keyboard shortcuts
├── FifiApplication     ApiClient + Coil image loader (shared OkHttp, disk caches)
├── AppViewModel        manifest → language picker → sections; per-section back
│                       stacks (survive rotation/fold + process death);
│                       Android 13+ per-app language sync
├── data/               API models, ApiClient (?v= versioning, in-flight dedupe,
│                       kids en-fallback), UiStrings (25 languages), recipe
│                       localization rules, search, routes + deep links
└── ui/
    ├── theme/          fresh-market Palette, per-language fonts, Material theme
    ├── components/     InteractiveCard (hover/focus/context menu/share), cards,
    │                   RemoteImage, KidsArt, status views, YouTube player
    ├── screens/        Home, Chapters, Search, Recipe detail, Settings, picker
    └── kids/           Kids catalogue, Get ready, step pager, celebration
```

### Shared with the other clients

| Asset | Source |
|-------|--------|
| Colours | `Palette` / `KidsPalette` — same hex values as TV/iOS (two dims darkened for 4.5:1 text contrast) |
| Fonts | Plus Jakarta Sans, Tajawal, Vazirmatn, Noto Nastaliq Urdu, Heebo, Baloo 2, Baloo Bhaijaan 2 (OFL, `res/font`) |
| UI strings | `assets/ui-strings.json` (verbatim from the iOS/iPad apps) + `ui-strings-android.json` overrides (device-neutral "about" text, share/open/back/privacy) |
| Kids art | 190 drawings from the TV SVG library, as 384px WebP in `assets/kids-art` |
| Localization rules | Arabic falls back to master fields, never English; cultural notes never fall back |

`scripts/generate-assets.mjs` regenerates fonts, strings, kids art, emblem,
launcher and store icons from the iPad repo (`cd scripts && npm install` first).

### Android / Google Play quality work

- **Adaptive layout** (large-screen quality tier 1 targets): bottom bar < 600dp,
  navigation rail 600–840dp (and on any window shorter than 480dp — landscape
  phones), permanent branded drawer ≥ 840dp. Recipe/Kids pages go two-column
  from 840dp; content widths are capped for readability. Resizable, every
  orientation, no letterboxing; fold/unfold, split-screen and freeform windows
  reflow live without recreating the activity.
- **Input:** keyboard focus rings on every card, Tab/arrow navigation,
  Ctrl+1…5 sections, Ctrl+F search, Esc back, ←/→ through Kids steps, system
  shortcut helper (Meta+/); mouse/trackpad hover lift and right-click context
  menu; long-press menu on touch; `touchscreen` not required (ChromeOS).
- **Accessibility:** TalkBack labels and headings, merged card semantics with
  Share custom actions, checkbox/radio/tab roles, live-region search status,
  ≥ 48dp targets (tested), all text in `sp` (tested at 200 % font), colour
  contrast ≥ 4.5:1, animations follow "Remove animations".
- **System integration:** SplashScreen API, edge-to-edge, predictive back,
  themed (monochrome) launcher icon, per-app language (`locales_config`),
  Android App Links for `fifi.cooking/recipe|chapter|kids/*`, share sheet,
  screen kept awake during Kids steps, backup limited to the language setting.
- **Video:** youtube-nocookie embed in a locked-down WebView; pauses with the
  activity, torn down on close; "Open in YouTube" hands off to the app.
- **Privacy:** only the `INTERNET` permission; no data collected or shared.
- **Size:** release AAB ≈ 15 MB (R8 + resource shrinking); only native code is
  AndroidX's tiny `graphics.path`, 16 KB-page aligned.

## Releasing

The upload key (`fifi-upload.jks`) and `keystore.properties` live in the repo
root locally and are **gitignored** — back both up somewhere safe (password
manager / encrypted drive). Losing the upload key means asking Google Play
support for an upload-key reset.

- Local: `./gradlew :app:bundleRelease` signs with the upload key when
  `keystore.properties` exists (unsigned otherwise).
- CI: pushing a tag `v*` builds a signed APK + AAB and attaches them to a
  GitHub release, once these repository secrets exist:
  - `UPLOAD_KEYSTORE_BASE64` — `base64 -i fifi-upload.jks | pbcopy`
  - `UPLOAD_KEYSTORE_PASSWORD` — the password in `keystore.properties`

Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` for every
Play upload. Store submission details: **[STORE.md](STORE.md)**.

## CI

- `.github/workflows/android-ci.yml` — every PR/push: lint, unit tests, debug
  APK and release AAB (artifacts); on `main`: instrumented UI tests on an
  emulator; on `v*` tags: signed release.
- `.github/workflows/pages.yml` — deploys `site/` to
  https://android.fifi.cooking on every commit to `main`.
