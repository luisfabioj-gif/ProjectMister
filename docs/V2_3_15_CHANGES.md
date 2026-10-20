# BOSS XI 2.3.15 — identity, club presentation and match contact

Based on the delivered 2.3.14 source `bbfd286fe350302146dc9aa278ee31bf688ace40`.
Version code 41 retains the package and existing update signing certificate.

- Club display names use recognisable alternate identities throughout shared
  screens, menus, old news, profiles and recruitment search. More than 1,500
  catalogue identities are covered. Stable club IDs, saved arrays, fixtures,
  results and cup membership retain their original identity and order.
- Every country's two playable divisions use Country 1st Division and
  Country 2nd Division, including labels read from older careers.
- New and saved media messages omit the newspaper/journalist meta labels and
  interview disclaimer. Source, dates, replies and read status remain intact.
- Club Pulse uses a prominent league-position card and four graphical stat
  cards, condition/confidence bars, form chips and the last result. The league
  card opens the table; balance, condition, board and manager cards open their
  relevant screens.
- League tables use a dense striped grid, club crests, goals for/against, points
  and ten-match form strips. Swipe horizontally for all columns. Club rows open
  their squads; division, results and rules actions follow the table. Form uses
  recorded results only, leaving unavailable results blank.
- Each stand has a saved name, banner, eight seat colours and four patterns,
  with a live seat-plan preview. Designs survive stand upgrades and backups.
  Supporter open days cost EUR 10k, give squad morale +1 and board confidence +1,
  and are limited to one event per stand per calendar month.
- Shared main-page headers centre the feathered BOSS XI logo against the
  background, with balanced back and inbox controls.
- Loose balls remain at their physical location while both teams chase them;
  possession transfers on contact. Dead-ball takers retrieve the ball and reach
  the restart location before kicking/throwing. Offside restarts follow the same
  path. Ball launches preserve the current ball position.
- Shot selection and resolution use shooting distance/angle, defender screens,
  and the goalkeeper's actual position and reach. An unreachable keeper cannot
  save a shot. Blocks and parries have a contact point followed by a visible
  rebound; keepers defend the goal-angle bisector instead of drifting out toward
  an approaching striker. Goal celebrations and own-half reset checks remain.

## Verification

Tested production source: `e236743c5f0afb73be9b371a22c987116df9bd0b`.
The final statistics-header correction covers both live and expanded match
statistics, with native assertions for each presentation.

Workflow [39410999293](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39410999293):

- Android 16 / API 36 job `118588446584`: passed.
- Android 15 / API 35 job `118588446344`: all eight refinements, nine prior
  feedback fixes and club-presentation checks passed. The full scenario reached
  minute 87.1 before its original two-minute full-time wait expired; no fatal
  application exception was recorded. The remaining match and compact checks
  are covered by the focused follow-up below.
- Both jobs passed the complete core suite, including 53 focused identity,
  stand, news and shot-geometry checks and 50 retained feedback checks.
- Each emulator installs 2.3.14, seeds and saves a career, and installs this
  update over it. Native modes cover the eight new requests, all nine prior
  feedback requests, the main club screens, full match/season/save regression
  cases, and all five formations in compact landscape at 720 x 1280.
- Both APIs passed 368 refinement assertions, 184 retained feedback assertions
  and 311 club presentation assertions. API 36 also passed the full regression
  scenario and 451 compact-formation assertions without a failed check.
- Native physics checks cover physical loose-ball recovery, retrieval before
  a throw-in, keeper participation in box recoveries, a beaten keeper's zero
  save reach, and an accurate open-goal shot entering the goal sequence.
  Retained checks verify ball-at-feet dribbling and both teams reaching their
  positions after a goal before the conceding side restarts.
- Stand controls, saved palettes/patterns/text, upgrade retention, monthly
  event costs and cooldowns, dated press cleanup, backups and career reloads
  were verified. Main UI and live statistics screenshots were visually reviewed.

Focused Android 15 follow-up:
[39413253283](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39413253283),
job `118595972468`, test-only source `6e43c6ee5016bb5a44525dcba68fff98bed9eb9c`.
Production source is unchanged from `e236743c5f0afb73be9b371a22c987116df9bd0b`.
The natural-match test allows four minutes per half and independently fails
after 45 seconds without clock progress, then checks event totals, full-time
save/reload and all five compact formations. Follow-up result: passed, with
95 natural-match assertions and 451 compact-formation assertions. Recorded
clock samples progress through both halves to full-time, with no fatal
application exception.

API 36 tested APK artifact: `12259891407`; QA artifact: `12260051361`.
API 35 focused tested APK artifact: `12260618659`; QA artifact: `12260543749`.
Both tested APKs are byte-identical. All downloaded artifact ZIP digests were
verified against GitHub's SHA-256 values. The downloadable APK is copied from
the tested workflow artifact and has the same verified hash.

- Package: `com.projectmister.game`, version `2.3.15`, code `41`.
- APK SHA-256: `2e2f9ffb806e8321ccfcfc1566ce2a320b4ce56082a9ebeeb4a13ad4022cd6b4`.
- Update certificate SHA-256: `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.

Verification uses Android emulators; physical-handset play feel remains subject
to device testing and player feedback.
