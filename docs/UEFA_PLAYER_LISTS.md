# UEFA player lists — 13 October 2026 implementation checkpoint

The current implementation has 27 passing host registration cases, 25 profile
cases and signed app/QA compilation. It is awaiting its own Android 35/36 gate;
no completion points or device-validation claim are added for source alone.

The Squad and Europe screens open **European player lists**. List A can be edited
and submitted before the current deadline. The saved automatic list covers the
ordinary roster; eligible youth players enter List B before each match. A late
free agent remains available domestically but cannot enter the closed league-phase
List A. Three eligible new players can be registered for the knockout phase by
4 February; later knockout rounds keep the saved list. Rejected submissions do
not change membership or consume extra places.

List A has at most 25 players. Its capacity is 17 plus the eligible reserved
places, up to eight; at most four association-trained players fill those reserved
places. Training comes from saved club-registration periods and completed domestic
season boundaries, independent of nationality. Birth dates and training periods
support the List B two-year rule, the age-16 exception and one same-association
loan of at most a year within three consecutive years. Initial youth histories
are generated attributes of fictional players; earlier first-team appearances
and match cards are never fabricated.

At least two actual goalkeepers appear on A and three across A/B. Participating
clubs promote a real generated academy keeper when needed. Promotions and market
hires share an append-only identity order, preserving older IDs and portraits.
An outgoing permanent goalkeeper can be replaced after league-phase matchday six
in Champions/Europa League; the added keeper uses a winter place. The Conference
League has six matchdays and no corresponding transfer exception.

UEFA selection, the frozen opponent bench, substitutions and suspension service
all require the current A/B list plus ordinary club/qualification eligibility.
Host integration checks prove that a transferred player outside A/B cannot serve
a UEFA ban through the new club's match, then can serve it after valid winter
registration. The added native Portuguese scenario plays actual league-phase and
knockout matches, rejects an unlisted forced starter, records an actual second-yellow
dismissal, serves it through an eligible UEFA fixture and admits only three of four
new players. It also checks interleaved market/academy IDs, portraits, exact saves
and atomic rejection of missing list/profile fields. It still needs passing logs.

All three save slots and typed backups preserve `roster_extensions`,
`player_training_history` and `uefa_player_lists`, with required version flags.
Goalkeeper flags are validated against immutable player origins. Annual lists
start empty and retain five previous editions; training histories persist. Legacy
careers receive generated profile metadata and use only known saved registration
dates. Existing baseline registrations with unknown earlier history are grandfathered
when reconstructing a first list; known late signings do not receive that exception.

The ordinary managed career model has ten winter-calendar associations. It does
not generate the special extra winter registrations for summer championships,
medical hearings or DNCG restrictions. Generated injuries last at most four weeks,
so UEFA's 30-day goalkeeper / 60-day outfield medical replacements cannot be
triggered by an ordinary generated injury. These exclusions do not manufacture
approvals or claim full regulatory hearing simulation.

Sources retrieved directly from UEFA on 13 October 2026:

- [Champions League article 31](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-31-Player-lists-Online): qualifying deadlines 2/16/30 July and 13 August 2026.
- [Europa League article 31](https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-31-Player-lists-Online): qualifying deadlines 3/17/31 July and 14 August 2026.
- [Conference League article 31](https://documents.uefa.com/r/Regulations-of-the-UEFA-Conference-League-2026/27/Article-31-Player-lists-Online): the same qualifying deadlines as Europa League.
- All three league-phase List A deadlines are **2 September 2026**; List B closes the day before each match. Qualifying permits two later A additions by the day before the first leg.
- [Champions League article 32](https://documents.uefa.com/r/Regulations-of-the-UEFA-Champions-League-2026/27/Article-32-Subsequent-registration-Online), [Europa League article 32](https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-32-Subsequent-registration-Online) and [Conference League article 32](https://documents.uefa.com/r/Regulations-of-the-UEFA-Conference-League-2026/27/Article-32-Subsequent-registration-Online): three new knockout players, including former other-club participants, by **4 February 2027**.

Later career deadlines project those rules; they are not presented as future
published UEFA decisions.
