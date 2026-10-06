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
records and weekly finances during cup matches. Both Android 35 and 36 passed the complete device gate in run
37537592141 (source e1b921b2c0b379168ee3663fea1578130dd64378):
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37537592141

The gate also passed all forty career configurations, all ten national playoff
paths, old-save upgrade, compact controls, AAB-derived installation and
different-key backup restoration. Reviewed API 36 cup-final and bracket
screenshots: the competition label, controls, dates, scores and separate
shootout results are readable. A queued wage-budget toast from the preceding
negative transfer test appears in the bracket capture; it is not cup UI.

Delivered APK: BOSS-XI-v2.3.4-Portuguese-League-Cup.apk (2,347,648 bytes),
version code 29, version 2.3.4-dev, artifact 11447766763.
SHA-256: 9f5c43a6a785bfaf96798f2492b787e45d180181fd6e5e739d5a0ec9566574bb.
Existing development signing certificate retained.

## Phone test

Back up from Career Hub before updating. Install the APK over the current
development build. In a spare slot choose Liga Portugal Betclic and Benfica,
then open Calendar → League Cup. Progress the league into late October for
the first cup match. Check tactics, substitutions, save/reload, the next
league date, transfers, history and stadium screens. Send screenshots or a
short recording with the club, game date and steps for any issue.
