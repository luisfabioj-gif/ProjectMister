# Competition expansion — checkpoint, 7 October 2026

## New League Cup format engine — 8 October 2026

[PORTUGUESE_LEAGUE_CUP_2027_ENGINE.md](PORTUGUESE_LEAGUE_CUP_2027_ENGINE.md)
records the new league phase, conditional qualifier, seeded playoffs, Final Four,
used-player-age tiebreak and annual archives. Core tests cover 72 tournaments and
ten annual rollovers. This model is not connected to playable careers yet;
Liga 3 promotion and Liga 2 relegation-playoff outcomes are a required dependency.
The latest playable test APK remains 2.3.6 below.

## Verified recurring Taça de Portugal — 2.3.6

[V2_3_6_CHANGES.md](V2_3_6_CHANGES.md) connects annual Portuguese domestic cups to
playable careers, updated entry groups, calendar/recovery, trophy history and
backup validation. Core tests and Android 35/36 QA passed run 37693888225, tested source
77a220dfb50ec48079a461e97d1e1674ffd6e09d. The History screenshot was reviewed. The broader UEFA, new Portuguese League Cup and other-country cup
integration remains unfinished.

## New engine checkpoint — recurring cups and UEFA, 7 October 2026

[RECURRING_COMPETITIONS_ENGINE.md](RECURRING_COMPETITIONS_ENGINE.md) records the
new season-owned domestic cup engine, DFB-Pokal/FA Cup format factories, all
three UEFA league-phase models, complete knockout brackets and annual archives.
Core tests pass, including 96 UEFA knockout brackets and thirty successive
European seasons. **Career screens, qualification, calendars and backups are
not connected to these new engines yet.** This is not a new playable release;
Portuguese recurring seasons and the remaining countries' cups remain open.

## Latest verified test build — 2.3.5

See [V2_3_5_CHANGES.md](V2_3_5_CHANGES.md) for the 146-club Portuguese domestic
cup, expanded player world, shared cup calendar, quick results and postseason
processing. Source 818703bf28f911e74652e11954693b803d97fa13 passed Android 35/36
verification in run 37607971403. The full-season test also exposed and verified
a fix for player ageing/development values being regenerated on reload.

## Previous verified build — 2.3.4, 6 October 2026

See [V2_3_4_CHANGES.md](V2_3_4_CHANGES.md) for the Portuguese League Cup.
Android 35/36 passed run 37537592141, source e1b921b2c0b379168ee3663fea1578130dd64378.
Worldwide recruitment, free agents, competition history, real Portuguese names
and stadium/portrait artwork were delivered in 2.3.3. The entries below describe
older checkpoints; their cup availability statements are superseded by these
versioned changes. Recurring cups, UEFA and other countries' domestic cups remain
unfinished.

## Verified calendar integrity and season archive — 6 October 2026

Legacy careers no longer fabricate cup or European fixtures/results when a date
passes. The calendar displays stored playoff ties, regulation/extra-time/penalty
results and actual winners. Shootout scores name the final-leg home and away
clubs. League dates remain explicitly simulated; playoff dates are not assigned.
Cups and European competitions remain unimplemented, with explicit empty states.

Database careers now archive the managed club's completed league season before
rollover: stable identity, displayed name, division, W/D/L, goals, points and tier
movement. The archive survives saves and backups and appears in Season history.
Old seasons cannot be reconstructed; no missing rankings or trophy claims are
invented. Legacy classic careers do not use this database-season archive.
Repeated rollover at round zero is blocked. Invalid archive data blocks loading
or backup replacement rather than being discarded. The archive supports 1,000
seasons; reaching that bound preserves the completed season and blocks rollover.

Core archive tests, application compilation and independent QA compilation pass.
Android QA adds recorded-calendar checks plus seven-country archive reload,
backup and duplicate-rollover checks. Both Android 35 (job 112224912909) and
36 (job 112224913027) passed the complete gate:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37450284832
Tested source: 85beac549dbf2b1698bfad88ade8c1e73d582d38.
All forty career variants, ten country playoff flows, release splits, legacy
upgrades and different-key restoration also passed. The calendar and Dutch
archive screenshots were visually reviewed. QA tables use injected standings
and scores to exercise transitions; they are not match-balance evidence.

Delivered APK: BOSS-XI-v2.3.2-Calendar-and-History.apk, 1,998,033 bytes.
SHA-256: 8853ec475c4b2e77d6df1b62133af8d367d5603e9b0199a8c46038ddf2bb2ead.
Artifact 11405188613 retains the verified APK; QA artifact 11405268670 contains
logs and screenshots. It uses the existing development signing identity.
The wider competition and production gaps below remain unfinished.

## Current verified build — all ten country transitions, 6 October 2026

