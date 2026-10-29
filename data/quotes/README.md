# 名言データ

現在の入力は **wikipedia.jsonのみ** です。`tools/quotes/build_catalog.py` でアプリのTSVを生成します。
`wikipedia-audit.json` が採否と出典の記録、`wikipedia-verification.json` が固定版の本文照合結果です。
`inventory.json` は実数を生成したものです。

その他のJSON・CSV・TSV・TXTは1.2.0以前の追加履歴と読み・解説の編集履歴です。現在のアプリの収録対象としては使用しません。
削除対象の旧IDと読みのみ、保存ミッションの移行のため生成される `retired.tsv` に残します。
[現在の出典一覧と方針](../../docs/WIKIPEDIA_QUOTES.md)。
