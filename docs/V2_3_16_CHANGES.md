# BOSS XI 2.3.16 — stadium pitch and match presentation

Based on delivered 2.3.15 source d2fd8bb04eaf8df94cdcd21566c841599a23c6c4.
Package and update signing key are retained; version code advances to 42.

- The live match uses an original stadium perspective inspired by the supplied
  reference: roof and seating, grass apron, alternating mowing stripes, corner
  flags, advertising boards and raised goals with nets. Markings use a 105 x 68 m
  field, 16.5 m penalty areas, 5.5 m goal areas, 9.15 m centre circle and correct
  penalty arcs. Players, ball, ball shadow and offside line use one ground-plane
  projection, aligned with the match's actual goal and touchline coordinates.
  The live camera is calibrated to the reference's foreshortened field and
  foreground apron; the higher tactics camera keeps all player cards readable.
- Half-time and live tactics share the stadium renderer with a higher camera
  angle for lineup readability. Paired instruction buttons, a fixed primary
  resume button, compact player cards with ability and condition, and a complete
  seven-player bench replace the cut-off horizontal substitute strip.
- League tables use five-match form, combined goals-for/against and narrower
  numeric columns. Club crests and two-line names remain visible alongside the
  statistics. The heading remains fixed outside the horizontal scroller.
- Post-match results show the manager's current division. Background fixtures
  from the other division are still simulated and saved so standings and the
  season continue correctly.
- Club names now use closer edited spellings, including SL Benfika, FC Portu,
  Arsenel and Manchester Unitas. All 365 playable identities remain distinct;
  older display aliases migrate in presentation without changing career IDs,
  fixtures, standings, personal names or saved custom text. These are alternative
  names, without a licensing-clearance claim.
- Goal commentary alternates the scoring team's colour and a light background
  for 3.6 seconds, retaining the goal message while later commentary arrives.
  The effect uses wall time, stops when leaving the match, and returns to the
  current commentary after its deadline.

## Validation

Verified source: `4dc582f7f5b0075f6cc49568468e203de53ed987`.
GitHub Actions run `40870543262` passed on Android 15 / API 35 and Android 16 /
API 36. Both jobs built and installed the delivered 2.3.15 baseline, seeded a
career, and upgraded it in place to this version using the existing signing key.

- Core checks passed, including 3,109 identity, stand, news and shot-geometry
  checks and 1,982 pitch projection and goal-flash lifecycle checks. Backup,
  domestic cup, recruitment, inbox and match restart regressions also passed.
- Each API passed 492 new presentation assertions, 368 retained refinement
  assertions, 184 earlier requested-fix assertions, 311 screen/navigation
  assertions, 95 natural-match assertions and 451 compact-layout assertions.
  The natural-match test played both halves without clock jumps and reloaded
  the saved result. Compact checks exercised all five formations at 720 x 1280,
  density 320, in landscape.
- The current-division result filter was exercised for all 10 countries and
  both tiers. Other-division fixtures still simulated and were saved.
- Native screenshots were inspected for the live stadium, half-time controls,
  substitutes, five-match table, goal banner, both result tiers and compact
  tactics. The photo asset decoded on the device; field, player and ball
  coordinates shared the tested projection. Goal-banner tests verified both
  colour phases, expiry and callback cleanup when leaving the match.
- Reports and screenshots: artifacts `12896940146` (API 35) and `12895534054`
  (API 36). Tested APK artifacts: `12895798760` and `12895434091` respectively.
  Both APKs were byte-identical, 7,555,087 bytes, and their downloaded artifact
  archives matched GitHub's SHA-256 digests before extraction.

Delivered APK SHA-256:
`8f4dea12794f7526f6941533108bc0e365f34ff8f12a0eb0e051abaca0e154f3`.
Signing certificate SHA-256:
`06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e`.
Manifest verified as `com.projectmister.game`, version `2.3.16`, version code
`42`, minimum API 26, target API 36. Validation used emulators; no physical
phone verification is claimed. This validation record is a documentation-only
addition after the tested source above.

## Stadium asset

Asset: `app/src/main/assets/match/stadium-daylight-v16.png`.
Created with the built-in image generation tool using the supplied tracker
stadium as a camera and style reference. The bitmap supplies the stadium
surroundings and grass texture; code supplies the exact pitch geometry.

Final generation prompt:

> Use case: photorealistic-natural. Asset type: panoramic stadium backdrop for the BOSS XI mobile football game. Input image 1 is a STYLE AND CAMERA REFERENCE ONLY: use ONLY the empty stadium inside the Live tracker card; ignore all the surrounding phone app interface, logos, players and text. Create a clean, original, photorealistic empty football stadium view that closely matches that reference's roof structure, black and grey seating bowl, daylight, elevated sideline camera, gently receding far stand and rich green turf. Landscape composition, very wide panoramic view. Upper 30 percent: a large detailed metal roof truss and densely detailed curved seating bowl reaching both left and right edges, grey stairs and aisles, stadium structure with realistic sunlight and shadows, no sky visible. Lower 70 percent: a perfectly empty continuous green football grass surface, subtle natural mowing texture and daylight. CRITICAL: the grass must have NO white pitch markings whatsoever, NO goal nets, NO goalposts, NO corner flags, NO football, NO people, NO advertising text or logos, NO labels, NO app UI, NO border, NO watermark; the game will add all exact field markings, goals and animated players later in code. Keep the stadium realistic and highly detailed, not an illustration, not vector art, not a diagram. Wide horizontal 1536 by 1024 or wider composition.
