# BOSS XI v2.3.1 — offers, menus and matchday

## Reported defects and changes

- Transfer offers and contract packages disappeared because Android's AlertDialog message panel takes precedence over its item list. All 40 dialog call sites now use a shared dark branded dialog with independent message, choice and action regions. Long menus scroll while confirmation/cancel controls stay outside the scroll. Multiple-choice sound controls and text input remain supported.
- The BOSS XI logo appears in normal page headers, every shared dialog, and the live-match sidebar. One small sampled bitmap is reused.
- Speeds are exclusively Slow / Medium / Fast, at 0.45 / 0.70 / 1.05 simulation seconds per real second. Medium is the default. Uninterrupted regulation play is approximately 200 / 129 / 86 seconds, excluding half-time and management pauses.
- Synthetic crowd hiss is replaced by CC0 spectator field recordings, including a separate goal reaction. See AUDIO_SOURCES.md. Two long, asynchronously prepared streams avoid short-loop fatigue and SoundPool truncation. Short effects use reduced gain, reaction cooldowns and pressure-sensitive mixing. Saves/misses are timed to contact rather than shot release.
- Original arrival steering bounds speed and acceleration, accounts for the pitch aspect ratio, and slows players toward their destinations. The extra dribble position update that bypassed movement limits is removed. Receiving runs use reachable lead distances; possession tracking eases the ball into control. Support players form passing angles; defensive lines recognise three/four/five defenders from formation anchors.

## Research and boundaries

Movement concepts were researched using Craig Reynolds' published [steering behaviour work](https://www.red3d.com/cwr/papers/1999/gdc99steer.html). MatchMotion is an original Java implementation. No proprietary Championship Manager source was sought, copied or decompiled. The target is readable management-match pacing, not a claim of engine equivalence.

Package, signing identity, existing saves and startup navigation safeguards remain unchanged. Version code is 26. Source remains on work/boss-xi-v2.3; main remains the stable baseline.

## Validation scope

Core checks cover substitution transactions, monotonic ball flight, pace/fitness, speed order, bounded acceleration/speed and arrival convergence. Emulator QA also taps the actual offer choices and contract package, verifies a signing and one budget deduction, reloads the signing, checks named speed controls, crowd stream preparation and mute, and runs the existing upgrade/navigation/half-time/full-match/compact formation checks. Final run and APK evidence will be added after completion.

Hardware listening and sustained device performance remain user-device validation. Waveform checks and successful media preparation cannot prove perceived sound quality. Complex match intelligence, long-career validation and unique portraits for every person remain future work.
