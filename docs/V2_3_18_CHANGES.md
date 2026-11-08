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

Earlier device checks at source `597e1a54d877394a44abd31a6e320ff913d6041d`
passed Android API 26 and 37 in run `42223262485`. Android 37 included natural
full match, all five compact formations, release split installation, career
migration and locked/configured editor checks. Source
`d625270c68f2ba711fef7011b6a39c7c6dc78218` changes only QA files from that
production code. Its Android 36 cups and all ten countries' European career
suites passed in run `42311492850`; its deep suite exposed the now-fixed
real-world heading bug and stopped there.

The final native suite has not run after the 8 November production fixes. The
runner now also checks that retained custom names render exactly without
unlocking unpaid reset access. An actual completed Play purchase has not been
tested: the publisher must configure the app, activate the product and test it
through Play before release. Physical-handset testing remains outstanding.
