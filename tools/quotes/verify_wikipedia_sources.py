#!/usr/bin/env python3
"""Recheck retained quotes against pinned Wikipedia revisions. Requires beautifulsoup4.
Usage: python3 tools/quotes/verify_wikipedia_sources.py [--refresh]
Only reads Wikipedia; writes a local verification report and a request cache.
"""
import argparse
import concurrent.futures
import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path
from bs4 import BeautifulSoup, Comment, NavigableString
from build_catalog import normalize

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'artifacts/wikipedia-audit/verified-revisions'
CACHE.mkdir(parents=True,exist_ok=True)

def fetch(revision,refresh=False):
    file=CACHE/f'{revision}.json'
    if file.exists() and not refresh:return json.loads(file.read_text())
    params=urllib.parse.urlencode(dict(action='parse',oldid=revision,prop='text|revid|displaytitle',
                                     format='json',formatversion=2,maxlag=5))
    for attempt in range(3):
        try:
            req=urllib.request.Request('https://ja.wikipedia.org/w/api.php?'+params,
                headers={'User-Agent':'RocketboyQuoteAudit/1.0 (https://github.com/hatake716/rocketboy)'})
            with urllib.request.urlopen(req,timeout=30) as response:data=json.load(response)
            assert 'error' not in data, data.get('error')
            parsed=data['parse'];assert parsed['revid']==revision
            file.write_text(json.dumps(parsed,ensure_ascii=False))
            time.sleep(.15)
            return parsed
        except Exception:
            if attempt==2:raise
            time.sleep(attempt+1)

def article_text(html):
    soup=BeautifulSoup(html,'html.parser')
    for node in soup.select('script,style,.reference,.mw-editsection,.navbox,.metadata,.infobox,rt,rp'):
        node.decompose()
    # A bibliography title, external-link label or dictionary redirect is not quote evidence.
    assert '百科事典記事はありません' not in soup.get_text(), 'Dictionary-only redirect'
    parts=[]
    for node in soup.descendants:
        if getattr(node, 'name', None) in ('h2','h3') and node.get_text(strip=True) in (
            '脚注','出典','参考文献','関連項目','外部リンク',
        ):
            break
        if isinstance(node, NavigableString) and not isinstance(node, Comment):
            parts.append(str(node).strip())
    text=''.join(parts)
    # Furigana displayed parenthetically is an annotation, not part of the poem.
    return re.sub(r'[（(][ぁ-んァ-ヶー]+[）)]','',text)

def verify(refresh=False):
    audit=json.loads((ROOT/'data/quotes/wikipedia-audit.json').read_text())
    revisions=sorted({e['revision'] for e in audit['retained']})
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        pages=dict(zip(revisions,pool.map(lambda r:fetch(r,refresh),revisions)))
    texts={rev:normalize(article_text(p['text'])) for rev,p in pages.items()}
    results=[]
    for evidence in audit['retained']:
        rev=evidence['revision'];page=pages[rev]
        assert evidence['page_title']==page['title'], evidence['id']
        assert normalize(evidence['quoted_text']) in texts[rev], evidence['id']
        assert evidence['attribution_checked'] is True, evidence['id']
        results.append(dict(id=evidence['id'],revision=rev,quote_present=True,
                            page_html_sha256=hashlib.sha256(page['text'].encode()).hexdigest()))
    report=dict(checked_date='2026-09-09',quotes=len(results),pages=len(pages),failures=0,results=results)
    (ROOT/'data/quotes/wikipedia-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return report

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--refresh',action='store_true');args=p.parse_args()
    report=verify(args.refresh)
    print(json.dumps({k:v for k,v in report.items() if k!='results'},ensure_ascii=False,indent=2))
