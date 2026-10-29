# BOSS XI 2.3.17 — physical match flow and readable tactics

Based on delivered 2.3.16 source d7dfce1f3485769e8013949a10fe932eaf5f1b00.
Version code 43 preserves the application ID, save format and update signing key.

- Passes retain the destination chosen at the kick. Players run to that point;
  the ball no longer bends or changes its endpoint to follow a moving receiver.
  Control requires physical contact. Unreachable passes remain loose and wait
  for a player to recover them. Completed passes are recorded on reception,
  including crosses, with missed receptions recorded as missed passes. A received
  cross credits the crosser as the potential assister.
- Defenders must be within challenge distance before a dribble can be lost or a
  duel can award a tackle/foul. Distant defenders close down while the attacker
  carries into space. Successful close challenges also populate the player's
  contribution statistics.
- Live tactics tokens give current ability and condition their own full-width
  row. The numbered identity and name stay above, so compact layouts retain both
  statistics. Bench names, role/ability and condition also have independent
  single-line rows, including maximum CA100 and 100% condition. Native checks
  measure glyph widths and row heights in all five
  formations, in addition to the existing visibility/overlap checks.
- Resuming at half-time immediately enters the physical kickoff reset, clearing
  an unfinished first-half flight before either side can act in the second half.
- Club crests retain their own colours and initials. An old view can finish
  drawing safely after another career loads a smaller club list, fixing the
  country-switch crash found during the Android 15 checks.

## Verification

Full engine and retained-feature verification passed on Android 15 / API 35 and
Android 16 / API 36 at source `15001d66f426c23acd3422716ca7fa700cc22e4d`,
run `40939110325`, jobs `123677478731` and `123677478032`.

| Native report | Passing assertions per API |
| --- | ---: |
| Match flow: fixed flights, physical/missed receptions, crosses, challenges, crest switch | 672 |
| Stadium, table, division results, tactics and goal commentary | 625 |
| Retained club/stadium refinements | 368 |
| Recruitment, inbox, contributions and earlier match fixes | 250 |
| Main screens, navigation and interactions | 377 |
| Natural full match, event totals and saved-result reload | 161 |
| All five compact landscape formations | 781 |

Both jobs installed the delivered 2.3.16 baseline, seeded careers and upgraded
in place. Core checks passed, including 56 feedback/challenge checks, 3,109
identity/stand/news/shot checks and 1,982 projection/goal-flash checks, alongside
competition, calendar and backup regressions. The full-run APKs were
byte-identical. Screenshots/reports are artifacts `12926252688` (API 35) and
`12925454799` (API 36); full-run APK artifacts are `12927775034` and `12926891510`.
Downloaded ZIPs matched their GitHub SHA-256 digests before extraction.

The final production change after that full run only replaces the substitute
button's wrapped text with independently measured name, role/ability and
condition rows. Match engine, save format and game data are unchanged. Final
source `1ac2bb0ced13deefbe995dbcf0d59cf9d67a7d16` passed focused
API 35/36 validation in run `40941280581` (jobs `123685002689` and
`123685002066`): install-over-baseline/save checks,
all presentation scenarios and five compact formations, with every substitute
at CA100 and 100% condition for the width/height checks.

Native screenshots have been inspected for half-time and compact tactics and
for natural full-time results. Validation uses emulators; physical-handset play
feel remains subject to device testing. No new bitmap or audio assets are added.

Both final jobs passed 709 presentation assertions and 991 compact-layout
assertions, plus baseline/save checks. Bench controls were exercised through
selection, confirmation, an actual substitution and second-half resumption.
The final compact screenshots show all seven substitutes with complete CA100
and 100% rows; field markers and substitutes have no clipped stat glyphs.

Final QA artifacts: `12927871353` (API 35), `12927082810` (API 36).
Final tested APK artifacts: `12926718766` (API 35), `12926948066` (API 36).
All four downloaded archives matched GitHub's SHA-256 digests. Final APKs are
byte-identical and both QA checksum files match the delivered APK.

- Delivered file: `BOSS-XI-v2.3.17-Match-Flow-Refinements.apk`.
- Size: 7,556,039 bytes.
- SHA-256: `6c727e0e9386d5d7e036fd1834ceedf11ccc179b08421a7e173db2ebec508c09`.
- Package: `com.projectmister.game`, version `2.3.17`, code `43`.
- Minimum API 26; target API 36; no new permissions.
- Update certificate SHA-256:
  `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.

The final binary passed apksigner verification. It retains the prior update
certificate so it can be installed over the delivered version. No fatal
application exception appears in either final native log. This validation
record is a documentation-only change after the tested source above.
