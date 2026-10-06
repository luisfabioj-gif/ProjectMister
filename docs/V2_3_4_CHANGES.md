# BOSS XI 2.3.4 development candidate

## Portuguese League Cup

New linked Portuguese careers contain the published 2026/27 eight-club draw.
Single-match quarter-finals lead to fixed semi-final pairings and a neutral
Leiria final. Draws go directly to simulated penalties; shootout goals do not
count as match goals. The manager watches their own matches; the other ties
are simulated as the calendar reaches them. League standings, result ledgers,
training weeks and weekly finances are not advanced by a cup match.

Cup events interrupt the weekly league calendar. Completing a cup match returns
to the next scheduled event without shifting all subsequent league dates.
Only established opponents appear in the draw. Cup scores and the winner are
saved, backed up and retained as a 2026/27 career archive after season rollover.
History links to that archive separately from real-world historical winners.

## Sources checked 6 October 2026

- Liga Portugal 2026/27 competition regulations, Annex III, articles 6, 9–11:
  https://www.ligaportugal.pt/backoffice/assets/20260701_RC_2026_27_f53785bcd4.pdf
- Published draw:
  https://www.ligaportugal.pt/news/27935/definidos-os-quartos-de-final-da-allianz-cup-20262027
- Quarter-final dates and Final Four announcement:
  https://www.ligaportugal.pt/news/28770/leiria-volta-a-receber-final-four-da-allianz-cup
  Quarter-finals: 27–29 October 2026; semi-finals: 5–6 January 2027;
  final: 9 January. Assignment of each semi-final to an individual day is
  simulated and labelled in the app. Kick-off times are not modelled.
- The separate 2027/28 rules introduce a league phase and playoffs, with
  European qualifiers entering the quarter-finals:
  https://www.ligaportugal.pt/backoffice/assets/20260701_RTLIGA_27_28_329c7f6d07.pdf

## Limits

- Existing careers are not assigned retrospective cup fixtures or scores.
  Start a new linked Portuguese career for the playable 2026/27 cup.
- The changed 2027/28 format is not active. The app retains the completed
  2026/27 archive rather than repeating an obsolete format as official rules.
- League dates remain simulated weekly dates; cup prize money, a separate
  cup statistics table, discipline and national player eligibility exceptions
  are not implemented. Other domestic cups and UEFA competitions remain open.
- This is a development-signed phone-testing candidate, not a Play release.

## Validation

Core checks exercise all 128 tournament winner paths, the fixed bracket,
chronological processing, direct penalties, snapshots at shootout boundaries,
backup round trips, malformed saves and date validation. Android QA adds a
watched quarter-final, semi-final and final, intervening background ties,
reload/backup after each stage and league resumption with unchanged league
records and weekly finances during cup matches. Device gate results are
recorded after CI completes.
