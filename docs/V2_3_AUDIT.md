# BOSS XI v2.3 — source audit and implementation plan

Baseline: successful run 36338934062, commit c52c5c905f279059c0d1cd0e53f9665950060e5f. Exact reconstructed source preserved in commit 45218f9 and tag baseline/v2.2.1-hotfix4-source. Original source regeneration and all Hotfix 4 patch checks passed locally. Baseline CI confirms compilation, APK assembly and startup; this is not evidence of full gameplay quality.

## Findings
- Architecture: 7,888-line MainActivity reconstructed from historical workflow eb11125, then modified by embedded Python string patches. Only three workflow files existed in the repository. Extremely fragile edit surface; no source-level regression tests.
- Startup: five immersive-navigation calls deliberately redirect to a no-op. Preserve them and the package, preference file, development certificate and existing field encodings.
- Shared UI: repeated 156dp logo, centred large heading, gradient buttons, long dashboard button list. Landscape stats occupy 134dp before eight controls compete for remaining height. No safe-inset helper.
- Portraits: RealisticHumanPortraitView draws facial shapes on Canvas; it is not photography. Player seed contains team and age, so identity changes after transfers and birthdays. No real asset cache.
- Match: fixed 120Hz steps, formations, defensive lines, runner tracking, set pieces and celebrations already exist. Build on these. Movement caps do not depend on pace or fitness. Flight destination follows the receiver, creating homing passes. Pass target selection is largely random. Goals are announced before flight completes.
- AI: opposition XI selected at kickoff, no management substitutions in processLiveMinute. User fitness decreases during match but opposition does not. Team quality includes opposition reserves.
- Substitutions: reversal checks whether an incoming player has ever been removed, rather than whether play resumed. A second dialog contains duplicate substitution code. Need one transactional ledger, exact pending-pair reversal, commit on resume, and tests for chained changes and exhausted allowance.
- Audio: one MediaPlayer loop at 0.24 volume plus four SoundPool effects. No load-ready tracking, dynamic layers, mute settings, focus handling or Activity cleanup. Synthesis is not a recording of a crowd; new original audio requires listening QA, not just waveform checks.
- Statistics: post-match random goals/assists run over all players, including the watched teams. Appearances are randomly assigned. Correct watched-match stats before adding more statistics.
- Profiles: player and staff profile sections and numerical attribute bars already exist; preserve contracts, scouting actions and current role information. Staff are three persistent named roles, not a full labour market.
- Finance/stadium: detailed season/month accounting and facility upgrades exist. Preserve calculations and save defaults. Current finances use weekly progression and approximate projections.
- Training/scouting: individual schedules, masked knowledge and progressive scouting exist. Preserve; improve presentation first.
- Board/news: confidence, funding requests, reports and persistent inbox exist. Preserve. No need for another overlapping system.
- Competitions/history: league fixture selection and simulated cup outcomes need a separate integrity review; cup calendars must not be described as a complete playable competition system. No durable multi-season career archive.
- Testing: current sanity checks are grep presence checks. Startup-only emulator run does not exercise screens, save compatibility, half-time or match completion.

## Delivery sequence / exact scope
1. Preserve exact generated baseline and migrate build to committed Java/resources with a dedicated branch workflow. Keep old workflows as reference, do not overwrite main.
2. Shared design tokens and ripple controls; compact headers; persistent primary navigation; dashboard fixture, result, league rank, finances, condition, board and alerts. Keep all existing destinations.
3. Original fictional photographic portrait atlas, bounded bitmap cache, stable persisted assignment independent of age/team, neutral fallback. Preserve all profile actions. Asset reuse across 360 players is deliberate; larger unique portrait library is later content work.
4. Testable substitution ledger and match maths; lifecycle pause/cleanup; freeze/recover pitch state across tactics; correct watched-team statistics.
5. Pace/condition acceleration, separation, tactical intent and score-aware shapes; weighted spatial passing; distinct flight arcs; opposition substitutions using the same player data and five-change limit. Preserve existing action chains and set pieces.
6. Event-driven audio manager with loaded-sample checks, independent original crowd layers, transient effects, ducking, event decay, mute and lifecycle/focus handling. No copied game sounds. Audio must be described as synthesized until listening review confirms quality.
7. Landscape controls of at least 48dp in scrollable container, scoreboard and commentary refinements, useful half-time summary. Finance/stadium inherit visual tokens without accounting rewrite.
8. Build and run unit regression checks, install baseline and upgrade to candidate on emulator, check save survives, navigate required screens, advance through tactics/half-time/full-time, collect screenshots and logs. Fix actual failures. Deliver candidate only with explicit test evidence and limitations.

## Recommended additions / postponements
High value now: assistant condition advice, truthful match statistics, opposition substitutions, persistent sound controls, stable portraits and substitution undo. Existing weather, referee strictness and runner tracking should be improved, not reintroduced as new features.
Next: captain and set-piece assignments; persisted appearances/minutes/assists and match ratings; full fixture integrity; saved career history; seasonal awards. These require coherent event and season models.
Postpone: promises, agent negotiations, instalments, complex loans, media controversies, full youth intake and additional staff job types. Adding them now increases save/accounting risk and distracts from matchday.

## Risks and release gates
Highest risk: startup insets, old saves, signing continuity, partial match mutations on abandonment, substitution eligibility, generated audio realism, portrait repetition, short landscape screens, event timing. No unsafe immersive call is to be enabled. No precision statistics unless actually calculated. No claim of full audit from screenshots until captured. No release claim until build and feature QA complete.

## Candidate checkpoint

See VALIDATION.md for the current evidence and remaining release gates. The implementation is a v2.3 candidate, not an assertion that the entire 50-section brief has passed. Portrait library now includes 42 faces, including female managers. Runtime and app-screenshot verification remain required.
