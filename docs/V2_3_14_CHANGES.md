# BOSS XI 2.3.14 — phone feedback fixes

Based on the delivered 2.3.13 source `78bbfa83b3f6c60d11d16c6ee6075ea7365a721b`.
Version code 40 retains the package and development update certificate.

- Recruitment: separate Sort and Filter controls for free agents and club pools;
  All players opens the complete active squad/world/free-agent database without
  duplicate recruited identities. Search and pagination cover the full result set.
  Filters include position, league country, club, ages, CA, PA, fee, wage, signing
  chance and budget availability. CA and PA use 1–100. PA is a development ceiling.
  Local signing estimates reuse the actual market-value club acceptance and
  requested-wage contract formulas. Fixed-term world offers show current
  registration/budget availability. Estimates are conditional on current terms.
- Half-time: numbered club-colour discs and surnames match the tactics board;
  ability and condition appear on every pitch marker and substitute.
- Presentation: home tabs have their own unclipped spacing and measured height;
  raised gradients, bevels, outlines, shadows and press feedback cover primary,
  secondary and dialog buttons. The dashboard manager briefing is removed.
- Contribution: separate player cards align ratings, condition, ability, status,
  goals, assists, passes, tackles and cards; departed participants remain listed.
- Inbox: dated cards, source/byline, read state, category filters, detail pages and
  contextual links. Fictional weekly newspapers and journalists generate stories
  and interview invitations as the career date advances. Replies have displayed
  morale/reputation effects, can be applied only once and survive save/restore.
  Legacy messages retain their text and are explicitly marked as undated.
- Match: direct controlled touches keep the ball at the carrier’s feet, with a
  possession ring; moving receptions end at the receiving player. Goal
  celebrations suspend the match clock/actions; both teams must physically reach
  and settle in their own-half kickoff positions before play resumes. Tactics
  retain the existing pitch animation state, including during minute zero.

Validated source: `78f8a6c9dacac8bd54b9c049cba3bf6f29a3042c`.
Both jobs in [feedback verification run 39335429890](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39335429890)
passed on 19 October 2026 using Android 15 (API 35) and Android 16 (API 36)
emulators. This includes installing the update over 2.3.13, preserving legacy and
current career saves, all nine requested fixes, shared screens, a complete live
match and reload, and all five compact landscape formations. Core regression
tests, including 49 new recruitment, inbox and restart checks, also passed.
Final home, database, unattached-player, inbox, contribution and normal/compact
tactics screenshots were visually reviewed. Physical-device play feel remains
subject to user feedback.

The APKs tested by both Android jobs are byte-identical:

- Package: `com.projectmister.game`, version `2.3.14`, code `40`.
- APK SHA-256: `8f947b2f283e7e0a8741f1904413b476d4976061881ffc9ef298c4e89fbfe4f9`.
- Signing certificate SHA-256: `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.
- Tested APK artifacts: `12226714290` (API 35), `12227310875` (API 36).
- Screenshots/reports: `12226413940` (API 35), `12226728783` (API 36).

The delivered `BOSS-XI-v2.3.14-Feedback-Fixes.apk` is the verified API 35 artifact,
renamed without modification. Install it over the existing signed installation
to preserve app data and career saves.
