# Portuguese recurring seasons — development checkpoint, 9 October 2026

This checkpoint is pending Android validation. It earns no additional verified
completion points. The latest passing Android build is the eight-country cups
source `f781e2d9a5a8cbdd90a2de12759a62aac8a0355a`, run 37858104698.

## Connected career behavior

New expanded Portuguese careers include all twenty catalog professional
leagues, the Portuguese national-cup clubs and five further B-team identities:
483 clubs and 9,660 generated, fictional players. Existing career club orders
are preserved; these new lower competitions require a fresh expanded career.

Liga 3 runs two ten-club regional leagues, the eight-club promotion league and
two six-club survival groups. Regional qualification is frozen after eighteen
rounds; survival ranking includes earned bonuses. Campeonato de Portugal runs
four fourteen-club leagues in 2026/27, promotion groups, four survival playoffs
and a neutral championship final. The next edition expands to four groups of
sixteen. Each phase stores actual simulated results, used-player ages and
neutral deciding games when necessary. These results, not random entrant lists,
determine the next professional field and lower replacements.

The Liga 2/Liga 3 barrage saves both legs, extra time and penalties. Managed
barrage matches use the live/quick match system. A B team stays below its parent;
Liga 2 has at most five B teams. Vacancies use the recorded sporting order and
barrage/relegation outcomes. A managed club relegated outside the two playable
professional divisions leads to a professional-club job choice before rollover.
The old club, squad, league summary and world remain available.

The published new Taça da Liga starts from 2027/28: an odd-field preliminary,
two distinct league-phase opponents with one home/one away fixture, European
quarter-final byes, seeded playoffs and quarters, then Final Four. League-phase
matches preserve ordinary draws and record only players who appeared. Actual
used-player ages break otherwise equal ranks. Neutral sporting decisions after
all published criteria are saved and shown; no club-ID qualification is awarded.
Cups and lower seasons roll over with exact immutable archives. The 2026/27
League Cup remains separately accessible in History. Save/backup validation
covers every new field before any preferences are replaced. The backup envelope
now allows 64 MiB to accommodate several expanded, long-career save slots.

## Sources and explicit projections

- Published new League Cup regulation, articles 3, 7–11 and 27:
  https://www.ligaportugal.pt/backoffice/assets/20260701_RTLIGA_27_28_329c7f6d07.pdf
- General professional regulation, article 18 (rank decisions), article 26
  (Liga 2/Liga 3 replacements) and Annex V (reserve restrictions):
  https://www.ligaportugal.pt/backoffice/assets/20260701_RC_2026_27_f53785bcd4.pdf
- Liga 3 2026/27 format:
  https://www.fpf.pt/DownloadDocument.ashx?id=32598
- Published preceding Liga 3 regulation for phase/whole-competition ranking,
  discipline, used-player mean age and neutral deciding matches:
  https://www.fpf.pt/DownloadDocument.ashx?id=30934
- Campeonato de Portugal 2026/27 format:
  https://www.fpf.pt/DownloadDocument.ashx?id=32599
- Official association explanation of the four-phase format:
  https://afatv.pt/news-details/32cc0a33-f2e0-4924-a469-0e5e43f20377
- Santa Clara B published 2026/27 regional fixtures:
  https://cdsantaclara.com/sorteio-da-equipa-b-no-campeonato-de-portugal/

Future dates and regional draws are simulated. Four projected qualifying groups
among the available real district clubs supply twenty result-derived next-field
entrants; these are clearly labeled in the game and are **not** presented as the
official twenty association championships. Future 64-club fourth-division fields
retain the projected promotion/survival format; no unissued later regulation is
claimed. The Liga 3 ten-point survival-bonus wording gap is interpreted as the
positional bonus without an additional points bonus.

Applying general LPF neutral rank-deciding matches to a remaining new-League-Cup
tie is an interpretation of article 27, not an explicit extra article in that
cup regulation. The literal preliminary pair can become unavailable because
article 3 excludes B teams. This otherwise unspecified case uses a simulated
organiser replacement: actual promoted clubs, the barrage survivor and ranked
retained Liga 2 clubs have priority. European exemptions currently follow the
provisional base league/cup allocation; full UEFA/titleholder/performance-place
qualification remains a separate pending integration.

## Validation

Core regressions pass 72 new-format League Cups, saved neutral rank decisions,
18 Liga 3 seasons, twelve fourth-division seasons and ten complete Portuguese
pyramid years with reserve restrictions, vacancy replacement, 56-to-64 expansion,
district results and canonical archives. The native Android gate adds three
Portuguese career seasons, watched and quick league-phase matches, the saved
barrage, fresh cup/lower fields, archive reloads and atomic malformed-backup
rejection. Device evidence is required before marking any milestone verified.
