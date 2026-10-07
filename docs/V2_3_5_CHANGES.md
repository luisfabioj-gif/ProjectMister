# BOSS XI 2.3.5 development candidate

## Playable changes

New linked Portuguese careers include the 146-club 2026/27 Taça de Portugal:
47 opening ties, 19 byes, staged second- and first-division entry, and five
European entrants in round four. The saved draw follows career results through
the neutral single-match semi-finals and final. Drawn matches use extra time
and penalties. The calendar displays drawn rounds, results and the winner.

The Portuguese world now contains 149 clubs (including three reserve sides
excluded from the cup) and 2,980 fictional players. The additional 113 lower-tier
clubs participate in the cup and player database; their leagues are not playable.
Existing careers retain their original club/player indices and competitions.

League and both Portuguese cup events advance in chronological order. League
dates are simulated around cup windows, with at least three calendar days from
cup dates. Post-league weeks apply wages, recovery, training and stadium project
progress before the season is finalised. Saves preserve that processing counter.

Quick result uses the selected starting players' ability and fitness, plus home
advantage. It records player goals, appearances, fitness changes, league results
and the normal career progression. Cup shootouts remain separate from goals.

Player age, current ability, potential and transfer value now persist explicitly.
Previously, reloading regenerated these fields and could lose season ageing or
training progress. Old saves retain their generated defaults where no saved
value exists; lost historical development cannot be reconstructed.

## Data and simulation boundaries

The opening cup draw and entrants are sourced; subsequent draws and all results
are simulated. Round-of-16 and quarter-final dates select one day within the
published competition windows. League fixture dates are not the official draw.
Club finances, player identities and ratings remain fictional simulation data.

Sources used for the 2026/27 field, staged entries, opening draw and calendar:

- https://afatv.pt/news-details/0c60aabb-210a-46df-9aa9-f57fec63d903
- https://historiatacaportugal.wordpress.com/category/2026-27/
- https://www.ligaportugal.pt/noticias/28742/sorteio-da-3.a-eliminatoria-da-taca-de-portugal
- https://www.ligaportugal.pt/backoffice/assets/Comunicado_Oficial_n_357_Calendarios_das_Competicoes_Profissionais_2026_27_3b82c06381.pdf
- https://cdmafrasad.pt/cd-mafra-segue-em-frente-na-taca-de-portugal/

## Validation status

Local core regression checks include 64 full domestic cup tournaments (9,280
ties), saved extra-time/shootout states, staged entries, chronological dates,
corrupt-save rejection, backup round trips and league/cup recovery gaps.
Android integration coverage adds a complete 34-round quick-result career,
managed runs through both cup finals, periodic reloads/backups, postseason
processing and next-season archive preservation. Android 15/API 35 and Android 16/API 36 passed run 37607971403 on
7 October 2026, source 818703bf28f911e74652e11954693b803d97fa13. The run also
passed older-save upgrades, all 40 standalone/linked career configurations,
all ten countries' playoff flows, compact layouts, release-bundle installation
and different-signing-key backup migration. The domestic cup winner screen was
visually reviewed. The first candidate exposed the ageing persistence issue;
the corrected candidate passed the unchanged season-age assertion.

Delivered development APK: version code 30, version name 2.3.5-dev.
SHA-256: `8f7369ae303f36b28f7d3f3dff6369cb6dbf10821783fe9299bcabbb0b2c2a66`.
Development signing certificate SHA-256:
`06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.

For a phone playtest, install the APK as an update and create a fresh Portuguese
career to receive the expanded world and Taça. Existing careers remain loadable
with their original competition set. This has been emulator-tested; physical
Samsung S25 Ultra testing remains with the user.

## Remaining completion work

This is a development candidate, not a complete football simulation or a
production release. Portuguese cups currently run in 2026/27 only; completed
results remain archived in subsequent seasons. Recurring cup formats, UEFA
competitions, other countries' domestic cups, authentic league scheduling,
national registration exceptions and lower-pyramid reserve replacement cases
remain open. A physical-phone playtest and production signing are still needed.
