#!/usr/bin/env python3
"""Build a dated, reviewable club snapshot from public squad/club pages.

No private transfer budget is asserted: the game allocation and strength are
explicit estimates calibrated from the observed squad market value.
Raw responses are cached outside the repository so interrupted runs can resume.
"""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import difflib
import hashlib
import gzip
import json
import math
from pathlib import Path
import re
import sys
import time
import unicodedata
from urllib.request import Request, urlopen

try:
    from bs4 import BeautifulSoup
except ImportError:
    sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'task-python'))
    from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
TM = 'https://www.transfermarkt.co.uk'
CODES = {'ENG': ('GB1','GB2','eng'), 'ES': ('ES1','ES2','esp'),
         'DE': ('L1','L2','ger'), 'IT': ('IT1','IT2','ita'),
         'FR': ('FR1','FR2','fra'), 'PT': ('PO1','PO2','por'),
         'NL': ('NL1','NL2','ned'), 'BE': ('BE1','BE2','bel'),
         'SCO': ('SC1','SC2','sco'), 'TR': ('TR1','TR2','tur')}

def normal(s):
    s = unicodedata.normalize('NFKD', s.casefold()).encode('ascii','ignore').decode()
    s = s.replace('&', 'and').replace('utd', 'united')
    s = re.sub(r'\b(fc|cf|sc|afc|ac|as|ud|cd|sv|fk|sk|vfb|vfl|tsg|rsc|ssc|bsc|bv|1900|1904|1899)\b', '', s)
    s = s.replace('munchen','munich').replace('koln','cologne').replace('nurnberg','nuremberg')
    return re.sub('[^a-z0-9]', '', s)

ALIASES = {'intermilan':'inter', 'internazionale':'inter', 'sportingcp':'sportinglisbon',
           'psv':'psveindhoven', 'ajax':'ajaxamsterdam', 'azalkmaar':'azalkmaar',
           'parissaintgermain':'parissg', 'parissaintgermainfc':'parissg',
           'athleticclub':'athleticbilbao', 'atleticomadrid':'atleticodemadrid',
           'vitoriasc':'vitoriaguimaraes', 'bodo/glimt':'bodoglimt',
           'estreladaamadora':'estrelaamadora', 'estrelaamadora':'estrelaamadora',
           'jongajax':'ajaxamsterdamu21', 'jongpsv':'psveindhovenu21',
           'jongaz':'azalkmaaru21', 'jongutrecht':'fcutrechtu21',
           'clubnxt':'clubbruggekvii', 'celticb':'celticfcreserve',
           'bayernmunchen':'bayernmunich', 'nacional':'cdnacional',
           'benfica':'benficalisbon', 'slbenfica':'benficalisbon'}

def key(s):
    n=normal(s)
    return normal(ALIASES.get(n,n))

def money(s):
    m=re.search(r'([\d.,]+)\s*(bn|m|k)?',s)
    if not m:return 0
    return round(float(m.group(1).replace(',','')) * {'bn':1e9,'m':1e6,'k':1e3,None:1}[m.group(2)])

