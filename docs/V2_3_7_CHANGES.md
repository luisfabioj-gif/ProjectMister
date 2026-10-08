# BOSS XI 2.3.7 — playable recurring DFB-Pokal

New linked German careers now contain a playable 64-club DFB-Pokal. The 28
additional clubs are appended to the career-owned database and receive squads;
existing club/player indices are retained. Existing 36-club German saves remain
loadable with their original world and recorded results.

## Gameplay

- Cup fixtures appear on the dashboard and Cup calendar alongside league games.
  Managed ties use the same watched-match and quick-result flows as Portuguese
  cups. Background ties resolve through the career simulation.
- Six knockout rounds, opening pots, amateur home rights, extra time and
  penalties, and a neutral final in Berlin are implemented.
- Simulated league dates avoid cup dates by at least three days. Cup results do
  not advance league rounds or alter league standings. Postseason recovery and
  wages advance through the May final; annual ageing waits for all competitions.
- Completed editions are archived at rollover. Each new year receives a fresh
  draw. Current results and archived finals are accessible through History.
- Active and historical editions are included in saved careers and typed backup
  files. Validation rejects missing cups, changed rules/calendar, invalid club
  identities, future results and corrupt archives before replacing preferences.

## Simulation boundaries

The opening 2026/27 field uses verified identities. Its draw and individual dates
are simulated, including the Supercup clubs' opening fixtures. Later seasons
retain the 28 lower cup clubs; the four lowest retained second-division clubs
enter the lower opening pot. Regional cups and Liga 3 qualification are not yet
played. This limitation is stated on the cup screen. Finances use the existing
career simulation, without an official DFB prize-money schedule.

This APK does not activate UEFA competitions, the 2027/28 Portuguese League Cup,
or the other countries' cups. Their remaining integration work is separate.

## Validation

Core regressions and both local Android builds passed. The device scenario plays
two complete German league/cup seasons, watches twelve managed cup ties,
reloads a pending shootout, checks all 63 cup ties per year, monotonic dates,
exact annual ageing, postseason processing, archive preservation and rejection
of a corrupted backup. Android 15/16 CI results are recorded after completion.

## Primary data and rules

- https://www.dfb.de/news/erste-pokalrunde-osnabrueck-gegen-bayern-hebc-hamburg-gegen-bvb
- https://www.dfb.de/news/das-sind-die-zeitgenauen-ansetzungen-der-ersten-hauptrunde
- https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/modus
- https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/rahmentermine
