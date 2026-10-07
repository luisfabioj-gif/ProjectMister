# Recurring competitions engine — 7 October 2026

This is a development foundation, **not a new playable competition release**.
The latest delivered gameplay build remains 2.3.5. These classes compile into the
application but are not called by MainActivity, its saved careers or its calendar.
Existing careers and the two playable 2026/27 Portuguese cups are unchanged.

## Implemented

- `SeasonCup`: independently qualified entrants each year, staged entry, seeded
  opening pots, amateur home rights, one/two-leg rounds, neutral finals, extra
  time and penalties. Explicit dates, deterministic draws and validated saves.
- `CupSeasons`: atomic rollover only after a champion exists, immutable complete
  tournament archives, contiguous seasons and fresh next-season results.
- `DomesticCupFormats`: Germany's 64-club DFB-Pokal and England's 124-club FA Cup
  main competition (80 first-round clubs, 44 third-round clubs). Qualified fields
  must be supplied by a future qualification layer. These factories do not create
  the missing lower-division clubs or play qualifying rounds.
- `EuropeanLeaguePhase`: 36-club Champions League, Europa League and Conference
  League fields; eight/eight/six matches; validated pots, associations and home/
  away balance; result chronology; all final-table Article 18 criteria; eight
  direct R16 clubs, sixteen playoff clubs and twelve eliminated clubs. An
  unresolved final sporting tie blocks qualification instead of using club ID.
- `EuropeanKnockout`: the Article 19 / Annex B bracket, eight playoffs, eight
  round-of-16 ties, four quarter-finals, two semi-finals and a neutral final.
  The 23 ties contain 45 fixtures. Seeded teams receive the return home leg;
  quarter-final and semi-final priority follows the bracket path even when the
  original seeded club is eliminated. The silver-side finalist is nominal home.
  Two-leg ties use aggregate goals, extra time and penalties, without away goals.
- `EuropeanSeason` connects a completed league phase to its own derived knockout
  bracket; `EuropeanSeasons` retains full completed seasons when fresh fields
  and calendars are supplied for the next year. No domestic access allocations,
  qualifying-round demotions or titleholder places are invented.

Snapshots regenerate draws and replay validated results. Altered pairings,
conflicting seed/calendar metadata, future results, out-of-order return legs,
incomplete rollover and played next-season states are rejected. Historical
snapshots are immutable strings; club IDs can change between season fields.

## Calendar and rules limits

Domestic factory dates select one day from the 2026/27 round windows and project
that calendar in future years. They are marked **simulated**, including the
first year; these are not TV fixture dates. DFB Supercup clubs' delayed opening
matches are not scheduled separately. The FA Cup factory begins at the first
round proper: qualifying rounds and their replay rules are not implemented.

UEFA callers supply a complete league-phase draw, clubs, coefficients, discipline
points and dates. There is no production draw generator or UEFA exception/waiver
mechanism. Ranking implements final standings; interim display criteria and
unresolved final sporting decisions still need their application workflow.
Future seasons currently reuse the verified 2026/27 format as a simulation, not
as a claim that future regulations have been confirmed.

Portugal's 2026/27 eight-club League Cup must **not** simply repeat unchanged:
Liga Portugal announced a new 2027/28 format. Verify the complete new regulation
and eligibility before migrating active Portuguese careers. Taça de Portugal
entry groups must also be derived from each new season's qualified field.

## Validation

`PATH=/workspace/scratch/boss-tools/jdk/bin:$PATH bash tools/test-core.sh` passed:

- All existing gameplay, league, promotion, world-market, Portuguese-cup and
  backup regression suites.
- Domestic season engines: 32 DFB-Pokal and 32 FA Cup tournaments, twenty-year
  archive recurrence, late entry, home rights, two-leg order and corruption.
- UEFA league phases: 36 completed phases across all three formats, all ranking
  criteria, pot/association constraints, qualification and save validation.
- UEFA knockout: 96 brackets with save/reload during regulation, extra time and
  penalties; venue inheritance after upsets, fixed bracket paths and all 45 games.
- Thirty annual UEFA rollovers across all three competitions; fresh changing
  entrant IDs, unchanged original archives and league-to-knockout restoration.

The Android app also compiled and its development APK signature verified locally
with `tools/build-sdk.sh`. No new playable feature APK is being distributed.

The CSV draws in `tests/fixtures` are synthetic validation fixtures, not actual
UEFA draw data. These tests prove engine behavior, not playable Android flows.

## Integration still required

1. Finish national qualification/entrant selection and fill missing lower tiers.
2. Implement Portugal's 2027/28 League Cup and migrate existing completed cups
   without losing saved 2026/27 winners.
3. Connect domestic and UEFA access selection to actual complete season tables,
   cup winners, titleholders and qualifying rounds; generate valid UEFA draws.
4. Reconcile all league/cup/UEFA dates, rest periods, player recovery and finance.
5. Wire engines into MainActivity, watched/quick matches, calendar/history UI,
   career persistence and typed backup validation.
6. Add remaining countries' cup formats after verifying their current rules.
7. Run Android 35/36 career and migration scenarios for the new playable flows
   before distributing an APK that claims these features.

## Primary rules checked on 7 October 2026

- [DFB-Pokal draw and match rules](https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/modus)
- [DFB round windows](https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/rahmentermine)
- [FA Cup 2026/27 rules](https://www.thefa.com/-/media/thefacom-new/files/competitions/2026-27/rules/rules-of-the-fa-challenge-cup-2026-27.ashx)
- [FA Cup round dates](https://www.thefa.com/competitions/thefacup/round-dates)
- [Champions League league-phase draw](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-16-Draw-system-league-phase-Online)
- [Conference League league-phase draw](https://documents.uefa.com/r/Regulations-of-the-UEFA-Conference-League-2026/27/Article-16-Draw-system-league-phase-Online)
- [Champions League final ranking](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-18-Equality-of-points-league-phase-Online)
- [Europa League final ranking](https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-18-Equality-of-points-league-phase-Online)
- [Champions League knockout draw](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-19-Draw-system-knockout-phase-Online)
- [Europa League knockout draw](https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-19-Draw-system-knockout-phase-Online)
- [Conference League knockout draw](https://documents.uefa.com/r/Reglement-de-l-UEFA-Conference-League-2026/27/Article-19-Procedure-pour-le-tirage-au-sort-de-la-phase-a-elimination-directe-Online)
- [Champions League Annex B](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Annex-B-UEFA-Champions-League-Competition-System-Online)
- [Liga Portugal 2027/28 League Cup announcement](https://www.ligaportugal.pt/news/28134/allianz-cup-com-novo-modelo-competitivo-em-2027-28)
