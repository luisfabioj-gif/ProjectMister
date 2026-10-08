#!/usr/bin/env python3
"""Validate shipped catalog shape and identity invariants without an Android runtime."""
import json
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/assets/competitions/2026-27.json'
d=json.loads(p.read_text())
expected={'ENG':(20,24),'ES':(20,22),'DE':(18,18),'IT':(20,20),'FR':(18,18),'PT':(18,18),'NL':(18,20),'BE':(18,15),'SCO':(12,10),'TR':(18,20)}
assert d['schemaVersion']==1 and d['season']=='2026/27'
assert len(d['divisions'])==20
ids=set(); divisions=set(); reserves=0
for league in d['divisions']:
    key=(league['countryCode'],league['tier'])
    assert key not in divisions;divisions.add(key)
    assert len(league['clubs'])==expected[key[0]][key[1]-1],key
    assert league['source'].startswith('https://')
    for club in league['clubs']:
        assert club['id'] not in ids,club['id'];ids.add(club['id'])
        assert club['name'].strip() and type(club['reserve']) is bool
        assert club['id'].startswith(key[0].lower()+':')
        reserves+=club['reserve']
assert len(ids)==365 and reserves==13
assert divisions=={(c,t) for c in expected for t in (1,2)}
print('PASS: 10 countries, 20 divisions, 365 unique club identities, 13 reserve sides')

cup=json.loads((p.parent/'portugal-cup-2026.json').read_text())
assert cup['schema']==1 and cup['season']==2026
clubs={c['id']:c for c in cup['clubs']}
assert len(clubs)==len(cup['clubs'])==113
assert not set(clubs).intersection(ids)
assert {level:sum(c['level']==level for c in clubs.values()) for level in (3,4,5)}=={3:19,4:52,5:42}
assert len(cup['firstRound'])==47 and all(len(pair)==2 for pair in cup['firstRound'])
opening=[club for pair in cup['firstRound'] for club in pair]+cup['byes']
assert len(cup['byes'])==19 and len(opening)==len(set(opening))==113 and set(opening)==set(clubs)
assert len(cup['lateEntrants'])==len(set(cup['lateEntrants']))==5
assert set(cup['lateEntrants']).issubset(ids)
print('PASS: Portuguese cup opening draw, 113 lower-tier clubs, 19 byes and five late entrants')

german=json.loads((p.parent/'germany-cup-2026.json').read_text())
assert german['schema']==1 and german['season']==2026
lower={c['id']:c for c in german['clubs']}
professional={c['id'] for league in d['divisions'] if league['countryCode']=='DE' for c in league['clubs']}
assert len(lower)==len(german['clubs'])==28 and not set(lower).intersection(ids)
assert all(c['id'].startswith('de:cup-') and c['name'].strip() and c['level'] in (3,4,5) for c in lower.values())
assert len(professional|set(lower))==64
assert len(set(german['unseededProfessional']))==4 and set(german['unseededProfessional']).issubset(professional)
print('PASS: German cup 64-club field, 28 additional identities and complete opening pots')
