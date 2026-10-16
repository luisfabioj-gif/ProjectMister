#!/usr/bin/env python3
"""Complete derived observations from dated ESPN squads and referenced kit articles."""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.parse import urlencode
import json,re
ROOT=Path(__file__).resolve().parents[1]
seed=json.loads((ROOT/'tools/data/club-profiles-review.json').read_text())
sources=json.loads((ROOT/'tools/data/alternate-club-sources.json').read_text())
FIFA=dict(x.split(':',1) for x in ('ENG:England|SCO:Scotland|WAL:Wales|NIR:Northern Ireland|IRL:Ireland|POR:Portugal|ESP:Spain|FRA:France|ITA:Italy|GER:Germany|NED:Netherlands|BEL:Belgium|TUR:Türkiye|BRA:Brazil|ARG:Argentina|URU:Uruguay|PAR:Paraguay|CHI:Chile|COL:Colombia|ECU:Ecuador|VEN:Venezuela|PER:Peru|BOL:Bolivia|MEX:Mexico|USA:United States|CAN:Canada|JAM:Jamaica|CRC:Costa Rica|PAN:Panama|HON:Honduras|DOM:Dominican Republic|HAI:Haiti|AUS:Australia|NZL:New Zealand|JPN:Japan|KOR:Korea, South|CHN:China|DEN:Denmark|SWE:Sweden|NOR:Norway|FIN:Finland|ISL:Iceland|AUT:Austria|SUI:Switzerland|POL:Poland|CZE:Czech Republic|SVK:Slovakia|HUN:Hungary|ROU:Romania|BUL:Bulgaria|CRO:Croatia|SRB:Serbia|BIH:Bosnia and Herzegovina|MNE:Montenegro|SVN:Slovenia|MKD:North Macedonia|ALB:Albania|KOS:Kosovo|GRE:Greece|CYP:Cyprus|UKR:Ukraine|RUS:Russia|GEO:Georgia|ARM:Armenia|AZE:Azerbaijan|KAZ:Kazakhstan|UZB:Uzbekistan|ISR:Israel|LUX:Luxembourg|LTU:Lithuania|LVA:Latvia|EST:Estonia|MLT:Malta|CMR:Cameroon|CIV:Cote d\'Ivoire|SEN:Senegal|MLI:Mali|NGA:Nigeria|GHA:Ghana|GAM:The Gambia|GUI:Guinea|GNB:Guinea-Bissau|CPV:Cape Verde|ANG:Angola|COD:DR Congo|COG:Congo|MAR:Morocco|ALG:Algeria|TUN:Tunisia|EGY:Egypt|BFA:Burkina Faso|BEN:Benin|TOG:Togo|GAB:Gabon|EQG:Equatorial Guinea|RSA:South Africa|ZIM:Zimbabwe|ZAM:Zambia|MOZ:Mozambique|BDI:Burundi|COM:Comoros|MTN:Mauritania|RWA:Rwanda|TAN:Tanzania|UGA:Uganda|KEN:Kenya|SLE:Sierra Leone|LBR:Liberia|IRN:Iran|IRQ:Iraq|SYR:Syria|LBN:Lebanon|JOR:Jordan|PLE:Palestine|KSA:Saudi Arabia|QAT:Qatar').split('|'))
def nation(v):return FIFA.get(v.strip(),v.strip())
def get(url):
    with urlopen(Request(url,headers={'User-Agent':'BOSS-XI-public-data-review/1.0','Accept-Encoding':'identity'}),timeout=35) as r:
        body=r.read()
        if r.status!=200 or not body:raise RuntimeError('Unavailable public observation '+url)
        return json.loads(body)
def wiki(title):
    q=urlencode(dict(action='query',titles=title,redirects=1,prop='revisions',rvprop='ids|content',rvslots='main',format='json',formatversion=2))
    p=get('https://en.wikipedia.org/w/api.php?'+q)['query']['pages'][0]
    if p.get('missing'):raise RuntimeError('Unknown club article '+title)
    rev=p['revisions'][0];return rev['slots']['main']['content'],f'https://en.wikipedia.org/w/index.php?oldid={rev["revid"]}',p['title']
