# BOSS XI 2.3.18 — Play candidate and paid editor

Package `com.projectmister.game`, version code 44, minimum Android API 26,
target API 36. Billing uses Google Play Billing Library 9.1.0. Sideloaded test
updates retain the existing development certificate; the release bundle is
unsigned for the publisher's private upload key.

## Included

- The optional Game Editor requires a verified purchase of
  `boss_xi_game_editor`. Price comes from Play, and buy option `editor-unlock`
  is a permanent single purchase. Pending payments, failed acknowledgements,
  absent or invalid receipts and unavailable Play checks cannot grant access.
  Restore checks Play again; saves and backups never grant ownership.
- Entry points and name, colour and reset mutations all check ownership.
  Careers, matches and the rest of the game remain free.
- Alternate club identities are closer to the original labels. Live pitch,
  half-time controls, five-match league form, compact standings, results from
  the managed division only, and flashing goal commentary are retained.
- History and English disclosures keep ordinary phrases such as
  "Real-world winners". The Portuguese club Real still has its alternate
  identity in unedited labels and match reports.
- Saved custom names appear exactly as entered, including names resembling
  another club and punctuation. Display overrides rebuild from the current
  world's saved edits. Single-team and all-team resets remove overrides and
  restore the default alternate names and colours; all-team reset batches
  preference changes once.
- In-app privacy information, a publisher policy template, store listing,
  icon, feature graphic, representative native screenshots and the upload
  configuration/signing tool accompany the candidate.

## Validation and limits

`tools/test-core.sh` passes on the final local game source: all competition,
calendar, season, backup, registration and match regressions, plus 3,145
identity/stand/news/shot checks, 1,982 projection/goal-flash checks and 51 paid
editor checks using real RSA signatures over synthetic Play-shaped receipts.
The QA API scanner resolves all 120 referenced Activity methods.

The final source `b921b4be4fd7732145ef85bb6f2494450cd0410a` passed all seven
emulator jobs on Android 8, 15, 16 and 17. The checks covered full matches,
compact half-time layouts, league tables and division results, all forty
standalone and linked career setups, recurring national cups and European
careers in all ten countries. Release splits installed on all four Android
versions. The editor stayed locked without a purchase, including when a save
already contained custom club names. Backup and restore passed across a
different signing key. See the
[final Android run](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/42476994853).

The configured Android 17 fixture checked Google Play and kept the editor
locked without ownership. Physical phone testing and an end-to-end purchase,
restore and refund check through Google Play remain pending.
