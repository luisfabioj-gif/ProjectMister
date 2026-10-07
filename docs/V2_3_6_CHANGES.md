# BOSS XI 2.3.6 — recurring Taça de Portugal candidate

The Taça de Portugal now creates a fresh playable edition when a linked
Portuguese career starts its next season. Current division membership controls
entry rounds; reserves remain excluded. Completed editions, winners, draws and
all scores are retained in History and in career backups.

Each following edition uses a newly shuffled opening field, recalculated byes,
staged league entrants and six deferred clubs from the provisional Portuguese
European access baseline (the top three, domestic cup winner where known, and
remaining league places). This is a simulated future edition: the existing
lower-tier club pool remains, future federation entry lists/calendar changes
and UEFA rebalancing are not predicted. No UEFA tournament place is created by
this deferred-entry calculation. Careers already past an unplayed season do
not invent a missing trophy or use an older winner as that year's cup winner.

The original TACA26 save format and draw remain unchanged. The new TACA2 format
owns a season year and variable entry-group sizes. Old completed editions move
to an immutable archive on rollover. Missing historical editions remain absent.
New archives are validated before loading/restoring; malformed data does not
replace existing preferences. A bounded archive blocks rollover before loss.

Cup calendar spacing and postseason wages/recovery/ageing now apply in subsequent
years. A failed cup/archive preparation leaves the existing season intact.

## Checks

Core regression suites pass, including twenty consecutive full Portuguese cup
editions, 145 ties each, changed deferred entrants (including a lower-tier cup
winner), cup/league rest gaps, archive persistence and backup encoding. Android
application and independent QA runner compile locally.

Device QA now plays two full Portuguese league/cup seasons, reloads during the
second edition, starts a third year, checks two trophy archives and exactly two
ageing passes, and rejects a corrupt archive during backup validation. Candidate
Android 35/36 results will be recorded after the workflow finishes.

## Still open

This does not complete the user's wider competition request. The changed
2027/28 Portuguese League Cup, playable UEFA admissions/qualifying rounds,
foreign-world opponents and the other countries' playable cups remain unfinished.
The UEFA/FA Cup/DFB-Pokal engines from the previous checkpoint remain foundations.

The full official 2027/28 Portuguese League Cup regulation was retrieved on
7 October 2026:
https://www.ligaportugal.pt/backoffice/assets/20260701_RTLIGA_27_28_329c7f6d07.pdf

It specifies two league-phase games (one home, one away), a conditional preliminary
qualifier when the phase would have an odd field, result/opponent-strength/used-
player-age tiebreaks, seeded playoffs and quarter-finals, and a neutral Final Four.
The preliminary qualifier requires actual Liga 3 promotion and second-division
relegation-playoff outcomes, which this two-division career currently lacks.
Using arbitrary bottom clubs would not implement that rule correctly.
