# 素材・出典

| 素材 | 作成元・取得元 | ライセンス |
|---|---|---|
| ロゴ、ロケット、地球、星、隕石、破片、アイコン | このリポジトリのCanvas・VectorDrawableコードで作成 | MIT |
| ORBIT RUNNER / BGM | `tools/generate_audio.py` のオリジナル譜面・波形合成 | MIT |
| 発射・迎撃・着弾・昇格・ゲームオーバー音 | 同スクリプトの波形合成 | MIT |
| DotGothic16-Regular.ttf | [Google Fonts配布ファイル](https://github.com/google/fonts/tree/main/ofl/dotgothic16)、[Fontworks原典](https://github.com/fontworks-fonts/DotGothic16) | SIL OFL 1.1（変更なしで同梱） |
| 天体名・分類 | NASA Solar System / 各惑星の衛星一覧を確認 | 名称・分類のみ。画像・解説文の転載なし |
| お題 | ゲーム用に選定した、ひらがなの単語・短いフレーズ | 外部辞書本文の転載なし |

DotGothic16の著作権表示とライセンス全文は `app/src/main/assets/licenses/DotGothic16-OFL.txt` に同梱しています。

同梱TTFのSHA-256: `3ad9af88726d42b40f7f365f0dcac785af73cf20ea6f1d5b44e57cc21150b8f1`。

音源は22050 Hz、16-bit、モノラルPCM WAV。乱数シード、テンポ、音階、長さを固定し、外部サンプルなしで再生成できます。曲名は「ORBIT RUNNER」、168 BPM、Eマイナー、32小節、約45.714秒のループです。波形は矩形波・三角波・ノイズから構成しています。既存作品の旋律の採譜・音源抽出は行っていません。
