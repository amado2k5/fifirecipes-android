# Releasing — signed builds, the upload key, and recovery

How the `v*` tag pipeline works, how to configure the upload-key secrets,
and exactly what to do if `fifi-upload.jks` is lost. Read this before
touching a release; STORE.md covers Play Console submission details.

## How the pipeline works

Pushing a tag `vX.Y.Z` runs the `release` job in
`.github/workflows/android-ci.yml` (after `build` passes):

1. **Restore upload key** — decodes the `UPLOAD_KEYSTORE_BASE64` secret to
   `fifi-upload.jks` and writes `keystore.properties` from
   `UPLOAD_KEYSTORE_PASSWORD`. Both secrets must exist or the job fails
   fast, naming whichever is missing.
2. **Signed APK + AAB** — `:app:assembleRelease :app:bundleRelease`. Signing
   comes from `keystore.properties` in the repo root (gitignored);
   `app/build.gradle.kts` picks it up automatically when present.
3. **Attach to GitHub release** — uploads
   `fifi-recipes-<tag>.apk` and `fifi-recipes-<tag>.aab`. If a release for
   the tag already exists (e.g. published from the web UI first), the assets
   are uploaded to it with `--clobber`; otherwise the release is created
   with title `FiFi Recipes <tag>` and generated notes.

The keystore and `keystore.properties` are **never committed** — they exist
only on the founder's machine (repo root) and inside the two CI secrets.
Back both up somewhere safe.

## Required repository secrets

GitHub repo → **Settings → Secrets and variables → Actions → Repository
secrets → New repository secret**:

| Secret | Value |
|--------|-------|
| `UPLOAD_KEYSTORE_BASE64` | `base64` of `fifi-upload.jks` |
| `UPLOAD_KEYSTORE_PASSWORD` | the keystore password (see below) |

CI writes `keystore.properties` with **the same password for both
`storePassword` and `keyPassword`**, so the keystore's store and key
passwords must be identical. If your `keystore.properties` has different
values, CI cannot sign with it as-is.

Never paste keystore material or passwords into issues, PRs, or chat —
secrets go directly into GitHub Settings.

---

## Path A — the original `fifi-upload.jks` is found

1. Locate `fifi-upload.jks` + `keystore.properties` (repo root, backups,
   password manager, old clones).
2. **Verify it is the registered upload key** before using it — the
   certificate SHA-256 must be
   `EC:E5:11:84:2E:DC:25:B7:F6:7E:94:FF:55:8B:37:16:50:8C:09:A5:CD:DE:BB:93:64:BA:F7:BC:21:5C:4C:78`
   (recorded in STORE.md §0; also shown in Play Console → Release → Setup →
   App signing → *Upload key certificate*):

   ```bash
   keytool -list -v -keystore fifi-upload.jks -alias fifi-upload | grep SHA256
   ```

   (`keytool` ships with every JDK — on macOS use the JDK's `bin/keytool`,
   e.g. `"$(/usr/libexec/java_home)/bin/keytool"`.)

3. Copy the keystore to the clipboard as base64:

   ```bash
   base64 -i fifi-upload.jks | pbcopy        # macOS
   base64 -w0 fifi-upload.jks | pbcopy       # Linux
   ```

4. Add the two secrets (table above). `UPLOAD_KEYSTORE_PASSWORD` is the
   `storePassword` value from `keystore.properties`.