Dutch promotion now derives period qualification from complete league history,
excludes reserves, handles repeated/automatic/last-place qualifiers and seeds the
six-tie bracket. Core tests cover all 64 winner paths and qualification edge
cases. The conflicting period-three start is interpreted from the KNVB calendar
and announcement; see NATIONAL_RULES_VERIFICATION.md.

Both Android 35 (job 112038547250) and Android 36 (job 112038546903) passed:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37391856850
Tested source: b3e5e53481e82cd2ab12258873b929d5e635bf14.
All 40 standalone/linked careers, watched playoffs in all ten countries, saved
season rollover, backup validation/restoration, release splits and different-key
migration passed. English, French, Italian and Dutch review screenshots and the
restored dashboard were inspected. Core tests cover every implemented knockout
winner path plus period qualification and ranking edge cases.

Missing disciplinary/deciding outcomes, lower-pyramid replacements, authentic
fixture calendars, playable cups/European admissions and physical-device/
production checks remain open. All ten transition implementations do not mean
all national rules or the full product are complete.

## Prior candidate — Italy, 6 October 2026

Italy's Serie A/B transition is implemented with the fourteen-point promotion
threshold, fixed preliminary/semi-final bracket, conditional final tiebreak,
and Serie A title/survival deciders. Local core tests cover 32 playoff paths and
saved phases. Device QA is extended to a watched five-match promotion and saved
20/20 season rollover. Android 35 passed run 37388502492, source ff06e8fb. Android 36 was blocked by an emulator System UI ANR over the qualification-guide button, confirmed in its screenshot; the failed job was rerun and passed (job 112038016660). The Dutch QA runner also preserves and dismisses only the two observed AOSP system ANR dialogs, with a strict retry limit. App ANRs are never dismissed.

This historical checkpoint preceded the Dutch implementation above. Its wider
competition and production gaps remain tracked in the current section.

## Prior candidate — England and France, 6 October 2026

Two more linked-country transitions are implemented: the six-club English
Championship playoff with reseeded semifinals, and France's two Ligue 2 single
matches followed by the two-leg Ligue 1 barrage. Country-specific standings,
saved playoff phases, watched matches, balanced tier movement and table badges
are connected. Core tests cover all 32 English and eight French winner paths.
Android 35/36 passed run 37387639632, source 73398af27c9f203587078929eb10c20ff0e68de3. Both promotion review screenshots were inspected; text/cards and navigation are readable. The run also passed backups, release-split install and different-key migration.

At this historical checkpoint, Netherlands transitions and the wider competition
and production gaps were still open. The current checkpoint above supersedes
its country coverage.

The prior screenshot archive fix passed Android 35/36 in run 37379146459,
source 06addafcd4df5154d462eb62ce1032c915ae003e, including the required PT/ES/BE
screen captures before uninstalling for the different-key migration test.

## Previous candidate — Belgium and signing-key migration QA, 5 October 2026

Source `e0aeafeed2acd4788774ec409f3e42933100f240` passed both Android 35
(job 111810605542) and Android 36 (job 111810605253):
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37324192531
This includes all forty career variants, watched PT/ES/BE playoffs, backups,
release-split installation and different-key uninstall/install career restoration.
The latter retained every stored field and reopened classic and linked careers.
The final migration screenshot was visually reviewed. Pre-uninstall screenshots
were removed by Android before the old end-of-run capture; the QA script now
archives them separately before uninstalling. That evidence-capture change is
pending its own device run; the successful gameplay result above is unchanged.

Belgium now applies its 2026/27 single-table 18-club top division and 15-club
second division: two relegations, one eligible automatic promotion, and seeded
two-leg semifinals/final for the second promotion. U23 teams cannot promote.
Table criteria include wins before goal difference and the later away-record
tiebreaks. Missing historical away records and final ties remain unresolved.
Parent/U23 relegation conflicts are detected. Licensing sanctions and movements
below tier two remain outside the implemented simulation.

Core tests cover all eight Belgian playoff paths, every saved knockout phase,
away-ranking criteria and U23 eligibility. Both Android APK and test runner
compile locally. Android QA adds watched Belgian playoffs and saved 18/15
transition, and a separate disposable-key uninstall/install/restore gate for
career migration. No production signing key or Play upload is involved.

The preceding Portugal/Spain/backup candidate (`c49eed09`) passed Android 35:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37321267896
Job 111800637235 passed all forty career variants, watched PT/ES playoffs,
backup export/cancel/validation/restore and AAB-derived release installation.
Android 36 stopped before emulator launch because SDK system-image extraction
failed with "Error on ZipFile unknown archive". The subsequent run above passed both API levels; the earlier run is not an
Android 36 game-test failure or pass.

