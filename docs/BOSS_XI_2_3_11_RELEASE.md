# BOSS XI 2.3.11 — verified Android test handoff

The tracked development scope is complete for the user's Samsung Galaxy S25 Ultra
real-world test. The actual APK is available with the final chat handoff.
Back up existing careers first and follow [the test guide](ANDROID_TEST_GUIDE.md).

## Exact build

| Field | Value |
| --- | --- |
| App | BOSS XI |
| Version / code | 2.3.11 / 37 |
| Package | `com.projectmister.game` |
| Minimum / target SDK | 26 / 36 |
| Tested source | `bd5059b96a93c91655d56db2402d0142c6c195bf` |
| Full Android workflow | [38602775811](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811) — success, 14 October 2026 |
| Android 35 job | [115985935375](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/job/115985935375) — success |
| Android 36 job | [115985934651](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/job/115985934651) — success |
| Delivered filename | `BOSS-XI-v2.3.11-Android-Test.apk` |
| APK bytes | 2,637,610 |
| APK SHA-256 | `005d9e0cb9eb0394bb638dcd67b704f051b65064552b0e7adfaa6f3e268981a5` |
| Signing certificate SHA-256 | `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e` |

Both passing candidate artifacts contain the exact same APK bytes. The artifact
ZIP digests and CRCs were verified before extraction; the manifest and APK v2
signature verify. This development APK retains the existing sideload update key.
It is debuggable; the separately tested AAB-derived release installation is
non-debuggable. No Play production-signing or publication claim is made.

## Preserved workflow artifacts

| Artifact | ID | ZIP SHA-256 |
| --- | ---: | --- |
| [Android 35 tested APK](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/artifacts/11917973416) | 11917973416 | `a839ca7f16f6f16cd47194e6bd6ea3952697cbdbdfd2249375b2074ac355d405` |
| [Android 36 tested APK](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/artifacts/11920215445) | 11920215445 | `220b21e6dee73ab9f422499a8d5bd47e800af392750602e07b8224b7e4fbc8ef` |
| [Android 35 QA](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/artifacts/11917728577) | 11917728577 | `dcf501a757bc60bfa99520b7c0156a304be62ac388487049cdd9bc4a95885be5` |
| [Android 36 QA](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811/artifacts/11919157922) | 11919157922 | `7cd0ef1aee871941effdda18303671d948dee5336d5ec232abb77e6eadc1030b` |

GitHub artifact retention ends on 13 November 2026. The actual APK is also saved
for the chat download. Later documentation-only commits do not alter this tested
production source or its APK.

## Verified behavior

Both complete gates include twenty country/tier registration scenarios; ten final
league decisions; actual French/Scottish cup restrictions, unavailable keepers,
stable promoted-player identities, qualification dates, appearances and ban
service; UEFA A/B lists, late free-agent exclusion and three winter additions.

Each of ten country careers completes two combined UEFA/domestic seasons with
watched and quick results, annual admissions, chronological calendars, exact
archives and backups. Recurring national cups, three Portuguese seasons, the
new Portuguese League Cup and lower-pyramid progression also pass. All twenty
standalone and twenty linked careers pass progression and backup checks.

Natural live matches reach full-time after half-time tactics and resume.
Compact layouts, legacy upgrades, release-bundle installation and exact
different-key restoration pass. Each QA archive retains 30 per-mode reports and
152 screenshots. Rules, UEFA-player, cup-player, division, release and migration
reports respectively contain 179, 31, 130, 751, 94 and 6 passing assertions on
both versions. Assertion counts are evidence, not completion weights. No game
fatal exception or app ANR appears in either final logcat.

The APK contains no native libraries, so its Java/asset payload supports the
phone's architecture. It packages 78 fictional photographic portrait tiles and
stadium/upgrade artwork. [The visual review](V2_3_11_QA_REVIEW.md) records the
screens inspected. Historical winners use sourced records; current career
results, future editions and adjusted draws are labelled simulations.

## Scope and next step

[The fixed tracker](COMPLETION_STATUS.json) has all 100 points verified:
**0% estimated feature scope remains, not a time estimate**.

Physical-phone audio, touch, performance and stability feedback is the next
validation step. Ordinary lower pyramids outside Portugal, special administrative
hearings and Play publication remain outside the declared development gate.
The Portuguese future-cup qualifier summary retains sparse vertical spacing;
names, dates and fixture controls remain readable and functional.

Keep existing saves by exporting backups and installing the APK as an update.
Start the expanded game in an empty slot. Report the club/division, season/date,
screen/action, watched or quick match, expected result and observed result;
include an exported backup for progression or save issues.
