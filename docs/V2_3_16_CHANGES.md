# BOSS XI 2.3.16 — stadium pitch and match presentation

Based on delivered 2.3.15 source d2fd8bb04eaf8df94cdcd21566c841599a23c6c4.
Package and update signing key are retained; version code advances to 42.

- The live match uses an original stadium perspective inspired by the supplied
  reference: roof and seating, grass apron, alternating mowing stripes, corner
  flags, advertising boards and raised goals with nets. Markings use a 105 x 68 m
  field, 16.5 m penalty areas, 5.5 m goal areas, 9.15 m centre circle and correct
  penalty arcs. Players, ball, ball shadow and offside line use one ground-plane
  projection, aligned with the match's actual goal and touchline coordinates.
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
- Goal commentary alternates the scoring team's colour and a light background
  for 3.6 seconds, retaining the goal message while later commentary arrives.
  The effect uses wall time, stops when leaving the match, and returns to the
  current commentary after its deadline.

Validation will be recorded after native Android screenshot and feature checks.
