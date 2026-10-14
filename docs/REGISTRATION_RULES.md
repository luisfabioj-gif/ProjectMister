# Player registration — 14 October 2026 test candidate

Ordinary contracted signings use the verified country and division windows in
`RegistrationWindow`. The world pool contains fictional professionals whose
contracts expired on 30 June 2026, with no later registration or amateur
reclassification. Eligibility is checked again when signing, before money or
squad state changes. Duplicate hires and budget limits retain their existing gates.

Out-of-window rules for this ordinary pool:

| Association | Ordinary expiry exception |
| --- | --- |
| Portugal | Contract ended by 30 June; last February day, with weekend extension |
| Germany | Unattached at the main deadline; before the winter window opens |
| England, Premier League | Explicit simulated Board approval under V.4 |
| England, EFL | Unattached at preceding deadline; fourth Thursday in March |
| Spain | Contract expired before the preceding window closed |
| Italy | Expired by 30 June; through 31 March |
| France | Released before the main deadline, without amateur reclassification; through winter deadline |
| Netherlands | Explicit simulated KNVB dispensation under article 10 |
| Belgium | End-of-season expiry; through 15 March |
| Scotland | Unregistered at preceding deadline |
| Turkey | The ordinary pool uses the published TFF windows |

Approval and later-year deadlines are explained as career simulations/projections.
The game advances by dates, so official closing-hour limits are represented by
an inclusive closing date. The pool does not invent evidence for medical jokers,
in-season mutual termination, a late international certificate, insolvency or
other exceptional release grounds. Profiles show their contract history, decision
and primary rule source. Medical/administrative appeal procedures are outside
this ordinary professional pool.

Primary references are linked by `PlayerRegistration.source`, including Liga
Portugal 2026/27 regulations, DFL LOS of 21 April 2026, FIGC CU 235/A for
2026/27, current RFEF Competition Regulations, RBFA Book B title 4, KNVB July
2026 rules, Scottish FA 2026/27 handbook, Premier League 2026/27 C/V rules,
EFL 2026/27 regulations and TFF professional-player registration regulations.
France uses article 213 of the LFP administrative regulation updated on
8 September 2026, retrieved directly on 13 October. Article 208 bis also supplies
the two-day ordinary qualification delay saved for actual French signings.
The current primary document is:
https://www.lfp.fr/assets/26_27_Reglement_Administratif_08_09_2026_81737d37a3.pdf

Official windows:
https://www.lfp.fr/article/les-dates-du-mercato-2026-2027

Candidate host tests check both divisions in all ten countries, deadline
inclusivity, later release/registration rejection, projected leap years and
explicit Board/KNVB approvals. The new Android gate checks both divisions of all
ten countries for an off-window ordinary free-agent decision, an out-of-window
contracted rejection, an in-window fee-free hire and exact reload/backup.
Those isolated native cases have passed on both Android versions. The refined
registration checkpoint remains pending the complete integrated Android gate;
isolated checks do not establish French cup continuity or UEFA list completion.
