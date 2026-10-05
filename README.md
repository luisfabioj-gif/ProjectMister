# BOSS XI v2.3.2 development candidate

Android football-management game, package `com.projectmister.game`.

## Status

Twenty selectable divisions across ten countries, with linked two-tier careers. Scottish, German and Turkish promotion checkpoints passed Android 35/36 verification. Other countries' transitions and playable domestic/European cups remain unfinished. See [current competition status](docs/COMPETITION_EXPANSION_STATUS.md), [audit and scope](docs/V2_3_AUDIT.md) and [validation status](docs/VALIDATION.md). Hardware audio/performance review remains open.

## Normal build

JDK 17, Gradle 8.11.1, Android SDK 36 and build-tools 35.0.0 (Android SDK 35 is also required for the independent QA runner):

```
gradle :app:assembleDebug --no-daemon
bash tools/test-core.sh
```

Builds now use committed Android source. Historical reconstruction workflows remain as baseline references. New candidate workflow: `.github/workflows/build-boss-xi-v2.3.yml`.

For a dependency-free SDK build of this Java app, set `JAVA_HOME` and `ANDROID_HOME`, then run `bash tools/build-sdk.sh`. This assembles a debug candidate through aapt2, javac, d8, zipalign and apksigner; it does not replace runtime QA or normal Gradle CI.

## Regression verification

`tests/RegressionTests.java` covers transactional substitutions, eligibility, limits, pace/fatigue, bounded ball flight and tactical intent.

`app/src/androidTest` contains an independent instrumentation runner, built with `bash tools/build-test-sdk.sh`. The CI workflow installs preserved Hotfix 4, seeds a career, installs the candidate without uninstalling, navigates core screens, checks substitutions before/after resumed play, crosses half-time/full-time, runs a complete match, verifies goal/shot consistency and saved round progression, and checks marker layout across all five formations on normal and compact landscape screens. It uploads screenshots and logs for review.

Startup's safe immersive-navigation no-op, existing preference names and development signing identity are preserved. The bundled certificate is the historical development certificate; this is not a new production signing setup.

## Assets

See [asset provenance](docs/ASSET_PROVENANCE.md). Forty-two reusable fictional photographic portraits load asynchronously from the APK. Crowd ambience and goal reactions now use CC0 field recordings; see [audio sources](docs/AUDIO_SOURCES.md). Original short effects remain. Quality needs hardware listening review.

See [v2.3.1 changes](docs/V2_3_1_CHANGES.md) for the offer fix, shared branded menus, three speed settings, recorded crowd and movement changes.
