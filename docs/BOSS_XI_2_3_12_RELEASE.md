# BOSS XI 2.3.12 — Android device-feedback release

The eleven device-feedback areas are verified, and the signed APK is saved for
the chat download. Export existing careers and follow [the test guide](ANDROID_TEST_GUIDE.md).

This update addresses the eleven observations from the Samsung Galaxy S25 Ultra
test: new careers use real divisions, revised club profiles and multinational
fictional squads; scouting has a direct action and dated reports; statistics,
positions and history are easier to read; stadium development has a selectable
complex and independently changing stand and facility images.

## Exact build

| Field | Value |
| --- | --- |
| Version / code | 2.3.12 / 38 |
| Package | `com.projectmister.game` |
| Minimum / target SDK | 26 / 36 |
| Tested game source | `85d4de03593b22a0bf9d3d6e5a34e00ea838aa37` (unchanged game code) |
| API 35 verification head | `577283a3cc66ac63a2eb46233712713157f97a01` |
| API 36 verification head | `9ccdf0210e6823daf4bbaa2280ca71a0d6b22e97` |
| Main Android 15/16 workflow | [38991170087](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170087) — both jobs passed |
| Android 16 final workflow | [38992435601](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38992435601) — success |
| Android 35 job | [117273668694](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170087/job/117273668694) — success |
| Android 36 job | [117277538737](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38992435601/job/117277538737) — success |
| Focused feedback workflow | [38991170085](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170085) — success |
| Delivered filename | `BOSS-XI-2.3.12.apk` |
| APK bytes / SHA-256 | 4,336,932 / `41562ea6edd9a483baa60ecff5e25846f3364fe84e538d2c8947801a8558d385` (identical in both final artifacts) |
| Signing certificate SHA-256 | `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e` |

The focused native run verifies all 365 observed club profiles, representative
colours and calibrated strength, multinational squads, the explicit Scout
action with attribute masking disabled, six-day versus seven-day progression,
completed assessments, exact identity/report reload and atomic corrupt-backup
rejection. It also checks ordinal table positions, comparison statistics,
actual map selection, decoding all five architectural images and all 64
stand/facility approvals and completions with cash/capacity and reload checks.
History and responsive accounts are captured, followed by compact landscape
formation and player-marker bounds checks.

The main workflow also completed successfully on both platforms on 17 October 2026.
Its Android 16 job [117273667879](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170087/job/117273667879)
passes the complete rerun. The separately verified Android 16 final artifact below
remains the byte-matched delivered candidate.

## Final artifacts and download

| Artifact | ID | ZIP SHA-256 |
| --- | ---: | --- |
| [API 35 APK](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170087/artifacts/12085910283) |12085910283| `d469afb820d7994986a40f844204abd183f0c4e89bc7f835aa9a7f4f0cc5c41c` |
| [API 36 APK](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38992435601/artifacts/12086166773) |12086166773| `7f9e9c868c11ecef9c316424e3a4269e239b7d80e26f7a057802ae66be1da5da` |
| [API 35 QA](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38991170087/artifacts/12085122905) |12085122905| `3d506f03492496c03ce5644058cb6ebdf9cddcc345859df7a6758684a40f51f7` |
| [API 36 QA](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38992435601/artifacts/12086710149) |12086710149| `3654b07785539c6453c0f709f76dd00c57be81ce29454a40f488fd00e3e9435b` |

The archives pass SHA-256 and ZIP CRC checks. The APK verifies its v2 signature,
package/version/SDK fields, expected profile and stadium assets, and absence of
native ABI restrictions or QA-only fixtures. Its certificate matches the
existing installation's development update key. Both candidates contain the
identical 4,336,932-byte APK. The separate AAB-derived, non-debuggable release
installation also passes; this delivered sideload APK remains debuggable and
is not a Play production-signing claim.

QA archives retain the API-specific previous 25 passing modes and failed test
fixture diagnostics, source and APK equivalence hashes, fresh mode reports and
screenshots. Native Match Summary, scouting/report, ordinal table, all five
complex stages, history, accounts, release stadium and season-review screens
were visually reviewed. The final artifacts are retained until 16 November 2026;
the actual APK is also saved for the chat download. Documentation-only changes
after these jobs do not alter the tested game or APK.

The [completion tracker](COMPLETION_STATUS.json) now verifies all 100 fixed
points: **0% estimated feature scope remains, not a time estimate.**

## Facts and game estimates

The profile snapshot covers all 365 clubs in the twenty selectable divisions.
Observed squad market values calibrate explicitly labelled estimates for club
strength and starting board allocations. These estimates are not audited club
finances or private transfer budgets. Primary citizenship counts are apportioned
to twenty fictional players per club; their names and nationalities are then
saved by player identity. Opening European participants use the dated 2026/27
snapshot, while subsequent qualification follows career results.

The five generated architectural stages represent game development, with
sixteen independently saved plots. They are not photographs of each named
club's real ground. History presents selected past winners without source
buttons; research references remain in the repository.

See [the feedback record](DEVICE_FEEDBACK_20261015.md) and
[stadium art provenance](STADIUM_ART_PROVENANCE.md) for source boundaries and
implementation details.

## Regression coverage and handoff

The shared captured England draw remains a native and host regression, including
the original impossible pots, exactly two projected-seed exchanges, unchanged
qualifiers and coefficients, strict association/pot constraints, balanced venue
sequences, previous-season venue exclusions and canonical saves. Organic
careers validate their actual draw rather than requiring updated strengths to
reproduce the old field.

The full release gate also retains live matches, half-time tactics, worldwide
recruitment and free agents, country registration and cup rules, all ten
combined two-season European careers, recurring national cups, forty
standalone/linked division careers, backups, baseline upgrade, non-debuggable
release-bundle installation and different-key restoration. Both final jobs pass every required gate, using the source-checked prior mode
evidence and their fresh corrected division/release checks.

### Source identity and completed mode evidence

Original run [38984023562](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38984023562)
completed 25 native modes on each platform: 8,915 passing assertions on Android
35 and 8,909 on Android 36. Both then stopped at the same test-only defect: the
forty-division test still added the removed Classic option to the league index.
The real chooser and saved-career loader already used the correct zero-based
indices. The test now verifies the index and label on save and reload for every
standalone and linked division; all forty are rerun, including Turkish tier two.

The continuation checks accept those earlier mode reports only when the game,
build configuration, QA fixtures and every other native test method remain
unchanged. They verify each archive checksum, API-specific terminal acceptance
and absence of failure markers, then build the original game source again and
require the candidate APK to be byte-identical. The current emulator API and
all inherited report hashes are rechecked immediately before native testing.
A missing or changed source, report or fixture selects the complete suite.
The earlier failed division report remains in diagnostics and earns no credit.

Android 35 has completed the corrected 871-assertion division gate, live/upgrade
setup, exact backups, compact layouts, release-bundle upgrade and different-key
restoration. Android 16 passes the same corrected 871-assertion division gate
and all downstream checks. Both candidates match their rebuilt reference
exactly and match each other byte for byte. Neither final logcat contains a
game fatal exception or app ANR. The source identity and inherited mode
hashes are preserved in `apk-equivalence.json` and
`previous-verified/evidence.json` within each continuation QA archive.

Use [the Android test guide](ANDROID_TEST_GUIDE.md). Export existing careers
before installing the update. Start a new career in an empty slot to use the
recalibrated starting club data; existing careers retain their saved finances
and progression. Physical Galaxy S25 Ultra testing remains the user's next
step after the emulator development gate.
