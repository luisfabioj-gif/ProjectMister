# UEFA career integration — development checkpoint, 10 October 2026

New expanded careers connect the Champions League, Europa League and Conference
League to the existing match, calendar, save, backup and History screens. The
opening edition uses the published 36-club fields and pots; results and draws are
career simulations. The world registry includes clubs from all 54 currently
admitted associations, including Liechtenstein's cup representative. Russia is
excluded from admissions. Player identities and abilities remain fictional.

Annual admissions use saved local league and national-cup outcomes, simulated
foreign domestic outcomes, three actual career UEFA titleholders and two earned
association performance places. The foreign tables use available real clubs;
they are labeled simulations, rather than complete official national leagues.
Qualifying plays concurrent two-leg champions/league/main paths, with Article 3
loser movements, coefficient seeding, association exclusions, saved venue draws,
extra time and penalties. Exactly 36 different clubs qualify for each phase.
EPS pass-down remembers the clubs already upgraded, so equally ranked qualifying
rounds cannot cycle between the same replacements.

Career association points include qualifying wins/draws, league/knockout points
and the competition-specific bonuses. Each association's denominator includes
all admitted clubs, including eliminated qualifiers. The opening campaign has
no recorded qualifying results and does not invent them; it uses its published
admitted-club denominators. Club coefficients and subsequent access rebalancing
are explicitly career projections, not published future historical coefficients
or access lists. When hypothetical titleholder outcomes produce an extra
qualifying bye, the projected list adjusts an existing Conference qualifying
entrant's round while retaining every admitted club and every guaranteed phase
berth. This simulated adjustment is not a claim about a future UEFA circular.

UEFA dates follow the opening published windows, with deterministic saved
reschedules around local national cups. Subsequent editions project those
windows. Admitted clubs reserve their potential qualifying and phase dates
before results are played, so qualification cannot silently move their league
schedule. League dates preserve at least two days between dated fixtures. The
calendar still needs the remaining postseason-decider integration before its
separate completion milestone can be verified.

Saved qualification inputs, draws, scores, dates and exact completed editions
survive reload and backup. Past league phases and qualifying draws appear in
History. Restore validates club identities, association membership, field
derivation, result chronology and archives before replacing preferences.

The independent Android runner now has a combined UEFA gate for all ten playable
countries: two complete careers per country, managed watched and quick UEFA
results, two-leg qualifiers, domestic-ledger isolation, no managed double
booking, monotonic dates, exact season history, reloads and atomic rejection of
corrupted backups. Existing national-only gates remain focused on their already
verified cup formats. Compilation is not passing emulator evidence. UEFA and
annual-qualification milestones stay pending until the required integrated
Android checks pass and remaining allocation details are finished.

Host checks pass 80 annual admission/qualifying campaigns and 34,260 two-leg
events across all 54 associations. The recurring career check also passes three
complete UEFA editions, 2,441 events including 848 qualifying matches, saved
reschedules, all-admitted association denominators and exact league/qualifying
archives. These checks do not establish Android or physical-device validation.

## Primary references checked 10 October 2026

- Opening qualified fields and pots: the competition-specific source links in
  `app/src/main/assets/competitions/europe-2026.json`.
- UEFA Article 3, titleholders, domestic qualification and performance places:
  https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-3-Entries-for-the-competition-Online
  https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-3-Entries-for-the-competition-Online
- Annex D, qualifying/phase points, bonuses and admitted-club denominators:
  https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Annex-D-Coefficient-Ranking-System-Online
- UEFA access list, including the opening admitted-club counts:
  https://www.uefa.com/nationalassociations/uefarankings/accesslist/allcompetitions/
- Real supplementary club identities, by association:
  https://editorial.uefa.com/resources/02a2-2004c8d438c3-b03724f6d21e-1000/uefa_european_football_directory_2025-26_march_edition.pdf.pdf

The existing draw, ranking and knockout rule references remain in
`UEFA_DRAW_INTEGRATION.md` and `RECURRING_COMPETITIONS_ENGINE.md`.
