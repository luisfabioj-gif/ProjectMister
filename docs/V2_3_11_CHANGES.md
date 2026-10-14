# BOSS XI 2.3.11 verified Android test build

This build completes the remaining implementation around the expanded
2.3.10 career checkpoint. Version code 37 preserves the package and existing
development signing identity. Both complete Android 35/36 gates pass on
14 October 2026, and the verified APK is available with the final chat handoff.

The finished test scope retains twenty selectable real-club divisions across ten
countries, linked promotion, worldwide/free-agent recruitment, recurring national
cups and three UEFA competitions, sourced historical winners, live match tactics,
78 fictional photographic portraits and stadium/upgrade artwork. Existing saves
retain their stable identities; a new career in an empty slot starts the expanded
world. Earlier feature and asset changes are recorded in the versioned notes.

- Country/tier transfer windows, ordinary free-agent exceptions and French
  qualification delays are saved and enforced before signings or selection.
- Consequential final league ties resolve through saved deciding matches,
  series or the modeled administrative draw. Reserve and parent-club conflicts
  retain stable club identities and eligible simulated qualification results.
- Actual watched/background participants accrue cup restrictions and
  suspensions. Unused substitutes do not receive invented appearances.
  Opponent substitutions use the frozen eligible bench.
- UEFA A/B lists enforce training places, goalkeeper minima, stage deadlines,
  late qualifying additions, closed league-phase lists and three winter additions.
  Only an eligible registered fixture serves a UEFA ban.
- Future projected UEFA pots that cannot satisfy association capacity use saved,
  explicit seed exchanges. All qualified clubs and original seeds are retained;
  the titleholder stays in pot one and strict opponent/venue rules still apply.
  The competition screen identifies each adjustment as a career simulation.
- French/Scottish cup sheets can promote a licensed fictional youth goalkeeper
  when existing keepers are unavailable. Promotions retain saved identities,
  wages and training records, preserving existing injuries and suspensions.
- Market hires and academy promotions share an ordered roster extension so
  earlier player IDs, portraits and save records remain stable.
- League, national cup, UEFA and postseason calendars retain dated fixtures,
  recovery gaps, elapsed-week accounting and chronological reloads. Published
  first-edition inputs and later projections are distinguished.
- Three slots and validated backups retain the new lists, training histories,
  participation ledgers and calendar fields. Missing/corrupt fields are rejected
  before replacing an existing career.
- The Scottish native test now verifies keeper appearances and exact reloads
  on both result paths before its final completion assertion.

## Handoff evidence

Tested source: `bd5059b96a93c91655d56db2402d0142c6c195bf`.
[Run 38602775811](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811)
passes the full Android 35/36 gate. Both jobs contain byte-identical APKs;
the manifest and preserved signing certificate verify. APK SHA-256:
`005d9e0cb9eb0394bb638dcd67b704f051b65064552b0e7adfaa6f3e268981a5`.

[The release record](BOSS_XI_2_3_11_RELEASE.md) identifies artifacts, reviewed
screens and validation limits. The fixed 100-point tracker now has every milestone
verified, including actual APK delivery: 0% estimated feature scope remains.

Use [ANDROID_TEST_GUIDE.md](ANDROID_TEST_GUIDE.md) for the Samsung Galaxy
S25 Ultra test and issue reports. Physical-phone audio/performance testing and
private Play signing/publication remain outside this development gate.