def squad(text):
    start=re.search(r'(?im)^=+\s*(?:Current squad|First.team squad|First team|Senior squad)\s*=+',text)
    if not start:return {}
    part=text[start.end():];stop=re.search(r'(?im)^==[^=]|^=+\s*(?:Out on loan|On loan|Reserve|Coaching)',part)
    if stop:part=part[:stop.start()]
    counts=Counter()
    for row in re.findall(r'(?is)\{\{\s*Football squad player\s*\|(.*?)\}\}',part):
        m=re.search(r'(?:^|\|)\s*nat\s*=\s*([^|\n}]+)',row)
        if m:counts[nation(m.group(1))]+=1
    return dict(sorted(counts.items()))
def complete(p):
    entry=sources.get(p['id'])
    if not p.get('nationalities'):
        espn=(entry or {}).get('espnId') or p.get('espnId')
        if espn:
            cc={'ENG':'eng','ES':'esp','DE':'ger','IT':'ita','FR':'fra','PT':'por','NL':'ned','BE':'bel','SCO':'sco','TR':'tur'}[p['country']]
            url=f'https://site.api.espn.com/apis/site/v2/sports/soccer/{cc}.{p["tier"]}/teams/{espn}/roster?season=2026'
            data=get(url);athletes=[]
            for group in data.get('athletes',[]):athletes.extend(group.get('items',[group]))
            counts=Counter(nation(a['citizenship']) for a in athletes if isinstance(a.get('citizenship'),str) and a['citizenship'])
            if data.get('season',{}).get('year')==2026 and sum(counts.values())>=12:p.update(nationalities=dict(sorted(counts.items())),rosterSize=sum(counts.values()),rosterSource=url)
    if entry:
        text,page,title=wiki(entry['wiki']);p['kitReference']=page;p['kitArticle']=title
        observed=dict(re.findall(r'(?im)^\|\s*((?:leftarm|rightarm|body|shorts|socks)1)\s*=\s*([0-9A-Fa-f]{6})\b',text));p['observedKitFields']=observed
        if entry.get('colours'):
            if not observed:raise RuntimeError('Club kit observation absent '+p['id'])
            p['primary'],p['secondary']=entry['colours'];p['colourSource']=page
        if not p.get('nationalities'):
            counts=squad(text)
            if sum(counts.values())>=12:p.update(nationalities=counts,rosterSize=sum(counts.values()),rosterSource=page)
    if not p.get('nationalities') or sum(p['nationalities'].values())<12:raise RuntimeError('Missing observed roster '+p['id'])
    if not p.get('primary') or not p.get('secondary'):raise RuntimeError('Missing observed club colours '+p['id'])
    return p
errors=[]
with ThreadPoolExecutor(max_workers=3) as pool:
    for f in as_completed([pool.submit(complete,p)for p in seed['clubs']]):
        try:p=f.result();print('VERIFIED',p['id'],p['rosterSize'],flush=True)
        except Exception as e:errors.append(str(e));print('ERROR',str(e),flush=True)
seed['errors']=errors;seed['verifiedOn']='2026-10-16';seed['nationalityMethod']='Primary citizenship from dated TM or current 2026 ESPN squads, with referenced current-squad articles where needed; largest-remainder apportionment to twenty fictional players.'
Path('/tmp/club-profile-cache').mkdir(exist_ok=True);Path('/tmp/club-profile-cache/profiles-progress.json').write_text(json.dumps(seed,ensure_ascii=False,indent=2)+'\n')
print('RESULT',len(seed['clubs']),'clubs;',len(errors),'errors',flush=True)
if errors:raise SystemExit(2)
(ROOT/'app/src/main/assets/competitions/club-profiles-2026.json').write_text(json.dumps(seed,ensure_ascii=False,separators=(',',':'))+'\n')
