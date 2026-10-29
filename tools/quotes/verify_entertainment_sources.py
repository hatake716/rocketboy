#!/usr/bin/env python3
"""Check selected short quotes inside the specified numbered article items."""
import argparse
import hashlib
from html import unescape
import json
from pathlib import Path
import re
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]


def verify(directory, fetch=False):
    data = json.loads((ROOT / "data/quotes/entertainment.json").read_text())
    provenance = json.loads((ROOT / "data/quotes/entertainment-provenance.json").read_text())
    quotes = {q["id"]: q for q in data["quotes"]}
    entries = provenance["entries"]
    assert len(quotes) == len(entries) == len({p["quoteId"] for p in entries})
    pages = {}
    directory.mkdir(parents=True, exist_ok=True)
    for url in data["selectionUrls"]:
        article = re.fullmatch(r"https://entertainment3150.com/archives/(\d+)\.html", url)[1]
        path = directory / (article + ".html")
        if fetch:
            with urlopen(Request(url, headers={"User-Agent": "ROCKETBOY quote verifier"}), timeout=30) as response:
                path.write_bytes(response.read())
        raw = path.read_bytes()
        body = raw.decode("utf-8").split('<div class="article-body-inner">', 1)[1].split("<!-- articleBody End -->", 1)[0]
        body = unescape(re.sub(r"<[^>]+>", "", re.sub(r"<br\s*/?>", "\n", body)))
        pages[url] = (hashlib.sha256(raw).hexdigest(), body)
    for entry in entries:
        q = quotes[entry["quoteId"]]
        digest, body = pages[q["sourceUrl"]]
        assert q["sourceUrl"] == entry["sourceUrl"]
        assert digest == entry["sourceHtmlSha256"], f"Article changed: {q['sourceUrl']}"
        marker = chr(ord("①") + entry["articleItem"] - 1)
        block = re.split("[①-⑳]", body.split(marker, 1)[1], maxsplit=1)[0]
        assert q["text"] in block, q["id"]
        assert entry["partialExcerpt"] == ("一部抜粋" in q["wording"]), q["id"]
        assert entry["kind"] in q["wording"], q["id"]
    for url in pages:
        selected = [q for q in quotes.values() if q["sourceUrl"] == url]
        assert len(selected) == 3
        # A conservative editorial size limit; this is not a copyright license.
        assert sum(len(q["text"]) for q in selected) <= 35
    return dict(entries=len(entries), articles=len(pages), article_hashes_matched=True,
                numbered_items_matched=True, original_sources_verified=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--html-dir", type=Path, required=True)
    parser.add_argument("--fetch", action="store_true")
    args = parser.parse_args()
    print(json.dumps(verify(args.html_dir, args.fetch), indent=2))
