# v2.3 validation status

This is an implementation candidate, not a finished release.

## Completed
- Verified GitHub run 36338934062 succeeded at c52c5c905f279059c0d1cd0e53f9665950060e5f.
- Recreated all Hotfix 4 source and resources, applied its original patches, and passed its sanity checks.
- Preserved exact source and signing identity in local commit 45218f9.
- New core Java regression tests pass: five-change allowance, chained pending replacements, reversal at limit, departed-player eligibility, prevention of post-resume reversal, pace/fatigue influence, bounded flight and score-aware intent.
- Candidate Java compilation, resource compilation, DEX generation, APK assembly and APK signature verification pass through direct Android SDK tools.
- Independent instrumentation APK builds.
- Fictional portrait atlases visually inspected; 42 compressed asset files bundled. Missing-asset fallback implemented.
- Startup's five safe no-op navigation calls retained. Package, preference keys and historical development certificate retained.

## Not completed / release gates
- Normal Gradle CI build. Local dependency resolution initially failed; later trusted certificates and an isolated cache allowed plugin downloads, but the full Gradle build was not completed before runtime reset.
- Install/launch and save upgrade test on emulator.
- Runtime navigation, paused substitution undo, half-time and full-time checks.
- App screenshot visual QA, including small landscape layouts. Generated portrait inspection is not app screenshot QA.
- Listening review on Android hardware. Crowd assets are original synthesis; realism is not yet verified.
- Long-career, season rollover, fixture/cup integrity and performance profiling.

## Prepared automation

.github/workflows/build-boss-xi-v2.3.yml builds committed source, installs preserved Hotfix 4, seeds a save, upgrades to v2.3 and runs app/src/androidTest/java/com/projectmister/game/qa/SmokeRunner.java. It collects screen images and logs. Screens include dashboard, squad/player, staff/profile, manager, tactics, finances, stadium, scouting, transfers, training, fixtures, board, inbox, live match and half-time. The runner uses real Activity screens through reflection; it is not a comprehensive gesture/UI navigation test.

Local software emulation failed before app launch: the Android system image was truncated relative to its archive, causing missing super/vbmeta partitions and a boot loop. Image restoration was attempted, but that runtime and its tooling did not survive the session reset. No app runtime success is claimed.

## Remote publication gate

Automatic approval review rejected a dry-run push because publication of a new branch to the public GitHub repository was considered unauthorised. No branch has been pushed; no remote main branch has been modified. Explicit approval is needed to publish work/boss-xi-v2.3 and run the prepared GitHub Actions workflow. Do not work around this rejection through another GitHub write tool.

## Scope limits

This candidate refines existing systems; it does not implement every optional feature in the brief. Portraits repeat across the database. No new full youth intake, complex agent negotiations, durable career archive, new competition engine or dedicated captain/set-piece assignment UI. Some old screens retain their original content structure under shared styling. Event-based ratings are a transparent simple contribution formula, not a comprehensive performance model. Audio header/post/catch samples are prepared but not all distinct cues are currently attached to corresponding match events.

## Rebuilt candidate checkpoint

Local commit: 3645e2c (contains implementation commit 0be1d4e and preserved baseline 45218f9).
Candidate APK size: 1,364,785 bytes.
SHA-256: 077bb37ab9427a84557c5bbf0aea52fe3e07a92bbb09b315bd558356d43abfd5.
Direct SDK rebuild and v2/v3 signature verification succeeded; core regression tests passed; instrumentation runner compiled. Runtime gates above remain pending.
