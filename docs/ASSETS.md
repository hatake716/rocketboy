# 素材・出典

| 素材 | 作成元・取得元 | ライセンス |
|---|---|---|
| ロゴ、ロケット、地球、星、隕石、破片、アイコン | このリポジトリのCanvas・VectorDrawableコードで作成 | MIT |
| CELESTIAL ADVENTURE / 天体別BGM全25曲 | `tools/music/scorebook.py` のオリジナル譜面、`tools/music/render.py` の4声合成 | MIT |
| ORBIT RUNNER / 訓練生BGM | `tools/generate_audio.py` のオリジナル譜面・波形合成 | MIT |
| 発射・迎撃・着弾・昇格・ゲームオーバー音 | 同スクリプトの波形合成 | MIT |
| DotGothic16-Regular.ttf | [Google Fonts配布ファイル](https://github.com/google/fonts/tree/main/ofl/dotgothic16)、[Fontworks原典](https://github.com/fontworks-fonts/DotGothic16) | SIL OFL 1.1（変更なしで同梱） |
| 天体名・分類 | NASA Solar System / 各惑星の衛星一覧を確認 | 名称・分類のみ。画像・解説文の転載なし |
| 名言228件（6カテゴリ） | [Wikipediaの固定版と引用箇所](WIKIPEDIA_QUOTES.md) | 記事テキストはCC BY-SA 4.0。記事中に引用された第三者の原著作物は個別の条件、古典原文は保護期間満了。独自解説はMIT |
| 既存の入力用の読み等の参照データ | [旧版の参照履歴](quotes-before-wikipedia.md) | 百人一首CSV：The Unlicense、ことわざ参照データ：MIT。元の利用表示を保持 |
| お題 | ゲーム用に選定した、ひらがなの単語・短いフレーズ | 外部辞書本文の転載なし |

DotGothic16の著作権表示とライセンス全文は `app/src/main/assets/licenses/DotGothic16-OFL.txt` に同梱しています。

同梱TTFのSHA-256: `3ad9af88726d42b40f7f365f0dcac785af73cf20ea6f1d5b44e57cc21150b8f1`。

既存の訓練生BGMと効果音は22050 Hz、16-bit、モノラルPCM WAV。乱数シード、テンポ、音階、長さを固定し、外部サンプルなしで再生成できます。曲名は「ORBIT RUNNER」、168 BPM、Eマイナー、32小節、約45.714秒のループです。波形は矩形波・三角波・ノイズから構成しています。既存作品の旋律の採譜・音源抽出は行っていません。

百人一首・ことわざの参照データの原ライセンスを `app/src/main/assets/licenses/Hyakunin-Unlicense.txt` と `Kotowaza-MIT.txt` に収録。アプリ設定から全文を閲覧できます。

## Wikipediaを出典とする名言（1.3.0）

全228件の出典リンクをWikipediaの確認済み版に統一。記事名・版番号・寄稿者への表示・引用箇所・表記の変更内容を各項目に付けています。
全文HTMLや記事の画像は同梱しません。独自解説と入力用の読みは、名言本文と区別しています。
[確認した引用の一覧](WIKIPEDIA_QUOTES.md) と [Wikipediaの利用表示](../app/src/main/assets/licenses/Wikipedia-Reuse.txt) を参照してください。

以前の紹介サイトからのデータは [過去の参照履歴](quotes-before-wikipedia.md) に残しています。アプリの生成元は `data/quotes/wikipedia.json` のみです。
古いデータが再び混入しないよう、生成時にWikipediaの固定版URL、採用記録、引用の照合値、人物参照を検査します。

## CELESTIAL ADVENTURE（1.3.1）

25天体のBGMをすべて新規作曲に置換。各曲3つの4小節主題を64小節に展開し、152〜194 BPMでループします。ZUN進行基本形の♭VI → ♭VII → iを土台とした和声です。矩形波2声、32サンプル4bit波形メモリ、7/15bit LFSRノイズの計4声で合成。ゲームボーイ風の構成をソフトウェアで再現したもので、実機APUの完全エミュレーションや実機からの録音ではありません。

44100 Hz・16-bitモノラルPCMからOgg Vorbis品質5に変換して同梱。訓練生「ORBIT RUNNER」と効果音5種は変更していません。既存作品の旋律・MIDI・サンプル・ROM音源は使用していません。[曲目表・和声の参考資料](SOUNDTRACK.md)、音源のハッシュ・長さ・ピーク値を記録した `data/music/tracks.json` を参照してください。譜面・合成コード・オリジナル音源はMITライセンスです。
