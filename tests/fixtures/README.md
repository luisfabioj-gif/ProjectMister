# Synthetic UEFA draws

These CSV files contain zero-based `round,home,away` identifiers used only by
engine tests. They are generated graph matchings, **not real UEFA fixtures**.
Each club has a distinct synthetic association; duplicate-association and
invalid-draw cases are then introduced explicitly by the tests.

- Champions/Europa: club `i` belongs to pot `i % 4`. The graph connects `i` to
  `(i + d) % 36` for offsets 1 through 4, directed home to away, partitioned
  into eight perfect matchings.
- Conference: club `i` belongs to pot `i / 6`, partitioned into six perfect
  matchings. Each club meets one opponent from every pot, with one home and
  one away match across each paired-pot group.

Tests remap identifiers to noncontiguous IDs (and different IDs each year),
so implementation cannot depend on array offsets being club identities.