def main():
    a=argparse.ArgumentParser();a.add_argument('--cache',type=Path,required=True)
    a.add_argument('--workers',type=int,default=3);a.add_argument('--rosters',action='store_true')
    a.add_argument('--cache-only',action='store_true');a.add_argument('--seed',type=Path)
    args=a.parse_args();args.cache.mkdir(parents=True,exist_ok=True)
    def fetch(url, optional=False):
        p=args.cache/(hashlib.sha256(url.encode()).hexdigest()+'.html')
        if p.exists():return p.read_bytes()
        if args.cache_only:
            if optional:return None
            raise RuntimeError('Not cached: '+url)
        try:
            with urlopen(Request(url,headers={'User-Agent':'Mozilla/5.0 (compatible; BOSS-XI-data-review/1.0)','Accept-Encoding':'identity'}),timeout=40) as r:
                body=r.read();encoding=r.headers.get('Content-Encoding','').lower()
                if encoding=='gzip' or body[:2]==b'\x1f\x8b':body=gzip.decompress(body)
                elif encoding=='br':
                    import brotli
                    body=brotli.decompress(body)
                elif encoding=='zstd':
                    import zstandard
                    body=zstandard.ZstdDecompressor().decompress(body)
                if body and body[:1] not in (b'<',b'{',b'[',b'\n',b' '):print('RESPONSE',url,encoding,len(body),body[:6].hex(),flush=True)
            temporary=p.with_suffix('.tmp');temporary.write_bytes(body);temporary.replace(p)
            time.sleep(.4);return body
        except Exception as e:
            if optional:return None
            raise RuntimeError(f'{url}: {e}') from e
    if args.seed:
        previous=json.loads(args.seed.read_text());output=previous['clubs'];unmatched=previous.get('unmatched',[]);errors=[]
    else:
        catalog=json.loads((ROOT/'app/src/main/assets/competitions/2026-27.json').read_text())
        divisions=catalog['divisions']; league_rows={};espn_rows={};errors=[]
        def league(d):
            cc,tier=d['countryCode'],d['tier'];code=CODES[cc][tier-1]
            url=f'{TM}/league/startseite/wettbewerb/{code}/saison_id/2026/plus/1'
            soup=BeautifulSoup(fetch(url),'html.parser');rows=[]
            for tr in soup.select('table.items > tbody > tr'):
                link=tr.select_one('td.hauptlink a[href*="/verein/"]')
                if not link:continue
                name=link.get_text(' ',strip=True)
                if not name:continue
                href=link['href'];tmid=re.search(r'/verein/(\d+)',href).group(1)
                if any(r['tmId']==tmid for r in rows):continue
                values=[td for td in tr.select('td.rechts') if '€' in td.get_text()]
                value=values[-1] if values else None
                cells=tr.find_all('td',recursive=False)
                rows.append({'name':name,'tmId':tmid,'marketValueEUR':money(value.get_text(' ',strip=True)) if value else 0,
                             'source':url,'rosterSource':f'{TM}/club/kader/verein/{tmid}/saison_id/2026/plus/1',
                             'observedTable':tr.get_text(' ',strip=True)})
            if len(rows)<8:raise RuntimeError(f'Incomplete league {cc}/{tier}: {len(rows)}')
            eurl=f'https://site.api.espn.com/apis/site/v2/sports/soccer/{CODES[cc][2]}.{tier}/teams?limit=1000'
            raw=fetch(eurl,True);teams=[]
            if raw:
                try:teams=[x['team'] for x in json.loads(raw)['sports'][0]['leagues'][0]['teams']]
                except (ValueError,KeyError,IndexError):pass
            return (cc,tier),rows,teams,eurl
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            for f in as_completed([pool.submit(league,d) for d in divisions]):
                try:
                    ident,rows,teams,eurl=f.result();league_rows[ident]=rows;espn_rows[ident]=(teams,eurl)
                    print('LEAGUE',ident,len(rows),'teams;',len(teams),'colour records',flush=True)
                except Exception as e:errors.append(str(e));print('ERROR',str(e),flush=True)
        existing=args.cache/'manual-matches.json'
        committed=ROOT/'tools/club_profile_matches.json'
        overrides=json.loads(committed.read_text()) if committed.exists() else {}
        if existing.exists():overrides.update(json.loads(existing.read_text()))
        output=[];unmatched=[]
        for d in divisions:
            ident=(d['countryCode'],d['tier']);rows=league_rows.get(ident,[]);used=set()
            teams,eurl=espn_rows.get(ident,([],''))
            for c in d['clubs']:
                target=key(c['name']);matches=[r for r in rows if key(r['name'])==target]
                if c['id'] in overrides:matches=[r for r in rows if r['tmId']==str(overrides[c['id']])]
                if not matches:
                    ranked=sorted([(difflib.SequenceMatcher(None,target,key(r['name'])).ratio(),r) for r in rows],key=lambda x:x[0],reverse=True)
                    if ranked and ranked[0][0]>=.79 and (len(ranked)<2 or ranked[0][0]-ranked[1][0]>.08):matches=[ranked[0][1]]
                if len(matches)!=1 or matches[0]['tmId'] in used:
                    unmatched.append({'id':c['id'],'name':c['name'],'division':ident,'candidates':[(r['tmId'],r['name']) for r in rows]});continue
                p=dict(matches[0]);used.add(p['tmId']);p.update({'id':c['id'],'name':c['name'],'country':ident[0],'tier':ident[1]})
                er=[x for x in teams if any(key(x.get(label,'')) in (target,key(matches[0]['name'])) for label in ('displayName','name','shortDisplayName'))]
                if not er:
                    ranked=sorted([(difflib.SequenceMatcher(None,target,key(x.get('displayName',''))).ratio(),x) for x in teams],key=lambda x:x[0],reverse=True)
                    if ranked and ranked[0][0]>=.78 and (len(ranked)<2 or ranked[0][0]-ranked[1][0]>.08):er=[ranked[0][1]]
                if len(er)==1:
                    p.update(primary=er[0].get('color',''),secondary=er[0].get('alternateColor',''),colourSource=eurl,espnId=er[0]['id'])
                output.append(p)
    def roster(p):
        s=None if p.get('nationalities') else BeautifulSoup(fetch(p['rosterSource']),'html.parser');nations=Counter(p.get('nationalities',{}));players=sum(nations.values());seen=set()
        for row in s.select('table.items > tbody > tr') if s else []:
            link=row.select_one('a[href*="/profil/spieler/"]')
            if not link:continue
            identity=re.search(r'/spieler/(\d+)',link['href']).group(1)
            if identity in seen:continue
            seen.add(identity)
            flags=row.select('img.flaggenrahmen')
            if not flags:continue
            nation=flags[0].get('title') or flags[0].get('alt')
            if nation:nations[nation]+=1;players+=1
        if players<12:raise RuntimeError('Incomplete roster '+p['id']+': '+str(players)+'; '+str(s)[:500])
        p['nationalities']=dict(sorted(nations.items()));p['rosterSize']=players
        details=s.select_one('.data-header__details') if s else None
        text=details.get_text(' ',strip=True) if details else s.get_text(' ',strip=True) if s else ''
        m=re.search(r'Stadium:\s*(.*?)\s+([\d.,]+)\s+Seats',text)
        if m:p.update(stadium=m.group(1).strip(),capacity=int(re.sub(r'[^\d]','',m.group(2))))
        p['strengthEstimate']=max(40,min(95,round(47+6.5*math.log(max(.2,p['marketValueEUR']/1e6)))))
        p['transferBudgetEstimateK']=max(100,round(p['marketValueEUR']*.08/1000/50)*50)
        info=f'{TM}/club/datenfakten/verein/{p["tmId"]}'
        body=None if args.seed and p.get('primary') and p.get('secondary') else fetch(info,optional=bool(p.get('primary') and p.get('secondary')))
        detail=BeautifulSoup(body or b'','html.parser')
        colours=[]
        for swatch in detail.select('.vereinsfarbe span'):
            match=re.search(r'background-color:\s*#([a-fA-F0-9]{6})',swatch.get('style',''))
            if match and match.group(1).lower() not in colours:colours.append(match.group(1).lower())
        if colours:
            p['primary']=colours[0];p['secondary']=colours[1] if len(colours)>1 else p.get('secondary') or ('111827' if colours[0]=='ffffff' else 'ffffff')
            p['colourSource']=info;p['clubColours']=colours
        if not p.get('primary') or not p.get('secondary'):raise RuntimeError('Missing real club colours '+p['id']+'; '+str(detail)[:500])
        p['strengthEstimate']=max(40,min(95,round(47+6.5*math.log(max(.2,p['marketValueEUR']/1e6)))))
        p['transferBudgetEstimateK']=max(100,round(p['marketValueEUR']*.08/1000/50)*50)
        return p
    if args.rosters:
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            for f in as_completed([pool.submit(roster,p) for p in output]):
                try:p=f.result();print('ROSTER',p['id'],p['rosterSize'],len(p['nationalities']),flush=True)
                except Exception as e:errors.append(str(e));print('ERROR',str(e),flush=True)
    result={'schema':1,'season':'2026/27','verifiedOn':datetime.now(timezone.utc).date().isoformat(),
            'budgetMethod':'Game board allocation estimate: 8% of observed squad value, rounded to EUR50k; minimum EUR100k. Not a disclosed real club transfer budget.',
            'strengthMethod':'Game estimate: clamp(47 + 6.5 * ln(squad value in EUR millions), 40, 95). Not an official rating.',
            'nationalityMethod':'Primary listed citizenship of each player on the current season squad page; proportional allocation to the fictional game squad.',
            'clubs':output,'unmatched':unmatched,'errors':errors}
    (args.cache/'profiles-progress.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print('RESULT',len(output),'matched;',len(unmatched),'unmatched;',len(errors),'errors',flush=True)
    if unmatched or errors:return 2
    if args.rosters:
        (ROOT/'app/src/main/assets/competitions/club-profiles-2026.json').write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n')
    return 0

if __name__=='__main__':raise SystemExit(main())