Still required: England, Italy, France and Netherlands season transitions,
deciding matches, lower-pyramid replacements, authentic calendars, playable
cups/European admissions and full registration rules, plus production/physical
phone checks. The entries below are historical checkpoints.

## Prior candidate — Portugal, Spain and career backups, 5 October 2026

Linked Portuguese and Spanish careers now seed country-specific promotion
campaigns, retain every playoff phase in saves, and apply balanced tier changes.
Portuguese leg order is drawn once and saved; aggregate draws go to extra time
and penalties. Spanish semifinals/final give the better league finisher the
second leg and advancement after tied extra time, without a shootout. Reserve
teams are excluded from eligible places. A relegated parent/reserve collision
blocks rollover because lower-tier replacements are not implemented.

Country tables use the verified Portuguese/Spanish tiebreak criteria available
in the simulation. Unknown historical head-to-head results and unresolved
qualification ties are kept unresolved. Fair-play/deciding-match resolution is
still pending. Pure Java tests cover Portuguese retention/promotion and venue
draws, all eight Spanish winner paths, saved phases and corrupted fixtures.

Career Hub now offers an explicit full backup/restore flow through Android's
document picker. It includes three career slots, team edits and sound settings.
The versioned, typed format has bounded input and a corruption checksum; it is
not encrypted. Restore validates all careers before replacement and requires
in-app confirmation. Cancellation and invalid files preserve current data.
Storage commit failures attempt to restore the previous preference snapshot.
Android QA covers actual file writes, restored classic/linked careers, pending
playoffs, invalid files/club IDs and cancellation. Provider/device-specific
picker behavior and migration to a private production key still require QA.

Core tests pass. Candidate Android build/device results are recorded below
when verified; this entry does not claim Android QA has already passed.

Remaining competition scope: England, Italy, France, Netherlands and Belgium
season transitions; deciding-match ties; lower-pyramid movements including
parent/reserve collisions; authentic date/break scheduling; playable domestic
and European cups/admissions; complete national squad/registration exemptions.
Production signing, Console setup and physical-device performance/audio QA
also remain. This is not a completed release.

## Previous verified work — Türkiye, 5 October 2026 (UK time)

Resumed from the saved Türkiye changes after the verified German/Scottish
checkpoint `d0552b4`. Türkiye now has saved single-match eliminators, a two-leg
semifinal and a neutral final, with two automatic promotions and three relegations.
Standings use TFF head-to-head/group mini-table rules. Final unresolved qualifying
positions block seeding rather than using club IDs as a sporting tie-break.
Neutral simulated finals receive no home advantage. Single-match live/review
labels no longer describe a nonexistent second leg or aggregate.

Regression coverage includes all 16 winner paths, saves in every knockout phase,
neutral final shootouts, no away-goal tie-break, and retained group mini-tables.
Android QA adds watched eliminator/semifinal/final progression, save reloads,
unchanged league ledger, stable club/player identities and the 18/20-club season
transition. All pure Java/data regressions passed, the SDK-built APK compiled
and passed signature verification, and the independent Android test runner built.
Code checkpoint: `9698b3e7af2e034a99a026dcfb6ca89f640be1cc`.

Published with explicit user approval on 5 October to `work/boss-xi-v2.3`.
Remote code: `819477ab3104275d98668c1d4387f64e8077b1ec`; its tree exactly
matches the locally tested checkpoint. Android 35/36 run:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37272085051
Both Android 35 and 36 jobs passed core regressions, APK/AAB builds, legacy
save upgrades, all forty standalone/linked career seasons, watched Turkish
eliminator/semifinal/neutral-final progression and saved promotion into the next
season. Compact layouts, complete live matches and AAB-derived release installs
also passed. API 36 first encountered an external "System UI isn't responding"
dialog, visible in its failure screenshot. A fresh-emulator retry passed without
changing source or weakening test assertions.

Reviewed API 35 screenshots: turkish-neutral-final.png and
 turkish-promotion-review.png. Neutral-final label and team markers are legible;
playoff scores, extra time, penalties and aggregates are readable. The review
scrolls to its later rounds. Scores were injected by transaction tests and are
not real matches or match-balance evidence.

Verified APK artifact: 11329530153 (API 35), also 11328724248 (API 36).
These are development builds signed with the existing test key, not a completed
competition expansion or a production Play Store release.

Still unfinished: the other seven countries' season transitions; deciding-match
ties; lower-pyramid relegation; authentic date/break scheduling; playable national
and European cups and admissions; full national squad/registration exemptions;
production signing, Play Console setup and physical-device performance/audio QA.
The historical entries below record the progression, not the current feature list.

This is development work, NOT a completed 20-division release. Last verified
user-delivered version: 2.3.1, run 36594832195. Stable main remains untouched.

## Agreed scope

