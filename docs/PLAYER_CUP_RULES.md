# Player cup restrictions — implementation checkpoint, 12 October 2026

The new player disciplinary ledger is connected to actual watched and background
fixtures, stable player identities, automatic team selection, substitutions,
transfers, profiles, annual saves and atomic backup validation. One hundred host
checks and app/QA compilation pass. Run 38296369629 exposed that the outer backup
schema did not yet allow the two new discipline fields. The following checkpoint
adds their strict string/integer types and verifies actual pending bans through
typed backups in all three slots. The added Android cup-player scenario still
needs to pass; the national-rules milestone remains pending. Competition player
participation limits, match-sheet restrictions and UEFA registration lists are
separate unfinished integration work. Earlier cards are not fabricated when an
existing career is upgraded.

## Verified current inputs

| Competition | Ordinary cautions / participation input | Primary source |
| --- | --- | --- |
| UEFA club qualifying | Third and subsequent odd cautions suspend; after each two eligible qualifying rounds, reduce unused cautions by one | UEFA 2026/27 article 63 |
| UEFA club league phase and knockout | **Fourth**, then subsequent even cautions suspend. Qualifying cautions and pending yellow bans expire before the league phase. Cautions expire after quarter-finals, but a pending quarter-final ban survives into the semi-final | UEFA 2026/27 article 63 |
| DFB-Pokal | Fifth caution suspends for the next cup match. A sending-off suspension carries to the next edition | DFB official competition format, section 7 |
| Copa del Rey | Third caution suspends. Reset caution cycles after quarter-finals; do not erase an already incurred suspension | RFEF March 2026 disciplinary code, article 119 |
| KNVB Cup, professional players | Second caution suspends; third does not; fourth and every further caution suspend. Counts and suspensions belong to the cup category | KNVB paid-football regulations, July 2026, article 31 |
| Coppa Italia | Each two cautions incur one cup match of suspension; cup penalties are separate from ordinary league penalties and pending bans follow a transfer / next edition | FIGC sporting-justice code, article 19/21, latest version linked by FIGC checked 12 October: 22 December 2025 |
| Portugal, professional league yellow-card accumulation | Fifth, ninth, twelfth, fourteenth and each next even caution incur a league match of suspension. Taça de Portugal / Taça da Liga cautions do **not** enter this league count | Liga Portugal July 2026 disciplinary regulations, articles 164–165 |
| FA Cup | Up to two clubs in an edition; only one club in any round. Unused substitutes do not count as having played. Match registration deadlines still apply | FA Cup 2026/27 rules 95–103 |
| Carabao Cup | Winter movers may play for the new club in the final; may not represent different clubs in the two semi-final legs. Loans are allowed unless the parent explicitly refuses | EFL official 6 August 2026 rule-change notice; full current cup document still to be read |
| Coupe de France | In the competition proper, the match sheet needs at least seven players who appeared in one of the first team's last two official matches, except during the stated FIFA senior international period. Match sheets allow 16 / 18 / 20 players depending on round | FFF Coupe de France 2026/27, article 7.3 |
| French professional first teams | Five cautions across league / Coupe de France / Trophée des Champions incur a ban. A firm commission sanction clears cautions up to the decision; subsequent cautions remain. Yellow bans begin the following Tuesday | LFP disciplinary rules 2026/27, articles 11/12/14; the career projects weekly Wednesday commission dates |
| Belgian Cup | Two cautions through round four; **three** from round five. Before round five, clear earlier cautions and pending yellow bans. An unserved cup yellow / double-yellow ban does not carry into the next season | Current RBFA Book B, articles B11.184/188/193 |
| Scottish Cup / League Cup | Each two cautions suspend. Scottish Cup resets before rounds one and four; League Cup resets before round two. Accumulation ceases before semi-finals; an already incurred ban remains. Repeated sending-off offences add a match per previous offence in that cup | Scottish FA JPP 2026/27, annex C, sections 2/3 |
| FA Cup / Carabao Cup | Current FA Cup competition-proper caution threshold is **two**, regardless of entry at first or third round, with further bans every two. Carabao thresholds two and four. Both stop accumulating after quarter-finals. Domestic sending-off recurrence adds a match per earlier offence | FA Handbook 2026/27, on-field regulations 24–34 and tables 1/2 |

The old three-caution UEFA league-phase rule is superseded in the retrieved
2026/27 regulations. Do not implement it from an older explainer. Qualification
registration histories, club transfers and stage resets must be saved; a card
counter alone cannot establish eligibility or serve a suspension correctly.
Background fixtures require actual simulated player appearances, separate from
unused benches, and watched fixtures must use the players who really appeared.

## Source links

- https://documents.uefa.com/r/Regulations-of-the-UEFA-Europa-League-2026/27/Article-63-Yellow-and-red-cards-Online
- https://www.dfb.de/maenner/wettbewerbe/dfb-pokal/modus
- https://rfef.es/sites/default/files/2026-03/1283_Codigo_Disciplinario__Futbol__CD_03.03.2026.pdf
- https://www.knvb.nl/downloads/bestand/29888/reglementen-betaald-voetbal
- https://www.figc.it/media/276306/codice-di-giustizia-sportiva-figc_modifica_-aggiornato_su_cu_18a_del_10-07-2025.pdf
- https://www.ligaportugal.pt/backoffice/assets/20260701_RD_53c4500043.pdf
- https://www.thefa.com/-/media/thefacom-new/files/competitions/2026-27/rules/rules-of-the-fa-challenge-cup-2026-27.ashx
- https://www.efl.com/news/2026/august/06/carabao-cup-2026-27---key-information-/
- https://media.fff.fr/uploads/documents/reglement-coupe-de-france-credit-agricole-2026-2027.pdf
- https://assets.ctfassets.net/qu0slcxvbwp6/4TZlz8yWaTibtBRSUk8Ai0/48f87db2a4679b5ad04a0039fe8683a5/Men-s_Scottish_Cup_Rules_2026-27.pdf
- https://www.tff.org/Resources/TFF/Auto/dd8f8b54192c42de80c71cd95068f530.pdf
- https://assets.ctfassets.net/qu0slcxvbwp6/4noZMOOZQZFerq3dDvCLzx/5e1f7f86c9c860d6d283eac5aad90dc1/JPP_2026-27.pdf
- https://belgianfootball.s3.eu-central-1.amazonaws.com/s3fs-public/rbfa/docs/pdf/reglement/bondsreglement_reglement_federal/KBVB_bondsreglement_Boek_B.pdf
- https://www.lfp.fr/assets/26_27_Reglement_Disciplinaire_25_06_2026_1decd7a4a3.pdf
- https://www.thefa.com/-/media/files/thefaportal/governance-docs/rules-of-the-association/2026-27/the-fa-handbook-2026-27.ashx
- https://www.figc.it/it/federazione/norme/codice-di-giustizia-sportiva

Still to finish: the complete 2026/27 Carabao Cup eligibility text and integration
of actual participation / registration histories. Turkish article 93 was checked
against the current TFF PDF: four cautions per category; a sending-off removes
that match's cautions from accumulation; yellow cards and pending yellow bans
expire at season end. Spanish article 119.4 exempts the caution that would complete
a cycle in the club's final game in that competition; this result-dependent
exception is connected to completed ties and covered by host tests. Special administrative hearings,
medical exceptions and sanctions for incidents the game cannot generate should
remain explicitly outside the ordinary simulated match model.
