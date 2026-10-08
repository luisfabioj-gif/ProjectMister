# BOSS XI 2.3.8 development checkpoint

New linked careers in England, Spain, Italy, France, the Netherlands, Belgium,
Scotland and Türkiye include recurring national cups. England has both the FA
Cup and League Cup; Scotland has both the Scottish Cup and League Cup.

Cup games use the existing watched/quick match flow. League points remain
separate. Every completed edition retains its draw, results and winner in History,
and the next year receives a new field based on the career's sporting order.
The Scottish League Cup adds eight groups of five, two home and two away games,
shootout bonus points, European byes and the seeded last sixteen.

New careers include real lower cup clubs and the foreign professional catalog
before player IDs are generated. Existing careers retain their identity registry.
The Italian cup follows its fixed 44-team path and seeded venues; Spanish lower
categories host, while French clubs receive home advantage for a two-level gap.
Two-leg result screens retain the aggregate and pending return leg.

The first 2026 fields use published English, Dutch and Scottish European
exemptions, Spanish Super Cup entrants and Italian seed order. Lower qualifying,
subsequent-year allocations, draws and individual dates within published windows
are simulated. Full UEFA admissions and lower-pyramid movement are separate
unfinished milestones.

Local Java tests pass 36 national cup engine editions (3,854 dated match events)
and 15 Scottish League Cups with two through six European byes. The production
source and independent Android runner compile locally. Android 35/36 acceptance
now exercises two full seasons per new country, watched and quick results,
monotonic dates, saves, exact archives and atomic rejection of corrupt backups.
Those device checks must pass before these milestones earn completion weight.

The completion tracker remains at its last verified checkpoint until workflow
evidence is available. This build does not claim the entire game is finished.