England, Spain, Italy, Germany, France, Portugal, Netherlands, Belgium,
Scotland and Türkiye; first and second divisions in each. Current 2026/27
club membership, country-specific registration rules, season calendars,
promotion/relegation and qualification for the following European season.

## Implemented in this checkpoint

- Connected `LeagueSchedule` to watched opponents, venues, background fixtures
  and the calendar for new careers. `fixture_version=2` is persisted; absent
  keys load version 0 and preserve legacy fixture order. Property tests cover
  10, 12, 15, 18, 20, 22 and 24 clubs. Android tests cover slot switching and
  calendar consistency across all 34 rounds.
- Added `assets/competitions/2026-27.json`: all twenty divisions, 365 unique
  stable club IDs, thirteen reserve sides, and primary source URLs per division.
  CompetitionCatalog loads bounded, immutable data; build validation checks
  expected membership counts and identity uniqueness. This catalog does not
  yet replace the legacy career world.
- Portuguese table now displays reference European entry stages and relegation
  zones, with an explicit projection disclaimer. It does not award admissions.
- Extracted verified 2026/27 ordinary registration windows for Portugal,
  England, France and Türkiye. The Portuguese career now uses that data.
  Unknown dates and unverified exact deadlines are not invented.
- Recorded the ten countries' provisional 2027/28 UEFA base access profiles.
  A qualification guide is accessible from the league table. It explicitly
  distinguishes reference rules from actual career admissions.
- Added Android 16/API 36 preparation, supported AGP/Gradle versions, an
  unsigned non-debuggable release bundle and API 35/36 emulator coverage.

## Audit findings / required remaining integration

MainActivity fixes club/player capacity at 18/360, fixtures at 34 and career
start at 9 August 2026. The league selector has one option. Legacy careers retain their old watched/background scheduling for compatibility;
new careers now share a valid whole-season schedule.
The apparent cup/European results are generated by date, not a playable
competition state machine; they cannot establish honest qualification.
Season-end changes ages/finances but does not enact league transitions.

Do not merely change the league dropdown or attach coloured zones and claim
that admissions work. Required next steps:

1. Integrate the sourced stable-ID club catalog into versioned careers.
2. Introduce save-versioned world/division membership and result ledgers;
   keep existing IDs and legacy saves stable. Avoid generating thousands of
   player views on the UI thread.
3. Connect the shared schedule to calendar, watched/background matches and
   results, with correct dates, breaks, byes and Scottish split handling.
4. Implement national tie-breakers from match ledgers, eligible promotion
   candidates/reserve restrictions, playoffs, and season transition.
5. Implement cups, actual European entry and qualifying/league/knockout
   progression. Domestic table badges must show next-season projections,
   cup dependencies and entry rounds; only confirmed outcomes get qualified
   status. Neither a fixed extra Champions League place nor a fixed cup-winner
   assumption is valid. Dutch Conference entry requires domestic playoffs.
6. Complete each country's transfer-window verification (professional men,
   domestic/international rules, deadline timezone and exceptions). Portugal's
   locally-trained U23 loan exception through 8 September needs player
   eligibility tracking before enabling it; ordinary window ends 4 September.
7. Test complete seasons, promotion swaps, odd-league byes, reserve-club
   exclusions, cup cascades, EPS/titleholder cases, old saves, and visual QA.

## Primary sources checked

- UEFA 2027/28 provisional access and calendar, circular 54/2026 (9 September):
  https://editorial.uefa.com/resources/02a9-218cd2086a08-882d0ce02937-1000/20260909_circular_2026_54_en.zip
  Listed at https://www.uefa.com/news-media/documents/circular-letters/
  Uses 2021/22–2025/26 association coefficients. Final rebalancing still pending.
- UEFA 2026/27 Champions League dates and current entrants:
  https://www.uefa.com/uefachampionsleague/news/02a6-20d57cfcd03e-407c22a7f465-1000--2026-27-champions-league-teams-dates-draws-format-final/
- Portuguese windows:
  https://www.ligaportugal.pt/noticias/28145/prazos-de-inscricoes-na-epoca-2026-27
- English windows:
  https://www.efl.com/news/2026/may/26/transfer-window-dates-confirmed-for-season-2026-27/
- French windows:
  https://www.lfp.fr/article/les-dates-du-mercato-2026-2027
- Turkish windows:
  https://www.tff.org/default.aspx?ftxtID=50633&pageID=687
- Dutch international windows (scope needs checking for all professional registrations):
  https://www.knvb.nl/assist-wedstrijdsecretarissen/veldvoetbal/overschrijvingen/internationale-overschrijvingen
  22 June–2 September 2026 and 4 January–2 February 2027, 23:59.
- Italian registration terms, FIGC circular 235, 2026/27: located in official
  files.figc.it results; summer 29 June–1 September, 20:00. Winter still to verify.
