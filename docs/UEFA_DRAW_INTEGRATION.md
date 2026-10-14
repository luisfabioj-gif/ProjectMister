# UEFA career draws — updated 14 October 2026

The 14 October Android England regression captured six English qualifiers with
five projected coefficient seeds in pot one. No legal draw can satisfy those
pots: every association club needs two opponents from each pot, while each
other club in that pot can face at most two from that association. For a nine-club
pot, `association total + association clubs in the pot <= 9` is necessary.
Changing the random seed cannot resolve this contradiction.

Simulated later editions now exchange the closest seeds only when that capacity
is violated. The titleholder stays in pot one. Every qualified club, coefficient
and original pot is retained; no association, opponent-count or venue restriction
is dropped. Original and adjusted pots are saved in ULP2 metadata, shown on the
competition screen and retained in archives. Unadjusted ULP1 saves and previously
successful seeded draws retain their original representation and draw algorithm.
The Conference equivalent is `association total <= 2 * (6 - pot count)`.

This is an explicit **career projection**, not a claim that UEFA has announced
those future pots. Article 16.03 permits administrative changes to draw conditions
to avoid deadlocks; the game models that discretion through saved seed exchanges.
Current qualified fields keep their published pots. The captured Android draw,
original fixtures and seed are a host regression fixture. Complete current
Android 35/36 evidence is still required.

## Original 9 October preparation

`EuropeanDraw` now generates seeded simulated league-phase draws from an explicit
qualified 36-club field. It solves club placement against association exclusions,
at most two opponents from another association, and the preceding edition's
same-venue pairings. Repository-owned templates preserve pot opponents, venue
balance, one game per club per matchweek, one home/one away in the opening and
closing pairs and at most two consecutive home or away games. Saves store the
generated draw rather than generating another draw on reload.

Core validation covers 36 distinct draws and 18 consecutive editions across all
three competitions using the actual 2026/27 Champions League multi-club
association distribution with synthetic IDs/coefficients. It checks deterministic
seed replay, all pot/association/venue constraints, prior-season exclusions,
canonical saves and impossible-field rejection. No optional numerical solver is
required in the APK or CI; the templates are committed Java data.

This is preparation for the career integration. It does not award European places
or create a playable European screen by itself and earns zero verified milestone
points. Qualification/titleholder/performance-place allocation, qualifying-round
movement, dated live/quick matches, career saves/backups, archives and Android
verification remain required for the UEFA milestone.

Primary rules rechecked on 9 October 2026:

- https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-16-Draw-system-league-phase-Online
- https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-17-Match-system-league-phase-Online
- https://documents.uefa.com/r/Regulations-of-the-UEFA-Conference-League-2026/27/Article-16-Draw-system-league-phase-Online
- https://www.uefa.com/uefachampionsleague/news/02a8-21717d0c6cb5-03a9a5ff1552-1000--champions-league-league-phase-draw-pots-confirmed/
- https://www.uefa.com/uefachampionsleague/news/02a8-215821715a96-9a3b43fad585-1000--uefa-champions-league-league-phase-draw/
