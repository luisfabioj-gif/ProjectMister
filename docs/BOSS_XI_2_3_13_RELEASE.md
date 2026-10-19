# BOSS XI 2.3.13 — olive and lime UI

The premium UI update is verified on Android 15 and 16. The signed
`BOSS-XI-2.3.13.apk` is saved for the chat download and ready for physical testing.

## Presentation

The October 19 references guide the deep olive atmosphere, charcoal cards,
electric lime action colour, larger rounded surfaces, consistent native line
icons and stronger type hierarchy. The original BOSS XI identity remains.

- All management pages use the shared header, typography and spacing system.
- Five persistent destinations open Home, Squad, Tactics, Inbox and the Club hub.
- The home fixture and full-time result use balanced crest and score heroes.
- The career hub has stadium photography and distinct saved-career cards.
- Squad cards show photographic fictional portraits and working role filters.
- The portrait tactics pitch shows the actual selected XI, including auto-picked
  careers without explicit slots. Each marker opens its player's assignment.
- Player, staff and manager profiles use olive hero cards and aligned metrics.
- Finances has a prominent balance, budget cards and responsive account rows.
- Scouting, competition tables, history, stadium and live-match controls share
  the same surfaces. Scroll-safe sheets use the same palette and touch controls.
- Authentic club kit colours, player identities and existing saved careers keep
  their established data. No additional runtime dependency or network is needed.

## Build and verification

| Field | Value |
| --- | --- |
| Version / code | 2.3.13 / 39 |
| Package | `com.projectmister.game` |
| Minimum / target SDK | 26 / 36 |
| Candidate source | `efaed35a48ae0bdc2455bc1b588aa945f009170b` |
| Premium Android workflow (both jobs passed) | [39256483536](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536) |
| API 35 / Android 15 | [118072960105](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/job/118072960105) — success |
| API 36 / Android 16 | [118072959564](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/job/118072959564) — success |

| Full game workflow | [39256483506](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506) — both jobs passed |
| Full API 35 job | [118072959549](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/job/118072959549) — success |
| Full API 36 job | [118072959751](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/job/118072959751) — success |
| APK bytes / SHA-256 | 4,348,576 / `93c141c01e23d14ff97af7fe8983785576f316a3a2a8d19869e8b0c2a96441f9` |
| Signing certificate SHA-256 | `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e` |

The full host core suite, native app and independent instrumentation builds pass.
Both full native suites run freshly on this source: 31 top-level mode reports
per platform, with 10,272 passing assertions on API 35 and 10,270 on API 36.
The dedicated UI suite adds 300 presentation assertions per platform, plus its
four-assertion baseline, 357-assertion feedback and 396-assertion compact modes.
All required terminal markers pass and no mode report contains a failure.

The full gate includes forty standalone/linked careers, all ten combined
European careers, recurring national cups, registration and eligibility rules,
watched matches, exact backups, baseline upgrades, compact layouts,
non-debuggable bundle-derived release installation and different-key recovery.
The UI gate additionally checks actual navigation, position filters, all eleven
selected-XI identities and assignment actions, management pages, audio sheets,
live presentation and all 64 independent stadium/facility upgrades.

Final native screenshots of the dashboard, roster, profiles, tactics, accounts,
club hub, training, transfers, scouting/report, inbox, history, tables, complex,
live match/statistics and compact formations were visually reviewed. The final
full and focused logcats contain no game fatal exception or app ANR. An observed
API 35 emulator launcher/system ANR was captured and dismissed by the existing
bounded environment handler; no application failure was accepted.

## Verified artifacts

| Artifact | ID | ZIP SHA-256 |
| --- | ---: | --- |
| [Premium APK / API 35](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/artifacts/12188570378) | 12188570378 | `38868437d52e15bcabc47e213a149dd0eaa0bdf7da1ebc19f734f312e5aa8d92` |
| [Premium APK / API 36](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/artifacts/12188705289) | 12188705289 | `efce932a671737b77797bd62eebd9b733648a62f5a559013bee3181d9f0ee7bc` |
| [Premium QA / API 35](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/artifacts/12187174517) | 12187174517 | `0948b7cb4b948725b04dd5e353d06a6baff2180e0c16de6baca8acb278ddfaca` |
| [Premium QA / API 36](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483536/artifacts/12188315643) | 12188315643 | `f4cceb44cdd3908968a4c12e9ad5ba36b50c83d9e6799762dad1aea17ef43cf7` |
| [Full QA / API 35](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/artifacts/12196171546) | 12196171546 | `0d591ee635270dd357bb95434412bc8d2ff72ed56b6302f5edd25342c0eb45ed` |
| [Full QA / API 36](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/artifacts/12193752162) | 12193752162 | `a12d6afb3bc4cfb6949c81c0bd361e317a408c9c9035c21057892751f3235683` |
| [Full candidate / API 35](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/artifacts/12196256500) | 12196256500 | `38868437d52e15bcabc47e213a149dd0eaa0bdf7da1ebc19f734f312e5aa8d92` |
| [Full candidate / API 36](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/39256483506/artifacts/12192872980) | 12192872980 | `d8db70053c009bff9f68f6c60f37d98e94f699643e85e3a7d4607ce1c8ca56d9` |

Downloaded archives pass their published digests and ZIP CRC checks. The full
API 35 candidate archive is identical to the downloaded premium API 35 archive.
Both premium APKs and the downloaded full API 36 candidate contain byte-identical
APK files. The full job APK identity records report that same SHA-256 on both
platforms. The APK v2 signature, package, version, SDK fields, premium UI code
and existing update certificate verify. Documentation-only updates after these
jobs leave the tested application bytes unchanged.

This is an Android sideload development build using the existing update key.
The emulator checks do not claim physical Samsung testing or Play publication.

Export existing careers before updating. A new career is optional for this
presentation update; it is still required to adopt the 2.3.12 revised starting
club data if the current career predates that release.