- Belgian reform: top tier has 18 clubs, no championship playoffs:
  https://www.proleague.be/nieuws/vanaf-seizoen-26-27-met-18-clubs-in-de-jupiler-pro-league
  Second division has 15 clubs and changed U23 rules:
  https://www.proleague.be/nieuws/challenger-pro-league-heeft-nieuw-format-zonder-quota-voor-u23
- Championship expanded playoffs (3rd/4th to semifinals, 5th–8th eliminators),
  contemporary reporting cites the EFL statement below. Direct fetch needs retry:
  https://www.efl.com/news/2026/march/05/efl-statement--sky-bet-championship-play-off-format/

## Verified club-list sources collected

Do not substitute 2025/26 rosters for current 2026/27 data.

- England top: https://www.premierleague.com/en/news/4675097/all-380-fixtures-for-202627-premier-league-season
  Opening ten fixtures identify all twenty clubs; Coventry, Ipswich and Hull
  are promoted. Check fixture uniqueness before importing this page wholesale.
- Spain top/second: https://www.laliga.com/laliga-easports/clubes and
  https://www.laliga.com/laliga-hypermotion/clubes (20 / 22).
- Germany top/second: https://www.bundesliga.com/en/bundesliga/clubs and
  https://www.bundesliga.com/en/2bundesliga/clubs (18 / 18); direct HTTP works
  when the search renderer fails.
- Italy second: https://www.legab.it/seriebkt/classifica (20); includes a
  four-point Juve Stabia deduction. An as-of-date start and a preseason start
  must not apply historical played results/deductions inconsistently.
- Italy promoted clubs are Venezia, Frosinone and Monza:
  https://www.legaseriea.it/serie-a/news/aspettando-il-calendario-della-serie-a-enilive-2026-27
- Netherlands top: https://eredivisie.nl/competitie/clubs/ (18).
- Scottish top opening fixtures identify all twelve clubs:
  https://spfl.co.uk/news/spfl-season-kick-off
- Portugal promoted Marítimo and Académico; third entrant/remaining squads
  still need complete verification:
  https://www.ligaportugal.pt/news/27935/definidos-os-quartos-de-final-da-allianz-cup-20262027

Additional confirmed calendar sources:
https://www.lfp.fr/assets/20251204_CA_Calendrier_General_26_27_Annexe_a0e398aab0.pdf
https://eredivisie.nl/nieuws/speeldagenkalender-betaald-voetbal-seizoen-2026-27-vastgesteld/
https://www.dfb.de/news/rahmenterminkalender-fuer-saison-2026/2027-festgelegt
https://spfl.co.uk/news/fixtures-qa-51267

Verification status is recorded in commits/CI; do not describe this checkpoint
as the completed league expansion or as ready for production publication.

## Android 16 investigation, 1 October

Run 36638214131 API 36 failed because a system dialog reading “Quickstep isn't
responding” covered the qualification button (QA screenshot 28). The game had
launched and loaded the baseline save. SmokeRunner now handles only that exact
AOSP launcher dialog, logs the environment interruption, and keeps BOSS XI
crash/ANR failures visible. Run 36892023940 validates the schedule integration.

Portugal top-flight membership completed from official opening fixtures:
https://www.ligaportugal.pt/news/28234/estoril-praia-fc-famalicao-abre-liga-betclic-202627
All other membership source URLs are preserved alongside each catalog division.
Names are factual identifiers; no club crests, kits or real player assets were imported.

Italian ordinary windows now support division-specific opening: Serie A
29 June, Serie B 1 July; both close 1 September 2026 at 20:00 Europe/Rome.
Winter 2 January–1 February 2027 at 20:00. `forDivision` prevents applying
Serie A's early opening to Serie B. Source: FIGC council statement, 27 April
2026 (URL embedded in RegistrationWindow). Other remaining dates stay
unverified rather than silently borrowing another country's window.

Additional fixture QA caught an imbalance hidden by full-season totals: the
initial circle rotation could allocate a rotating club all away games in the
first half-season. Corrected with alternating fixed-pair venues and stable
rotating-pair orientation. Tests now check first-half balance and a maximum
three consecutive home/away fixtures. Version 2 uses the correction; version 1
development saves retain their recorded venue order until a season migration.

## Transfer date verification completed for all twenty divisions

FIFA's public association pages embed the men's registration calendar under
`pageProps.association.pageData.transferRegistrationCalendar.men`. Reading that
page data resolved missing Belgian, Scottish, Spanish, German and Dutch dates.
The extracted date-only records and URLs are in TRANSFER_WINDOW_REFERENCE_2026-27.json.
FIFA's 00:00 timestamps are calendar placeholders, NOT verified midnight deadlines.
RegistrationWindow keeps unknown exact times null and rejects exact-instant queries
for them. Dutch 23:59 is additionally supported by KNVB's international-registration
page. Spain's FIFA winter close is 1 February, resolving the inconsistent 2 February
copy previously found on another page. Men's and women's calendars were not mixed.

