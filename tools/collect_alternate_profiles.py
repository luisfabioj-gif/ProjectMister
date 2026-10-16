#!/usr/bin/env python3
"""Read alternate public observations where the first source is unavailable."""
import json
from urllib.request import Request,urlopen

for url in [
    'https://en.wikipedia.org/w/api.php?action=query&titles=OGC_Nice&prop=revisions&rvprop=content&rvslots=main&format=json&formatversion=2',
    'https://site.api.espn.com/apis/site/v2/sports/soccer/por.1/teams/5309/roster?season=2026',
    'https://en.wikipedia.org/wiki/OGC_Nice?action=raw',
]:
    try:
        with urlopen(Request(url,headers={'User-Agent':'BOSS-XI-public-data-review/1.0','Accept-Encoding':'identity'}),timeout=30) as r:
            raw=r.read();print('SOURCE',url,'STATUS',r.status,'TYPE',r.headers.get('Content-Type'),'BYTES',len(raw),'PREFIX',raw[:400],flush=True)
    except Exception as error:print('UNAVAILABLE',url,str(error),flush=True)
