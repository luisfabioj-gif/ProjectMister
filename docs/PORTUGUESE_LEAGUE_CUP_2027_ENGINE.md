# Portuguese League Cup 2027/28 engine — 8 October 2026

This is an engine checkpoint, not a new playable APK. The delivered 2.3.6 build
remains the verified recurring Taça de Portugal release; the later 2.3.7 APK
adds a playable recurring German cup. MainActivity does not
yet instantiate this new League Cup model, and its career backup fields are not
connected to these snapshots.

## Implemented and tested

`PortugueseLeagueCupSeason` accepts a qualified field, ordered direct
quarter-final entrants, actual preliminary qualifiers when required, and an
explicit calendar. It implements:

- A preliminary match only when the league-phase field would otherwise be odd.
  Missing qualifying identities are rejected; no arbitrary bottom clubs are used.
- Two league-phase fixtures per club: one home, one away, distinct opponents.
- Points, goal difference, goals, collective opponents' points/goal difference/
  goals, then the lower mean age of players actually used. The used-player set
  counts each player once across both matches. Ages are supplied in days at the
  common final league-phase date; conflicting age/identity data is rejected.
- Playoffs pairing the highest qualifying finisher at home to the lowest,
  then second to second-lowest. Their number depends on direct QF entries.
- Eight quarter-final clubs, seeded 1–8, 2–7, 3–6 and 4–5, then the fixed
  semifinal paths (QF1 vs QF3, QF2 vs QF4), and a neutral final.
- Direct penalties after drawn knockout matches; no extra time.
- Chronological result replay, saved pending shootouts, fixture validation,
  bounded snapshots and explicit unresolved-ranking state. A final unresolved
  sporting tie is retained and reported, never decided using club ID.

`PortugueseLeagueCupSeasons` requires a finished edition before replacement,
retains immutable complete archives and rejects reused years or already-played
next editions. New qualified fields and dates must be supplied each year.

The final core test run passed all existing regressions plus 72 complete new-
format tournaments, odd/even fields with five/six/seven direct qualifiers,
shootout reloads, home/away constraints, seed paths, invalid appearance/event
rejection, a used-player-versus-appearance-count age test, and ten annual
archive rollovers. The Android application compiled locally.

## Integration still required

The lower-promotion qualification bridge is now implemented separately in
`PortugueseLowerPromotion`. Given eligible final Liga 3 and Liga 2 standings,
it saves both playoff legs, extra time and penalties, derives promotion and
relegation from the winner, and builds the next League Cup field and its actual
preliminary pairing. Reserve teams are excluded after movement, so a relegated
reserve team changes field size and preliminary-round parity correctly.
The published 2026/27 playoff has Liga 3 at home first; later seasons take an
explicit draw outcome. Existing club IDs are retained.

Tests cover both playoff winners, both later-season venue orders, reloads during
the tie, invalid snapshots, qualification before completion, relegated direct
entrants, and reserve relegation changing cup eligibility. Core regressions and
local Android compilation passed. This bridge is not yet called by MainActivity
and does not generate Liga 3 standings or alter career divisions itself.

1. Produce actual career Liga 3 promotion and Liga 2 relegation-playoff outcomes.
   Article 7's preliminary fixture uses the second promoted Liga 3 club and the
   relegation-playoff winner, with the better previous-season finisher at home.
   The current two-division career does not produce these outcomes.
2. Supply verified eligibility and direct-qualifier ordering from completed
   domestic/European access outcomes. The constructor checks field consistency;
   it cannot certify the sporting provenance of caller-supplied club IDs.
3. Record players actually used in watched and quick-result league-phase games,
   with a consistent age reference date. Match registration rules still belong
   in the shared registration layer.
4. Connect phase-specific fixtures, result presentation, calendar/rest scheduling,
   prize money, current-career persistence, backups and historical UI.
5. Migrate the old 2026/27 League Cup archive without rewriting its results.
6. Add a workflow for an unresolved final sporting decision and any organiser
   exceptions; no undocumented tiebreak is invented by this engine.
7. Run real Android career scenarios before claiming the new format is playable.

Dates are supplied and labelled by the caller. Test dates are simulations and
future formats are not represented as confirmed regulations. The league-phase
draw is a shuffled cycle split into two perfect matchings: a valid simulated
draw, not the actual official fixture list or a reconstruction of its ceremony.

## Rules

Primary regulation, linked by Liga Portugal's statutes/regulations page:
https://www.ligaportugal.pt/backoffice/assets/20260701_RTLIGA_27_28_329c7f6d07.pdf

Liga 3 / second-division playoff dependency checked against Liga Portugal:
https://www.ligaportugal.pt/news/28154/definido-o-emparelhamento-dos-playoffs-202627

FPF 2026/27 Liga 3 format reference (20 clubs, two initial groups of ten):
https://www.fpf.pt/DownloadDocument.ashx?id=32598