All ten countries now have summer/winter dates available through forDivision,
with Italy's separate tier opening. The qualification guide displays these dates.
National exemptions (free agents, emergency keepers, youth and special loans),
UEFA squad registration deadlines, and future-season dates still require dedicated
eligibility handling; verified ordinary windows are not a claim to all regulations.

Cross-check against all ten FIFA men's association calendars found an important
scope difference: Portugal's FIFA association page gives 15 September / 6 February,
whereas Liga Portugal explicitly gives 4 September / 1 February for its professional
competitions. Keep the more specific league deadlines already implemented; do not
replace them from a blanket association import. Italy's FIFA opening is Serie A's,
not Serie B's. Both differences are retained in the reference JSON.

Additional rule research for the next integration milestone:
- EFL's 5 March statement confirms six Championship playoff participants and seven
  fixtures, with two-legged semifinals for third/fourth, but says final details were
  to be agreed later in 2026. Recheck the current playoff regulations rather than
  relying on the old evergreen four-team explainer.
  https://www.efl.com/news/2026/march/05/efl-statement--sky-bet-championship-play-off-format/
  https://www.efl.com/documents/play-off-rules/
- Belgian 15-club second tier: champion promoted, positions 2–5 play two-legged
  promotion rounds, better-ranked club home second, bottom two relegated. Reserve
  promotion eligibility must also respect parent-club rules; the newer removal of
  protected relegation quotas must not be confused with unrestricted top-tier entry.
  https://www.proleague.be/nieuws/challenger-pro-league-heeft-nieuw-format-zonder-quota-voor-u23
- SPFL confirms split rounds on 24/25 April, 1/2 May, 8/9 May, 11/12 May and
  15/16 May 2027; no Premiership fixtures on 26 September or 3 October. The
  third round-robin and split need a dedicated schedule, not a generic 34-game one.
  https://spfl.co.uk/news/fixtures-qa-51267

## Verified checkpoint, 1 October 2026

Code commit: 627006c378a5efffe1193ca2df966e94de3325df.
GitHub Actions: https://github.com/luisfabioj-gif/ProjectMister/actions/runs/36893728897
Both API 35 and API 36 passed Java/APK/AAB builds, baseline-save upgrade,
navigation, transfer interaction, match/half-time/substitution checks, full
match statistics, reload and compact landscape checks. Local core/data checks
and manual SDK builds also passed. Table screenshots were inspected on both
APIs; latest guide, match and compact tactics screenshots were also inspected.
Compact tactics still uses a scrolling pitch viewport; not all eleven markers
are visible simultaneously on the shortest landscape layout. Keep that as a
presentation follow-up rather than calling visual QA flawless.

Stable main and the previously delivered APK are unchanged. No completed
20-division APK or Play Store release is claimed. The catalog is shipped data;
active careers still use the legacy 18-club world. Remaining work is explicitly
listed above, especially world/save integration and actual competition outcomes.


## Playable-division integration, 2 October 2026 — awaiting device verification

Twenty catalog divisions are now selectable for new careers. CareerDivision saves
own their club identities and seed data; legacy saves restore the original world.
Squad, scouting, roles, table and result arrays follow the selected division size.
Players, budgets and ratings remain explicitly fictional. One division is active
per career at this stage; a two-division promotion world is not yet implemented.

All active fixtures share the schedule, including 15-club byes, the Scottish
Championship's four meetings, and the Premiership's 33-round plus 6/6 split.
The split is persisted and table groups stay locked. A bye advances club operations
without inventing a match or appearances. Android QA exercises all twenty full
seasons, save/world switching, a 24-club live match and a 15-club rest round.
Local property tests cover 21 schedule formats. Device results will be recorded
when CI completes; compilation alone does not validate this change.

Remaining explicit limitations: dates still use a simulated weekly calendar;
national tie-breakers, split venue optimisation, reserve eligibility, promotion,
relegation and playable domestic/European cups are unfinished. New division careers
do not show the legacy Portuguese fictitious cup results. The next-season action
retains the division and states that limitation. Later career seasons explicitly use a simulated registration calendar based on
2026/27. The transfer hub and season review label it as simulated; verified
2026/27 data remains unchanged. This is not a final release APK.

