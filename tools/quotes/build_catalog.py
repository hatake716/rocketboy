#!/usr/bin/env python3
"""Build the Wikipedia-verified offline catalog; reject any other quote source."""
import argparse
import hashlib
import json
import re
import unicodedata
from pathlib import Path
from urllib.parse import parse_qs, urlparse

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / 'data/quotes'
OUT = ROOT / 'app/src/main/resources/quotes'
TARGET = 3000
CATEGORIES = ('history', 'business', 'fiction', 'game', 'proverb', 'hyakunin')

def normalize(text):
    return re.sub(r'[^\wぁ-んァ-ヶ一-龠々ー]', '', unicodedata.normalize('NFKC', text)).lower()

def build():
    catalog = json.loads((DATA / 'wikipedia.json').read_text())
    audit = json.loads((DATA / 'wikipedia-audit.json').read_text())
    quotes, authors = catalog['quotes'], catalog['authors']
    assert quotes, 'The game needs at least one verified quote'
    assert len({q['id'] for q in quotes}) == len(quotes), 'Duplicate quote ID'
    assert len({q['reading'] for q in quotes}) == len(quotes), 'Duplicate reading'
    assert len({normalize(q['text']) for q in quotes}) == len(quotes), 'Duplicate text'
    assert len({a['id'] for a in authors}) == len(authors), 'Duplicate author ID'
    assert {q['authorId'] for q in quotes} == {a['id'] for a in authors}, 'Missing or unused author'
    evidence = {q['id']:q for q in audit['retained']}
    assert set(evidence) == {q['id'] for q in quotes}, 'Every included quote needs evidence'
    removed = audit['removed']
    assert len({q['id'] for q in removed}) == len(removed)
    assert not set(evidence).intersection(q['id'] for q in removed)
    added = audit.get('added_ids', [])
    assert len(added) == len(set(added)) and set(added).issubset(evidence)
    assert len(quotes) + len(removed) == audit['previous_count'] + len(added)
    for q in quotes:
        assert q['category'] in CATEGORIES, q['id']
        assert re.fullmatch(r'[ぁ-んー]{1,64}', q['reading']), q['id']
        assert len(q['explanation']) >= 30 and q['wording'] and q['license'], q['id']
        uri = urlparse(q['sourceUrl'])
        assert uri.scheme == 'https' and re.fullmatch(r'[a-z-]+\.wikipedia\.org', uri.hostname or ''), q['id']
        e = evidence[q['id']]
        assert e['attribution_checked'] is True, q['id']
        assert q['sourceUrl'] == e['url'], q['id']
        assert parse_qs(uri.query).get('oldid') == [str(e['revision'])], q['id']
        assert normalize(q['text']) == normalize(e['quoted_text']), q['id']
        assert hashlib.sha256(e['quoted_text'].encode()).hexdigest() == e['quote_sha256'], q['id']
        assert 'Wikipedia' in q['source'] and 'Wikipedia' in q['license'], q['id']
    OUT.mkdir(parents=True, exist_ok=True)
    def tsv(name, fields, rows):
        body = '# ' + '\t'.join(fields) + '\n'
        for row in rows:
            values = [str(row.get(k, '')) for k in fields]
            assert all('\t' not in v and '\\' not in v for v in values)
            body += '\t'.join(v.replace('\n', '\\n') for v in values) + '\n'
        (OUT / (name + '.tsv')).write_text(body, encoding='utf-8')
    for a in authors:
        a['type'] = 'work' if a.get('isWork') or a.get('type') == 'work' else 'person'
    tsv('authors', ('id', 'name', 'period', 'biography', 'type'), authors)
    tsv('catalog', ('id','text','reading','authorId','explanation','source','sourceUrl','category','wording','license'), quotes)
    revised = audit.get('revised_readings', [])
    for item in revised:
        assert item['id'] in evidence and item['reading'] != next(q['reading'] for q in quotes if q['id'] == item['id'])
    tsv('retired', ('id','reading'), removed + revised)
    counts = {c:sum(q['category'] == c for q in quotes) for c in CATEGORIES}
    total = len(quotes)
    report = dict(included_total=total, categories=counts, hyakunin_count=counts['hyakunin'],
                  source_policy='Wikipedia本文と帰属を確認できる名言のみ', source_review_complete=True,
                  previous_count=audit['previous_count'], removed_count=len(removed),
                  added_count=len(added), exhaustive_wikipedia_coverage=False,
                  historical_requested_total=TARGET,
                  target_note='Wikipedia本文と帰属を個別確認して追加する。未確認の言葉で3000件に水増しせず、全Wikipediaの網羅は主張しない。')
    (DATA / 'inventory.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    return report

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--require-target', action='store_true')
    args = parser.parse_args()
    report = build()
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if args.require_target and report['included_total'] != TARGET:
        raise SystemExit(f"Historical target {TARGET}; found {report['included_total']} verified entries.")
