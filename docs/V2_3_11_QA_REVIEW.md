# BOSS XI 2.3.11 visual and artifact review — 14 October 2026

This records reviewed evidence. It does not replace the complete Android 35/36
gate or final APK delivery.

## Passing Android 36 checkpoint

- Source: `7b7bd0ede2246bfa66d6f8e1ce9e6cb02803453a`.
- [Run 38562072883](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38562072883),
  Android 36 job `115845354362`: success. Android 35 in that run reached natural
  half-time but was stopped by the overall full-mode time limit.
- QA artifact `11899166314`; ZIP SHA-256:
  `ebdef704a8b6020bc41336e11f67523936e4ccdfbac226e0e312f70c1fe70cab`.
- Candidate APK artifact `11899161453`; extracted APK is 2,637,610 bytes.
- APK SHA-256:
  `005d9e0cb9eb0394bb638dcd67b704f051b65064552b0e7adfaa6f3e268981a5`.
- Manifest: `com.projectmister.game`, version 2.3.11 / code 37, minimum SDK 26,
  target SDK 36. Signature verifies with the existing development certificate:
  `06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.
- The APK contains no native libraries, 78 portrait assets and stadium/upgrade
  artwork. A single APK is suitable for the supported phone architectures.

The complete API36 gate includes ten two-season UEFA careers, recurring national
cups, forty standalone/linked careers, release installation and different-key
restore. Per-mode reports contain 179 rules assertions, 31 UEFA-player-list
assertions, 130 cup-player assertions, 751 division/backup assertions and 94
release-navigation assertions, all passing. These are assertions, not extra
milestone weights.

## Screens visually reviewed

The API36 archive contains 150 preserved screenshots in
`before-key-migration/qa`. These individual screens were reviewed:

- `01-dashboard.png`: real club names, next fixture, club pulse and controls.
- `03-player-profile.png`: portrait, contract, birth date, training and discipline.
- `09-stadium.png`: stadium illustration, ground information and upgrade artwork.
- `23-compact-match.png`: scoreboard, pitch, statistics and touch controls.
- `32-history-hub.png`: sourced records distinguished from simulated career data.
- `31-worldwide-market.png`: free agents and the twenty-division transfer pool.
- `decisions-FR.png`: saved final-decision navigation and projection explanation.
- `europe-ENG-adjusted-draw.png`: all four original/adjusted seed records are
  readable, with an explicit simulation explanation and competition standings.
- `22-natural-fulltime.png`: completed natural match, statistics and league results.

The API35 archive `11894515369` preserves the same readable England seeding
adjustment and `21-natural-halftime.png`, with role-coloured tactics markers,
conditions, substitutions and Return to Match. Its ZIP SHA-256 is
`7179977117c6147c918cee66613dc87c0c8d5fb5c7545698e6cd517a3bb1e0b2`.
No game FATAL EXCEPTION, app ANR or BOSSXI_QA failure was found in that logcat.
This does not establish an API35 full-time or complete-gate pass.

## Current rerun

Source `bd5059b96a93c91655d56db2402d0142c6c195bf` retains byte-identical
production and native-test source. It changes only the full-mode execution
budget (480 to 600 seconds), overall workflow budget (90 to 120 minutes) and
status documentation. Every assertion and the separate 75-second match progress
bounds remain intact. [Run 38602775811](https://github.com/luisfabioj-gif/ProjectMister/actions/runs/38602775811)
must finish before its complete-gate evidence is credited.

Physical Samsung Galaxy S25 Ultra testing and audio/performance feedback remain
the user's next step after the verified APK handoff.
