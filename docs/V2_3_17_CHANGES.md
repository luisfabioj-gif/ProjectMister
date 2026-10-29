# BOSS XI 2.3.17 — physical match flow and readable tactics

Based on delivered 2.3.16 source d7dfce1f3485769e8013949a10fe932eaf5f1b00.
Version code 43 preserves the application ID, save format and update signing key.

- Passes retain the destination chosen at the kick. Players run to that point;
  the ball no longer bends or changes its endpoint to follow a moving receiver.
  Control requires physical contact. Unreachable passes remain loose and wait
  for a player to recover them. Completed passes are recorded on reception,
  including crosses, with missed receptions recorded as missed passes.
- Defenders must be within challenge distance before a dribble can be lost or a
  duel can award a tackle/foul. Distant defenders close down while the attacker
  carries into space. Successful close challenges also populate the player's
  contribution statistics.
- Live tactics tokens give current ability and condition their own full-width
  row. The numbered identity and name stay above, so compact layouts retain both
  statistics. Native checks measure glyph widths and row heights in all five
  formations, in addition to the existing visibility/overlap checks.
- Resuming at half-time immediately enters the physical kickoff reset, clearing
  an unfinished first-half flight before either side can act in the second half.

Validation is pending native Android checks. The independent runner covers fixed
trajectories under receiver movement, contact-based completions, missed-pass
recovery, distant and close challenges, saved-career upgrades and the retained
2.3.16 feature checks. No new bitmap or audio assets are added.
