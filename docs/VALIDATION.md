# BOSS XI v2.3 validation

This is a tested development APK, not a Play Store production release. Android hardware listening and sustained performance review remain necessary.

## Baseline and source integrity

- Original successful baseline: Actions run 36338934062 at c52c5c905f279059c0d1cd0e53f9665950060e5f.
- Reconstructed Hotfix 4 source passed its generation, patch and sanity checks.
- Exact generated source tree is preserved remotely in commit 73275b274b02c3273e72d9710f0639a54f29d590 (same tree as original local checkpoint 45218f9).
- Package com.projectmister.game, preference names, development signing certificate and all five safe startup navigation no-ops are retained.
- v2.3 now builds from ordinary committed Java and resources on work/boss-xi-v2.3. Historical workflows are retained. Main has not been changed.

## Verified checks

- Pure Java regressions: substitution limits, chained pending changes, reversals, departed-player eligibility, pace/fatigue, bounded ball flight and score-aware intent.
- Normal Gradle build with JDK 17 / Gradle 8.9 / Android SDK 35, plus independent instrumentation APK.
- Direct SDK build, DEX generation and APK signature verification.
- Android 35 emulator: install Hotfix 4, seed and reload a career, install v2.3 over it, preserve manager and club, launch MainActivity.
- Navigation: dashboard, squad, player profile, staff hub/profile, manager profile, tactics, finance, stadium, scouting, transfers, training, fixtures, board and inbox.
- Live match: paused substitution reversal restores allowance; returning from tactics preserves pause; committed departures cannot return; half-time automatically opens tactics; full-time saves exactly one round and survives reload.
- Complete simulation without clock jumps: attacking events occur, goals <= shots on target <= shots, scorer event totals equal final score, opposition uses at most five substitutions, progression survives reload.
- Run 36445977977 passed the complete simulation (10 shots, 3 on target, 1 goal, 5 AI substitutions in that sample). This is a regression example, not a balance study.

## Visual QA and fixes

Screenshots cover the screens above plus match and half-time. Review found overlapping tactics markers, an asynchronous pitch layout issue and transient rotation captures. Changes introduce separate 48dp-high targets, responsive width, scrolling for tall formations, a persistent Return to Match control, and post-layout population. Regression checks require all 11 markers to have nonzero visible layout, stay within pitch bounds and avoid intersection, across all five formations. A compact 640x360dp landscape pass is included.

The pitch uses original resolution-independent drawing. The scoreboard and role/team identity remain legible; Pause/Resume and Tactics are directly accessible. Forty-two fictional photographic portrait assets are loaded asynchronously with bounded caching and a neutral fallback.

## Failures resolved

1. SDK setup requested retired package `tools`: changed setup to explicitly install supported platform-tools.
2. Upgrade fixture used SharedPreferences.apply immediately before instrumentation process exit: added a synchronous flush and baseline reload assertion. Subsequent upgrade checks pass.
3. Tactics markers overlapped: widened positional spacing, bounded token sizes and provided vertical scroll when necessary.
4. Markers created inside layout existed but were not drawn: moved population to a posted callback; tests now also require positive measured size.
5. Tactics pitch background had zero height inside a wrap-content scroll child: use the measured frame dimensions and verify the background fills them.
6. Transient audio focus loss left ambience silent: resume only when playback is still requested; explicit lifecycle pause prevents an unwanted restart. Crowd loops have higher stream priority than short effects.

## Limitations

- Tests navigate real Activity screens using reflection; they do not constitute exhaustive gesture testing of every button or dialog.
- Original synthesized crowd and effects require listening review on Android hardware. No claim of studio-quality stadium recordings is made. Some prepared header/post/catch cues are not yet attached to distinct engine events.
- Emulator logs include skipped frames during transitions. No sustained hardware frame-time or battery benchmark has been completed.
- Long careers, season rollover and competition/fixture integrity need deeper validation.
- Portraits repeat across the database. Some management screens retain their established content structure with shared visual styling.
- No full youth intake, complex agent negotiations, instalments, new competition engine, durable career archive or captain/set-piece assignment UI is claimed.

## Publication

User explicitly authorised branch publication and validation. Installing ChatGPT Codex Connector for the repository resolved the earlier 403. Source and assets are published on work/boss-xi-v2.3. No token or password was requested from the user.

## Final verified candidate — 28 September 2026

- Successful Actions run: https://github.com/luisfabioj-gif/ProjectMister/actions/runs/36447293505
- Tested source: `35115cd7e6d06adf8afe49dfd7af581033d5001f`.
- APK artifact: `10981815304`; QA screenshots/logs artifact: `10981890240`.
- APK bytes: 1383627; SHA-256: `ac8c77133cbb3db51b177fdae2d418299eb28b13dd98e245aa56e151bb59b503`.
- Final natural-match sample: 16 shots, 3 on target, 0 goals, 5 opposition substitutions. All consistency, save-upgrade, navigation, match and compact-layout regressions passed.
- Final screenshots inspected: half-time tactics, compact 4-2-3-1 and 5-3-2, and compact live match. The green pitch background now renders correctly. Tall formations deliberately scroll; Return to Match remains outside that scroll. A transient substitution toast is visible in the half-time capture.

This closes the current implementation and automated verification milestone. The limitations above remain release-readiness work, especially hardware audio review, sustained performance and long-career validation.

## v2.3.1 follow-up

See [V2_3_1_CHANGES.md](V2_3_1_CHANGES.md) for final successful run 36594832195, new offer/action tests, recorded audio validation, screenshot findings and APK hash. Earlier synthesized-crowd notes above describe v2.3; current audio provenance is in AUDIO_SOURCES.md.