5. Continue to [Finish the release](#finish-the-release).

## Path B — the upload key is lost

A lost upload key is recoverable **only because the app uses Play App
Signing**: Google holds the app signing key, so replacing the upload key is
a console operation, not a re-release of the app.

**Verify the assumption first.** Play Console → FiFi Recipes → Release →
Setup → App signing (in some console versions: *Test and release → App
integrity → App signing*). The page must show a Google-managed **App
signing key** certificate. STORE.md §2 records that Play App Signing was
accepted at app creation — confirm it is still enrolled. If the app is
*not* on Play App Signing, the upload key **is** the final signing key and
losing it is unrecoverable — stop and reassess before generating anything.

The **app signing key itself is held by Google and is not lost** — only
the upload key needs replacing.

1. Generate a new upload key (any machine with a JDK):

   ```bash
   keytool -genkeypair -v \
     -keystore fifi-upload.jks \
     -alias fifi-upload \
     -keyalg RSA -keysize 2048 \
     -validity 10000 \
     -storetype JKS
   ```

   When prompted for passwords, **use the same password for the keystore
   and the key** (press Enter at the key-password prompt) — CI uses one
   secret for both. Record the password; it becomes
   `UPLOAD_KEYSTORE_PASSWORD`. The DN fields can be anything.

2. Export the public certificate:

   ```bash
   keytool -exportcert -rfc -alias fifi-upload \
     -keystore fifi-upload.jks -file upload_certificate.pem
   ```

3. Play Console → FiFi Recipes → Release → Setup → App signing →
   **Request upload key reset** → upload `upload_certificate.pem` → submit.
   Google reviews and applies the reset (can take a couple of days — watch
   for the confirmation email/console status).

4. **Only after Google confirms the reset**, add the two CI secrets using
   the *new* `fifi-upload.jks` and its password (same steps as Path A,
   steps 3–4). Until the reset completes, Play rejects bundles signed with
   the new key — do not add the secrets early and do not upload the new-key
   AAB to Play yet.

5. Back up the new `fifi-upload.jks` and its password somewhere safe, and
   keep `upload_certificate.pem` with them.

---

## Finish the release

Once the secrets exist, produce the signed artifacts for the pending tag.
For `v1.0.6` specifically, the failed run is
[42909756663](https://github.com/amado2k5/fifirecipes-android/actions/runs/42909756663).

**Important:** "Re-run failed jobs" reuses the workflow version from the
tag's commit (`b791683`, *before* the create-or-upload fix). Both options
below work; pick one.

### Option 1 — delete the GitHub release, re-run the failed job (simplest)

1. Copy the release notes text first if you want to keep them (the release
   was written in the web UI).
2. Delete the release **but keep the tag**:
   - Web: Releases → *FiFi Recipes v1.0.6* → Delete → **untick**
     "Also delete the tag" → confirm. Or run
     `gh release delete v1.0.6` (never touches the tag).
3. Actions → Android CI → run 42909756663 → **Re-run failed jobs**
   (or `gh run rerun 42909756663 --failed`).
4. The release job restores the key, builds the signed APK + AAB, and
   `gh release create` succeeds because no release exists. Re-paste the
   release notes afterwards via Edit release if you copied them.

### Option 2 — keep the release and its notes (uses the fixed attach step)

Requires the pipeline-fix PR to be merged to `main` first, then point the
tag at the merge commit so the new run uses the fixed workflow:

```bash
git fetch origin
git tag -f v1.0.6 origin/main
git push -f origin v1.0.6
```

The tag push fires a fresh Android CI run on `v1.0.6`; its release job sees
the existing release and uploads the assets with `--clobber`, leaving the
notes untouched. The merge commit changes only CI/docs — app code and
versionCode/versionName are identical. Afterwards verify the release page
still exists at `releases/tag/v1.0.6` (it should; force-updating the tag
does not delete it).

### Either way, verify

- The release page shows assets `fifi-recipes-v1.0.6.apk` and
  `fifi-recipes-v1.0.6.aab`.
- Download the AAB (`gh release download v1.0.6 -p '*.aab'`) and check it
  before uploading to the Play Console draft — see below.

## Verify the AAB before Play upload

The bundle must be `versionCode 7` / `versionName 1.0.6` and signed with
the registered upload key:

```bash
# Version — needs bundletool (comes with Android SDK command-line tools):
bundletool dump manifest --bundle fifi-recipes-v1.0.6.aab \
  | grep -E 'versionCode|versionName'
# (The tag's app/build.gradle.kts already pins 7 / 1.0.6; Play Console
# rejects any other versionCode for the draft.)

# Signing certificate — prints the signer cert and its SHA-256:
keytool -printcert -jarfile fifi-recipes-v1.0.6.aab
```

Compare the SHA-256 fingerprint:

- **Path A (original key):** must equal
  `EC:E5:11:84:2E:DC:25:B7:F6:7E:94:FF:55:8B:37:16:50:8C:09:A5:CD:DE:BB:93:64:BA:F7:BC:21:5C:4C:78`.
- **Path B (new key):** must equal the fingerprint of your new cert —
  `keytool -printcert -file upload_certificate.pem` — and, once Google's
  reset completes, the *Upload key certificate* shown in Play Console →
  Release → Setup → App signing.

Then upload `fifi-recipes-v1.0.6.aab` to the existing Play Console draft
(**Release → Testing → Closed testing – Alpha**); the release notes are
already saved there.

## Normal release flow (for future versions)

1. Bump `versionCode` + `versionName` in `app/build.gradle.kts`, merge to
   `main` (see STORE.md for submission details).
2. Push the tag — either `git tag vX.Y.Z && git push origin vX.Y.Z`, or
   create a GitHub Release for the new tag from the web UI. Both work now:
   CI creates the release if absent, or attaches assets to an existing one.
3. Check the run went green and the release has both assets.
