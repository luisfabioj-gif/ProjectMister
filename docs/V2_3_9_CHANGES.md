# Passing Android integration gate — 9 October 2026

Source `d805d5a0628cebeb57b3f302caf1e0d2eb798d5f` passed both Android 35/36 jobs
in [run 37953129633](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37953129633).
The corrected third-season check exercises the actual managed league-phase
fixture after earlier opponent fixtures advance. It confirms one watched and
one quick league-phase result, at least eleven quick-match appearances, two
exact new-format League Cup archives, three complete lower-division archives,
chronological dates, saves, backups and corrupted-backup atomicity. Startup,
upgrade, navigation, existing countries' cups, release installation and
separate-key restore also pass the run. This is emulator evidence, not physical
phone testing or a finished-game handoff. API35 screenshots 40/41 were inspected;
the cup summary's excess spacing is a presentation follow-up.

# Portuguese recurring seasons — development checkpoint, 9 October 2026

The recurring Portuguese seasons are now verified by the passing gate above.
They add 5 points to the completion tracker (74 verified, 26 remaining).
The UEFA draw engine is source preparation and earns no verified UEFA points.

The first integrated Android run, 37861517495, failed on both API levels during
2027 rollover with `No safe league date`. The new League Cup's 8 August match
and 11 August rank-decision window block every date in the old four-day search
around the 9 August league opening. The calendar now preserves existing valid
dates and searches forward when necessary, retaining at least three days of
recovery. A bounded impossible calendar still fails explicitly. Core regressions
cover 72 combined cup calendars across 24 years, all rest gaps and deterministic
recalculation. The passing gate above verifies this calendar correction.

Run 37951724923 confirmed that rollover no longer crashes and reached the third
Portuguese season on both API levels. The next failure was the gate's exact
watched/quick league-phase assertion: it inspected the opponent fixture before
Continue simulated earlier ties and opened the manager's actual game. The gate
now inspects that active game, requires exactly two managed league-phase games,
and executes one through the same quick-result completion path used by the UI.
The two-game/one-quick requirements, appearances, dates, saves and archives remain
required. Verification awaits the subsequent Android run.

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
