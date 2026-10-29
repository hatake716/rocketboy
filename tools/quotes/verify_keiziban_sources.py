#!/usr/bin/env python3
"""Verify selected short excerpts against their attributed blocks in source HTML.

Use cached HTML from the selection date for reproducible SHA-256 checks. --fetch
downloads current pages into that cache and reports if the site has changed.
This checks the introduction site's text, not the original work or statement.
"""
import argparse
import hashlib
from html import unescape
import json
from pathlib import Path
import re
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]


def plain(html):
    return unescape(re.sub(r"<[^>]+>", "", re.sub(r"<br\s*/?>", "\n", html))).strip()


def verify(directory, fetch=False):
    catalog = json.loads((ROOT / "data/quotes/keiziban.json").read_text())
    provenance = json.loads((ROOT / "data/quotes/keiziban-provenance.json").read_text())
    quotes = {quote["id"]: quote for quote in catalog["quotes"]}
    entries = provenance["entries"]
    assert len(quotes) == len(entries) == len({entry["quoteId"] for entry in entries})
    assert len({entry["sourceArticleUrl"] for entry in entries}) == len(entries)
    directory.mkdir(parents=True, exist_ok=True)
    for entry in entries:
        quote = quotes[entry["quoteId"]]
        article_url = entry["sourceArticleUrl"]
        parsed_url = urlsplit(article_url)
        assert parsed_url.scheme == "https" and parsed_url.netloc == "meigen.keiziban-jp.com"
        path = directory / (parsed_url.path.strip("/") + ".html")
        if fetch:
            with urlopen(Request(article_url, headers={"User-Agent": "ROCKETBOY quote source verifier"}), timeout=30) as response:
                path.write_bytes(response.read())
        raw = path.read_bytes()
        assert hashlib.sha256(raw).hexdigest() == entry["sourceHtmlSha256"], f"Source HTML changed: {article_url}"
        parts = re.split(r'<div class="hreview" id="hreview-(\d+)">', raw.decode("utf-8", errors="replace"))
        blocks = dict(zip(parts[1::2], parts[2::2]))
        block = blocks[entry["sourceReviewId"]]
        attribution = plain(re.search(r"<h4[^>]*>(.*?)</h4>", block, re.S)[1])
        original = plain(re.search(r'<div class="description">(.*?)</div>', block, re.S)[1])
        assert "\ufffd" not in quote["text"] and quote["text"] in original, quote["id"]
        assert attribution == entry["sourceAttribution"], quote["id"]
        assert quote["sourceUrl"] == entry["sourceUrl"] == article_url + "#hreview-" + entry["sourceReviewId"]
        assert entry["partialExcerpt"] == (quote["text"] != original), quote["id"]
        assert entry["partialExcerpt"] == (quote["wording"] == "紹介サイトの一部抜粋"), quote["id"]
        assert len(quote["text"]) <= 35, quote["id"]
    return {"entries": len(entries), "article_hashes_matched": True,
            "attributed_excerpts_matched": True, "original_sources_verified": False}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html-dir", type=Path, required=True)
    parser.add_argument("--fetch", action="store_true", help="Fetch current pages; do not update provenance hashes")
    args = parser.parse_args()
    print(json.dumps(verify(args.html_dir, args.fetch), indent=2))
