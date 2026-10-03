# National rules verification — 3 October 2026

These are implementation inputs, not claims that the game already enforces them.
Linked Scottish careers implement the Premiership/Championship playoff ladder.
German top/second-tier transitions are now implemented, awaiting device validation.
Other countries still retain division membership at season end. Scottish unresolved
qualification ties and the lower-pyramid boundary remain unimplemented.
Never silently substitute a generic playoff or a club-index tie-break for these rules.

## England

2026/27 Championship playoffs expand to six teams (places 3–8). Places 3/4 enter
at the semifinals; 5–8 contest single-match eliminators. Confirm the final seeded
pairings against the current handbook before encoding them. The older EFL
four-team evergreen explainer is superseded.

https://www.efl.com/news/2026/march/05/efl-statement--sky-bet-championship-play-off-format/

Premier League equal-points order: overall goal difference, goals scored,
head-to-head points, head-to-head away goals; consequential unresolved ties need
a deciding match. Do not extend this rule automatically to the Championship.

https://www.premierleague.com/en/news/4638196/could-the-premier-league-title-be-won-on-goal-difference

## Scotland

Premiership: 33 rounds, then fixed upper/lower groups of six for five rounds.
Bottom club relegated; Championship champion promoted. Championship 3 v 4,
winner v 2, winner v Premiership 11, all two-legged; higher-ranked club hosts
second. Aggregate ties use extra time and penalties. Championship 10 relegated;
9 enters a separate lower-league playoff.

C36: points, goal difference, goals scored, head-to-head points, head-to-head
GD. C37 requires a neutral deciding match for consequential unresolved ties;
C38 otherwise permits shared positions. Split allocation is itself consequential.

https://spfl.co.uk/admin/filemanager/images/shares/pdfs/MASTER%20-%20Rules%20and%20Regulations%20(CLEAN%20-%2029%20July%202026).pdf

## Portugal

Two eligible second-tier clubs promoted automatically; top-flight bottom two
relegated. Next eligible second-tier club meets top-flight 16 in a two-leg playoff;
leg order drawn, aggregate ties use extra time then penalties. B teams cannot
promote; parent-club relegation constraints also require implementation.

Article 18: head-to-head points, head-to-head GD, overall GD, wins, goals scored.
During the season omit head-to-head GD until both meetings are played.
Remaining final ties require neutral match(es); interim ties share a position.
Use group mini-tables, not a non-transitive pairwise comparator.

https://www.ligaportugal.pt/backoffice/assets/20260701_RC_2026_27_f53785bcd4.pdf

## Netherlands

Two highest first teams in Eerste Divisie promote, excluding reserve teams.
2026/27 has THREE playoff rounds, unlike the 2025/26 four-round document.
Six second-tier entrants include period qualifiers, with replacement rules,
plus Eredivisie 16. Seed the six by final standings: 6 v 1, 5 v 2, 4 v 3;
semifinals A-winner v B-winner and Eredivisie club v C-winner; final between
those winners. All ties two legs, higher seed/top-flight club hosts second;
extra time and penalties after tied aggregate. Dates in this regulation remain
provisional. Reserve/parent and lower-pyramid rules are in separate regulations.

https://www.knvb.nl/downloads/bestand/30197/reglement-play-off-promotie-degradatie-2026-27
https://www.knvb.nl/downloads/bestand/30190/promotie--en-degradatieregeling-betaald-voetbal-seizoen-2026-27
https://www.knvb.nl/downloads/bestand/30195/reglement-periodekampioenschappen-eerste-divisie-2026-27

The period document's listed third interval starts at round 21, leaving round 20
unassigned; verify this apparent inconsistency before coding the period calendar.
Disciplinary ranking needs actual recorded disciplinary data, not fabricated zeroes.

## Türkiye

1. Lig has 20 clubs. Top two promote; places 17–20 relegate. Third enters playoff
final directly. Fourth hosts seventh, fifth hosts sixth in single games; winners
meet over two legs with higher finisher hosting second. Winner meets third in
neutral single final. Ties use extra time then penalties. Read player eligibility
appendices separately; a correct playoff does not imply squad rules are implemented.

https://www.tff.org/Resources/TFF/Auto/18c19be2866242679eb483a7a154e0cf.pdf

## Spain

Promotion playoffs: 3 v 6 and 4 v 5, two legs in both semifinals and final, higher
league finisher hosts second. No away-goals rule; tied aggregate goes to extra
time, then the higher league finisher advances, without a shootout. Apply reserve
eligibility before selecting entrants. Verify current RFEF competition bases for
full league ordering and relegation rules before activating season transitions.

https://www.laliga.com/es-DE/noticias/quienes-juegan-el-play-off-de-ascenso

## Belgium

Top flight expands to 18 with 34 rounds and no playoffs; bottom two relegated.
Second tier has 15: champion promoted, places 2–5 contest two-legged semifinals
and final, higher seed hosts second. Bottom two relegated. Abolition of the U23
relegation quota is not permission for reserves to enter the top flight.

https://www.proleague.be/nieuws/qanda-wat-verandert-er-aan-het-competitieformat
https://www.proleague.be/nieuws/challenger-pro-league-heeft-nieuw-format-zonder-quota-voor-u23

## France

2026/27 Ligue 2: 34 rounds, opening 8 August, final 22 May. Fourth hosts fifth
on 25 May, winner visits third on 28 May; survivor meets Ligue 1 16 on 3/6 June.
Ligue 2 16 meets Ligue 3 third on 1/6 June. Preserve winter and international
breaks using the published round calendar, rather than weekly dates.

https://www.lfp.fr/article/ligue-2-bkt-le-calendrier-de-la-saison-2026-2027

## Germany

Verified against DFL SpOL dated 11 June 2026, sections 2 and 3:
- Both tiers: points, GD, goals scored, direct-meeting aggregate, direct-meeting
  away goals, all away goals. If the meetings are incomplete, use only GD/goals
  after points; unresolved interim positions are shared. Final unresolved ties
  require a neutral deciding match. Do not prematurely use one direct meeting.
- Two automatic promotions/relegations. Bundesliga 16 faces second-tier third
  over two legs, with extra time and penalties after a tied aggregate.
- Return-leg home advantage belongs to the club with FEWER free days before
  the first leg; draw lots if equal. It is not intrinsically given to either tier.
  This requires the actual fixture calendar before seeding venues.
- Second-tier bottom two relegate; 16 meets third-tier third over two legs with
  the same rest-day venue rule. No third-tier world currently exists in the app.
- Licensing adjustments in section 3 are not yet simulated.

https://media.dfl.de/sites/2/2026/06/Spielordnung-SpOL-2026-06-11-Stand.pdf

https://www.bundesliga.com/en/bundesliga/news/how-does-promotion-and-relegation-work-in-the-bundesliga-10645

## Italy — verification incomplete

Do not use the 2016/17 Lega B playoff explainer as current regulation. It contains
historical point-gap thresholds. Obtain the current FIGC/Lega B notice covering
2026/27 promotion, playoff eligibility, playout exemptions and tied-score rules.
Also verify Serie A consequential relegation/title ties separately.

https://www.legab.it/documentazione