Further primary-rule verification, 2 October:
- SPFL rules updated 29 July 2026, C15–C17: 33 rounds then locked six-team
  groups; five split games, at least two at home. C18–C25: last relegated,
  eleventh enters a ladder against Championship fourth/third, then second;
  higher seed hosts leg two, aggregate ties use extra time then penalties.
  C36: points, goal difference, goals scored, head-to-head points and goal
  difference. C37–C38: unresolved consequential ties require a deciding game;
  otherwise equal positions. An arbitrary club-index fallback is not a final
  official tie-breaker. Store a complete results ledger before implementing it.
  https://spfl.co.uk/admin/filemanager/images/shares/pdfs/MASTER%20-%20Rules%20and%20Regulations%20(CLEAN%20-%2029%20July%202026).pdf
- Premier League's own explainer confirms 2026/27 Championship eliminators:
  fifth hosts eighth and sixth hosts seventh; third/fourth enter two-leg semis.
  https://www.premierleague.com/en/news/4611805/who-will-be-promoted-from-efl-championship-to-premier-league-for-2026-27-season
- Belgian Pro League Q&A explicitly confirms the top flight's bottom two
  relegated in 2026/27. Its older U23 quota paragraphs are superseded by the
  newer no-quota notice; do not import that obsolete protection.
  https://www.proleague.be/nieuws/qanda-wat-verandert-er-aan-het-competitieformat


Integration run 37000932886 passed on API 35 and 36 at code d9eb26d:
all twenty complete-season cases, 24-club live result/reload, odd-league bye,
Scottish split persistence, legacy upgrade, navigation and compact match tests.
Visual QA identified tight L/GD spacing; centred numeric cells and explicit
Scottish split dividers are being verified in the follow-up build. The new-career
screen now explicitly identifies its weekly fixture calendar as simulated.
The preceding 3437f80 run failed a QA setup assertion because the test stopped
the ticker before asserting that the match was active; d9eb26d corrected that.

## Verified selectable-division checkpoint

Code 06d80c85042e6be5d5beba7c76fa93c0c60be45e passed both API 35 and 36 in
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37001842373
All twenty full-season cases, legacy upgrades and existing feature tests passed.
Final API 35 Scottish and English table screenshots were inspected: numeric
columns are separated and the two Scottish groups are visibly labelled.

## Result-history integration — in development

LeagueResults now retains each played fixture and score, guards duplicate
completion, and provides group-only mini-table data for future tie-breakers.
A matchday results screen is connected to new division careers. The save stores
its ledger separately, scoped to the save-owned club order. Older missing
history is explicitly unavailable; aggregate tables are not reverse-engineered
into fictional results. Corrupt optional history falls back without discarding
the career. National tie-breakers and consequential-tie playoffs remain to wire.

## Result-history checkpoint verified, 2 October 2026

Code: 71b9b6e25bad78ab9652e3afc26bd7d3758bb214.
Run: https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37060661003
API 35 and 36 both passed APK/AAB build, installation, startup, legacy upgrade,
existing navigation/transfer/match/half-time/substitution regressions and all
20 complete-season cases. Each division's full result count survived save/reload.
Core tests cover duplicate/conflicting deliveries, malformed snapshots and
three-team mini-table calculations. The API 36 matchday-results screenshot was
inspected; fixture names, score rows and navigation fit the phone viewport.

Local compilation initially found a corrupt SDK D8 JAR in transient build tools.
Restoring that JAR from Google's official build-tools 35 archive fixed local APK
and QA-runner assembly; no app startup workaround or test gate was changed.

Next implementation: a shared country world with stable club/player identities
across both tiers, then country-specific tie-breakers and consequential playoffs,
reserve eligibility, promotion/relegation transactions, proper dated calendars,
and real cup/European admission progression. Current first/second divisions are
individually playable; changing league membership between seasons is unfinished.
This remains a development checkpoint, not the completed requested release.

## Linked-country checkpoint verified, 2 October 2026

Code: 009c12bf17c81080322540ed9de0ebd4e3467c33.
Run: https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37072048204
Both API 35 and 36 passed all gates. New careers include both national tiers,
with stable club/player IDs, separately browsable tables and shared background
simulation. The Scottish split also runs when managing a Championship club.
Forty complete-season cases cover the 20 prior standalone formats and all 20
linked starting divisions. Result histories and both memberships survive reload;
validated tier swaps retain identities and division sizes. Existing standalone
and classic careers remain compatible. The API 35 opposite-tier Scottish table
screenshot was inspected: group labels and all numeric columns fit correctly.

Follow-up: allow browsing stored results in either tier, not only the managed one.
National rules research is recorded in NATIONAL_RULES_VERIFICATION.md, including
superseded playoff formats and outstanding verification. Tier swaps are validated
at the model layer but are not yet driven by season-end promotion playoffs. The
full expansion and production Play release therefore remain unfinished.

## Linked results and release packaging verified, 3 October 2026

