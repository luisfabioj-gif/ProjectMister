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

Validation evidence is recorded after the Android feedback workflow completes.
The workflow tests the previous APK-to-update path, all nine requested areas,
shared screens, a complete live match/save, and all five compact formations on
Android 35 and 36. Core tests cover existing competitions and the new models.
