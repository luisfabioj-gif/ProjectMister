# Google Play preparation — 29 September 2026

NOT approved for publication. Competition expansion and release QA are ongoing.

## Build preparation

- Preserve application ID `com.projectmister.game` and startup no-op.
- Target/compile API 36; AGP 8.10.1, Gradle 8.11.1, JDK 17.
- `gradle :app:bundleRelease` creates an **unsigned**, non-debuggable AAB.
- Never use the repository's public development key as a production signing
  key. Debug APKs retain it to preserve upgrades for existing testers.
- Before first Play upload, configure a private upload key outside the repo,
  enroll in Play App Signing, and sign the release bundle. Signing identity
  differs from current sideloaded test builds; provide a tested save-export /
  import path before directing existing testers to replace their installation.
- No production key or Play credentials have been created, uploaded or exposed.
- API 35 and 36 emulator jobs exercise baseline save upgrade, navigation,
  transfer negotiation, tactics, live match/audio and compact landscape layout.
- APK success does not establish AAB/Play-install success: validate bundletool
  splits, release manifest, device installs and Play pre-launch reports too.

## Data safety audit of current source

The manifest requests no network, location, contacts, camera, microphone or
advertising-ID permission. No ads, billing, analytics or third-party tracking
SDK is declared. Names, fictional manager date of birth, career progress,
settings and edited club information are stored in local SharedPreferences.
Portraits/audio are bundled assets. Android backup is enabled, so operating
system backup/device-transfer behavior must be described; do not promise that
data can never leave the device. Users can delete a career from the main menu,
or erase all app storage through Android settings.

Before submission, publish an accessible privacy policy with the actual
developer/legal identity and support contact, link it from the app and Play
listing, and complete Data safety from the final binary's behavior. These
identity/contact fields cannot be invented. Re-audit after adding any SDK.

## Remaining publication work

- Finish gameplay integration and release regression/visual QA.
- Private upload signing; secure key backup; monotonically increasing release
  version code; signed AAB and device/Play internal-track validation.
- Save export/import for development-key to Play-key migration.
- Adaptive app icon, 512px store icon, 1024x500 feature graphic, representative
  phone screenshots, concise store description and supported-device checks.
- Rights review for real competition/club naming before commercial listing;
  no official crests, kits, logos, players' likenesses or endorsement claims
  have been licensed by this work. Existing fictional portraits and original /
  documented CC0 audio remain the game's assets.
- Developer account verification, content-rating questionnaire, target
  audience, ads declaration, app-access response, privacy URL and Data safety.
- Applicable personal-account production access testing (12 opted-in testers
  for 14 continuous days for affected accounts), then pre-launch report review.
- Google Play publication is a later explicit action, not part of this build.

## Official references

https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
https://developer.android.com/develop/adaptive-apps/guides/app-orientation-aspect-ratio-resizability
https://developer.android.com/about/versions/16/setup-sdk
https://developer.android.com/build/releases/agp-8-10-0-release-notes
https://developer.android.com/studio/publish/upload-bundle
https://support.google.com/googleplay/android-developer/answer/9842756?hl=en
https://support.google.com/googleplay/android-developer/answer/9859455?hl=en
https://support.google.com/googleplay/android-developer/answer/14151465?hl=en

## Local bundle packaging verification, 2 October 2026

For the release AAB from successful run 37000932886 (code d9eb26d), official
Google bundletool 1.18.3 `validate` passed. `build-apks` produced an APK set
signed only with the existing public development key for testing; both generated
base APK signatures passed apksigner. The release manifest preserves
com.projectmister.game, version code 27, minimum API 26, target API 36,
non-debuggable application, and no requested permissions. Estimated compressed
APK-set delivery is approximately 1.80 MB (bundletool get-size).

No bundle-generated APK was installed on a device in this local check; CI
installs the Gradle debug APK. Bundle/split device installation and a private-key
Play internal-track installation remain required before production readiness.
No private production key was created and nothing was uploaded to Play Console.
