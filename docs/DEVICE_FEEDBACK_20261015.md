# BOSS XI 2.3.12 — device feedback

Status: implementation under verification; no updated APK delivered yet.

This release addresses the eleven observations from the Samsung Galaxy S25 Ultra test. The previous 2.3.11 completion evidence remains evidence for that release, rather than a claim that these changes have passed.

| Feedback | Implementation | Required release evidence |
| --- | --- | --- |
| Classic career option | Twenty real division choices; old careers still load | Native chooser and upgrade checks |
| Club colours | Dated public club colours; contrasting away kit when needed | Full profile validation and representative native kit checks |
| Club finances, strength, Europe | Observed squad values calibrate explicitly estimated board allocations, player values, salaries and strength; published opening UEFA fields | Data checks, annual European campaigns and saved careers |
| Ordinal positions | Shared display formatter for 1st, 2nd, 3rd, 11th, etc. | Native dashboard and tables |
| Match and other statistics | Labelled comparison columns and bars; missing quick-match detail shown as unavailable; structured finance rows | Native statistics screens and visual review |
| Equal league positions | Sequential display positions while retaining the sporting tie-break and decider rules | Equal-points native table and sporting regression tests |
| Stadium upgrades | Five aligned architectural stages, sixteen independently rendered plots, touch selection and zoom; completed work changes the saved component | All 64 component upgrades, cash/capacity checks, reloads and screenshots |
| Player nationalities | Primary citizenship counts apportioned to each twenty-player fictional squad by largest remainder; country-specific names | All-club apportionment checks and exact identity saves |
| Scouting | Explicit action regardless of masking, seven-day assignment, dated assessment, Inbox return, report history | Calendar progression, persistence and corrupt backup rejection |
| History | Season and winner rows; source controls removed | Native navigation and visual review |
| Presentation | Smaller brand header, consistent cards, comparison bars, result cards, responsive accounts and recruitment controls | API 35/36 screenshots and compact layout checks |

## Data boundaries

Private transfer budgets and a single official club-strength rating are not public facts. This release labels both as game estimates. Board allocations are calibrated from observed squad market values and rounded to the existing million-euro economy; the minimum playable allocation is EUR1 million. Published squad market values are themselves public valuations, rather than audited finances.

The game retains fictional players. Nationality shares use observed primary citizenship, then the closest achievable allocation to twenty players; this does not assert that a fictional squad is the real roster or that twenty places can reproduce every fractional percentage exactly. Names and nationalities are saved by immutable player identity so transfers and future data refreshes do not rename a career's players.

The initial European participants come from the 2026/27 competition snapshot. Later seasons qualify from career results. Displaying a unique league-table position does not discard the country's sporting tie-break rules or invent a final qualification outcome where a deciding match is required.

## References and verification

- Club observations: 347 Transfermarkt season rosters, 11 current 2026 ESPN rosters and 7 referenced current-squad articles. All 365 retain their league valuation source, nationality counts and ground observations. Club palettes use observed swatches or reviewed kit/club-colour references; game display hex values are curated. Wikipedia observations retain revision URLs, and official club/league references cover the remaining kit exceptions.
- Opening UEFA fields: https://www.uefa.com/uefachampionsleague/clubs/ ; https://www.uefa.com/uefaeuropaleague/clubs/ ; https://www.uefa.com/uefaconferenceleague/clubs/ .
- Interface research: https://www.footballmanager.com/fm26/features/fm26s-reimagined-user-interface — clear information hierarchy, direct actions and readable grouped data; BOSS XI keeps its own visual identity.
- Host gates: `tools/test-core.sh`, including player identity, report serialization and corrupt-input checks.
- Android gates: `tools/qa-device.sh`, including the new feedback scenario alongside the existing upgrade, match, national-rule, domestic-cup, UEFA, save/restore and compact-screen regressions.

The release evidence and APK identity will be appended after the exact committed source passes the device gates.