Code 9f595fb5cecba2e89072b0775799a111e64e02e6 passed all Android 35/36 gates in
run 37073864954, including the new other-division results visibility check and
release-bundle install/feature tests. The first results-view test incorrectly
waited on the UI thread, preventing rendering; moving accessibility polling to
the instrumentation thread fixed the test. No startup workaround was changed.

## Scottish playoff integration — awaiting device validation

KnockoutTie separates regulation, aggregate, extra-time and shootout scores;
ScotlandPromotion implements the three-stage SPFL ladder with persisted legs.
The manager watches 90 minutes of their own legs using the existing live engine;
extra time and shootouts are explicitly simulated. Completed playoffs can move
clubs between the two linked tiers without reordering any club or player IDs.
Scottish table ordering now uses tied-group head-to-head points/GD after overall
points/GD/GF. Unresolved consequential ties, or an unverified historical split,
do not silently award promotion through the fallback display order. The existing
retain-divisions development option remains available for that unfinished case.

Not implemented here: deciding league ties; lower-pyramid relegation; playoff
calendar dates/recovery; fully visual extra time/shootouts; the other national
promotion formats; domestic cups or actual European admission. Playoff player
statistics for background clubs and full end-of-season honours still need work.

## Scottish postseason verified, 3 October 2026

Runs 37145455786 (e3ed59a) and 37145760482 (5631b77) passed Android 35/36,
including all forty standalone/linked career seasons, watched playoff legs,
reload between legs, unchanged league statistics, stable club/player identity
through promotion, legacy upgrade and AAB-derived device installation.
The second run also verifies live aggregate orientation after venues reverse.
Equal Scottish table positions are marked explicitly and consequential unresolved
places are not awarded. Return-leg match intent now also considers aggregate score.

## Germany integration (awaiting device validation)

Added DFL-specific ordering, with direct-meeting criteria withheld until both
meetings are complete, shared unresolved positions and separate away-goal rules
for league ordering versus knockout ties (no knockout away-goals advantage).
Linked German careers now seed two automatic swaps plus the Bundesliga-16 /
second-tier-third playoff. Watched legs, reloads and the existing stable-identity
season transition share the small PromotionCampaign interface. Existing Scottish
snapshot strings remain unchanged and readable.

Venue selection uses final scheduled league dates: fewer rest days gives the
return leg at home, equal days draw lots and persist the result. The current
weekly simulated calendar gives the tiers equal dates, so this build uses the
draw path. Official dated calendars remain a separate unfinished requirement.
Third-tier relegation, unresolved neutral deciding matches and licensing
adjustments remain inactive. German table labels say which lower-pyramid zones
are inactive. Full UEFA admissions and the other eight countries remain pending.

Visual review of the Scottish Android screenshots exposed identical purple
markers for both clubs. KitColours now resolves clashes with a contrasting away
secondary or fallback colour, calculated once per match, preserving saved club
colours. Home/away marker outlines also differ. Unit tests cover identical kits
and dark/light fallbacks; new device QA asserts separation in the watched leg.

## Germany and match presentation verified, 3 October 2026

Code 1a96ad28a10bcda95897ff6524b024d4ef4d24be passed both Android 35 and 36:
https://github.com/luisfabioj-gif/ProjectMister/actions/runs/37146454621
The jobs verified German watched legs, a saved first-leg score, aggregate-driven
late-game opposition intent, actual promotion, stable identities, unchanged
league results, and reload after the season transition. The Scottish tests,
all forty career seasons, legacy upgrades, navigation, half-time/substitutions,
compact tactics and AAB-derived release installation also passed.

Reviewed API 35 screenshots: german-promotion-review.png and promotion-live-leg.png.
Long automatic-promotion club names fit, the season action is unobstructed,
aggregate scores are readable, and identical club colours now render distinct
purple/white markers. Screenshot test scores are synthetic fixtures used to
exercise the transaction and aggregate display, not real match results.

Verified development APK artifact: 11282931160 (API 35; 1,961,513 bytes).
This is an installable development update using the existing test signing key,
not the finished twenty-division expansion or a production Play release.


## Portuguese League Cup verified, 6 October 2026

Version 2.3.4 adds the playable 2026/27 Portuguese League Cup in new linked
Portuguese careers, with dated cup events, watched manager matches, background
results, direct penalties and a saved trophy/archive. Both Android 35/36 passed
run 37537592141 at source e1b921b2c0b379168ee3663fea1578130dd64378.
See V2_3_4_CHANGES.md for sources, test evidence and installation notes.

This updates earlier blanket statements that no cups are playable. Other
domestic cups, UEFA competitions and authentic league calendars remain
unimplemented. The published 2027/28 Portuguese League Cup format depends on
European qualification and lower-pyramid entrants; it is not replaced by the
obsolete eight-team format. Existing careers receive no retrospective results.
