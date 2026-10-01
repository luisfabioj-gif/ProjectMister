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
