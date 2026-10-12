# BOSS XI 2.3.10 development candidate

- Ordinary free-agent registration uses country/tier rules and explained eligibility;
  Portuguese expiry hires can join outside normal windows before their deadline.
- Forced reserve relegation no longer strands an expanded career. Its division
  vacancy uses an eligible real lower club from simulated qualification with a saved seed;
  identities and squads stay in place. Managers leaving the playable tiers can
  choose another club, as in the existing Portuguese transition.
- New UEFA careers store the whole league calendar at creation. All local entrants
  reserve potential UEFA dates; regular league rounds finish before postseason.
- Promotion and Portuguese lower-barrage legs have saved, dated fixtures, recovery
  gaps and chronological reload/backup validation. Older in-progress calendars
  retain their fixture version; the following edition adopts the new calendar.
- Premier League head-to-head rules cover prospective European-admission places.
- Season-transition failures now include diagnostic evidence in Android logs.
- Removing the saved calendar or league-decision state from a new-schema backup
  is rejected before any existing career or setting is replaced.

The tracker has 89 verified points, including the previous candidate's Android
35/36 UEFA and annual-qualification gate. Final equality now has saved neutral matches/series or administrative draws,
with regular disciplinary ranking tracked separately. Cup player restrictions
and suspensions are still unfinished; current primary inputs are in
`PLAYER_CUP_RULES.md`. The complete rule/calendar
checkpoint still requires its integrated Android gate; private
Play signing/publication and physical phone testing remain outside this gate.
