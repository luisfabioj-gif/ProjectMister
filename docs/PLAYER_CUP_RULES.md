# Player cup restrictions — research checkpoint, 12 October 2026

These are verified implementation inputs. They do not claim that player cup
eligibility or suspension enforcement is finished. The national-rules milestone
remains pending even after final league decisions pass Android QA.

## Verified current inputs

| Competition | Ordinary cautions / participation input | Primary source |
| --- | --- | --- |
| UEFA club qualifying | Third and subsequent odd cautions suspend; after each two eligible qualifying rounds, reduce unused cautions by one | UEFA 2026/27 article 63 |
| UEFA club league phase and knockout | **Fourth**, then subsequent even cautions suspend. Qualifying cautions and pending yellow bans expire before the league phase. Cautions expire after quarter-finals, but a pending quarter-final ban survives into the semi-final | UEFA 2026/27 article 63 |
| DFB-Pokal | Fifth caution suspends for the next cup match. A sending-off suspension carries to the next edition | DFB official competition format, section 7 |
| Copa del Rey | Third caution suspends. Reset caution cycles after quarter-finals; do not erase an already incurred suspension | RFEF March 2026 disciplinary code, article 119 |
| KNVB Cup, professional players | Second caution suspends; third does not; fourth and every further caution suspend. Counts and suspensions belong to the cup category | KNVB paid-football regulations, July 2026, article 31 |
| Coppa Italia, published 2025 code | Each two cautions incur one cup match of suspension; cup penalties are separate from ordinary league penalties. Confirm against the latest revision before treating this as 2026/27 enforcement | FIGC sporting-justice code, article 19 |
| Portugal, professional league yellow-card accumulation | Fifth, ninth, twelfth, fourteenth and each next even caution incur a league match of suspension. Taça de Portugal / Taça da Liga cautions do **not** enter this league count | Liga Portugal July 2026 disciplinary regulations, articles 164–165 |
| FA Cup | Up to two clubs in an edition; only one club in any round. Unused substitutes do not count as having played. Match registration deadlines still apply | FA Cup 2026/27 rules 95–103 |
| Carabao Cup | Winter movers may play for the new club in the final; may not represent different clubs in the two semi-final legs. Loans are allowed unless the parent explicitly refuses | EFL official 6 August 2026 rule-change notice; full current cup document still to be read |
| Coupe de France | In the competition proper, the match sheet needs at least seven players who appeared in one of the first team's last two official matches, except during the stated FIFA senior international period. Match sheets allow 16 / 18 / 20 players depending on round | FFF Coupe de France 2026/27, article 7.3 |

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

Still to finish verifying: the latest FIGC and French professional disciplinary revisions,
Belgian cup penalties, current Scottish Judicial Panel rules, current Turkish
article 93 and the complete 2026/27 Carabao Cup eligibility text. Older summaries
do not establish their current rule versions. Special administrative hearings,
medical exceptions and sanctions for incidents the game cannot generate should
remain explicitly outside the ordinary simulated match model.
