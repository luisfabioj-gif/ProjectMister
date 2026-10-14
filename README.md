# BOSS XI 2.3.11 Android test build

Android football-management game, package `com.projectmister.game`.

Twenty selectable divisions across ten countries use real club identities and
fictional footballers. Careers connect league standings, linked-tier promotion,
recurring national cups and the Champions League, Europa League and Conference
League. Portuguese careers include the expanded lower pyramid and the published
future League Cup format. Transfers cover the twenty-division world pool and
unattached players, with country registration decisions explained in profiles.

History keeps sourced previous winners separate from your saved career trophies.
Player lists, training histories, cup appearances, suspensions and calendars are
saved alongside squads and finances. Watched and quick matches use eligible
players; half-time tactics and substitutions remain available during live play.

## Validation and installation

The exact verified scope and current blockers are in
[the completion tracker](docs/COMPLETION_STATUS.md), with machine-readable
milestone evidence in [COMPLETION_STATUS.json](docs/COMPLETION_STATUS.json).
Version 2.3.11 passes the complete Android 35/36 gate in
[run 38602775811](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811).
All 100 feature-scope points are verified and the tested APK is available with
the final chat handoff. [APK provenance](docs/BOSS_XI_2_3_11_RELEASE.md) records
the source, artifacts, checksum and preserved update signature.

[The Android test guide](docs/ANDROID_TEST_GUIDE.md) covers backup, installation,
existing saves, starting the expanded game and reporting issues on a real phone.
The build retains the existing development signing certificate for sideloaded
updates. Play publication and physical-phone testing are separate from emulator
validation; [Play preparation](docs/PLAY_STORE_READINESS.md) records their scope.

## Build and test

Use JDK 17, Gradle 8.11.1, Android SDK 36 and build-tools 35.0.0. SDK 35 is also
needed for the independent instrumentation runner.

```bash
gradle :app:assembleDebug :app:bundleRelease --no-daemon
bash tools/test-core.sh
bash tools/build-test-sdk.sh
```

For a dependency-free SDK build, set `JAVA_HOME` and `ANDROID_HOME` and run
`bash tools/build-sdk.sh`. The candidate workflow builds committed source,
installs the preserved baseline, verifies upgrades, navigation and live matches,
tests registration and player eligibility, completes recurring country careers,
checks compact landscape and installs release splits. It also checks restoration
across a different disposable signing identity and uploads logs and screenshots.

## Assets and football data

[Asset provenance](docs/ASSET_PROVENANCE.md) describes the reusable fictional
photographic portraits and stadium/facility artwork.
[Audio sources](docs/AUDIO_SOURCES.md) identify bundled crowd recordings and
effects. Hardware listening and sustained phone performance are checked during
the real-world test.

Published sources, projected later dates and simulated approvals are identified
in the game and repository notes. Generated youth training histories describe
fictional players; unknown prior appearances are not invented. Special medical
hearings, licensing decisions and competitions outside the modeled career world
are not presented as fully simulated regulations.

See [2.3.11 changes](docs/V2_3_11_CHANGES.md) for the current handoff scope and
[validation](docs/VALIDATION.md) for dated evidence.
