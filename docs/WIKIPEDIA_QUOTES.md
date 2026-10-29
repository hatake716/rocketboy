# Wikipediaを出典とする名言（1.3.0）

確認日：2026-09-09。1.2.1の172件に56件を追加し、228件を収録しました。以前の削除189件は引き続き未収録です。

| カテゴリ | 収録数 |
|---|---:|
| 歴史上の人物 | 37 |
| 企業経営者 | 14 |
| アニメ・漫画作品 | 18 |
| ゲームの名言 | 22 |
| ことわざ | 37 |
| 百人一首 | 100 |
| **合計** | **228** |

## 今回の調査範囲

日本語版Wikipedia内の12検索（各最大40件・重複を除き423記事）と既存候補記事、10人物・作品記事を追加確認。全Wikipedia・全人物・全作品の網羅を意味しない。
検索語・記事一覧・追加56件の固定版と帰属メモは [調査記録](../data/quotes/wikipedia-expansion.json) に保存しています。3,000件や全Wikipedia収録という表示は行いません。歴史上の人物のカテゴリには思想家・科学者・芸術家などを含みます。

## 確認と表記の基準

日本語版Wikipediaの人物・作品・ことわざの記事と、名言文字列の検索結果から本文を確認しました。人物や作品の記事があるだけでは採用せず、言葉が掲載され、その人物・作品の言葉として説明されているか確認しています。
同じ文字列でも番組名・曲名・書名・外部リンクの題名だけのもの、別の人物の言葉や偶然の部分一致、ウィクショナリーへの案内しかないページは根拠にしていません。
削除はこの調査で確認できなかったという判断であり、Wikipedia全体に別訳や別表記も存在しないと断定するものではありません。

全項目に記事の固定版URL（oldid）、記事名、引用箇所、帰属の確認メモを保存しています。
本文の照合では空白・句読点等を正規化。和歌のルビは取り除き、改行を調整しました。独自の短い解説は引き続き各項目に付けています。
百人一首44番は「藤原朝忠」記事、残り99首は「百人一首」記事の一覧を参照。49番の読みは掲載本文「夜はもえて」に合わせて「て」を補いました。

Wikipediaの記事テキストの利用条件はCC BY-SA 4.0です。Wikipediaの記事に引用された元の作品や台詞に、この許諾が自動的に及ぶわけではありません。古典和歌の原文は保護期間満了です。
[同梱の利用表示](../app/src/main/assets/licenses/Wikipedia-Reuse.txt)、[Wikipediaの二次利用案内](https://ja.wikipedia.org/wiki/Wikipedia:ウィキペディアを二次利用する)、[Wikimedia利用規約](https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use/ja) を参照してください。

## 更新時の保存データ

採用した名言のIDを維持します。削除した名言は収集済み一覧にも出現候補にも表示しません。
保存中のミッションに削除名言が飛行していた場合は、その隕石だけを除去し、スコア・最高記録・シールド・経過時間・他の隕石・設定を維持します。削除による加点・減点・着弾ダメージはありません。
百人一首49番は収集記録を維持し、古い読みの飛行中の隕石だけを除去します。新しい読みでは通常どおり再出現します。
削除した名言の旧IDと読みだけを `retired.tsv` に保存し、移行判定に使用します。本文・解説・出典は削除対象としてAPKに収録しません。

## 編集元と再検証

- `data/quotes/wikipedia.json`：アプリで使用する唯一の名言編集元。
- `data/quotes/wikipedia-audit.json`：採用228件の固定版・帰属メモ・引用SHA-256と、削除189件のID・理由・調査候補。
- `data/quotes/wikipedia-verification.json`：固定版を取得し直した本文照合の結果。
- `app/src/main/resources/quotes/`：生成される本文・人物・移行用ID/読み。
- 他のJSON・TSV・CSVは旧版の追加履歴であり、生成スクリプトは読みません。

```bash
python3 tools/quotes/build_catalog.py
# Python環境にbeautifulsoup4が必要。固定版のWikipedia記事を読み取り、結果を保存。
python3 tools/quotes/verify_wikipedia_sources.py --refresh
```

前者は重複、人物参照、読みの上限、Wikipediaの固定版URL、採用記録との整合を検査します。
後者は固定版を再取得し、採用した本文の掲載を再照合します。発言者・作品の帰属と文脈の判断は、採用記録の確認メモを併せてレビューしてください。

## 採用した引用と出典

### 歴史上の人物

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| descartes-think | 我思う、ゆえに我あり。 | ルネ・デカルト | [我思う、ゆえに我あり・110152782](https://ja.wikipedia.org/w/index.php?oldid=110152782) |
| pascal-reed | 人間は考える葦である。 | ブレーズ・パスカル | [ブレーズ・パスカル・110758059](https://ja.wikipedia.org/w/index.php?oldid=110758059) |
| confucius-learning | 学びて時に之を習う。 | 孔子 | [論語・110804704](https://ja.wikipedia.org/w/index.php?oldid=110804704) |
| tomo8-confucius | 義を見てせざるは勇無きなり | 孔子 | [義を見てせざるは勇無きなり・109221142](https://ja.wikipedia.org/w/index.php?oldid=109221142) |
| tomo8-nietzsche | 脱皮できない蛇は滅びる。 | フリードリヒ・ニーチェ | [脱皮・110707428](https://ja.wikipedia.org/w/index.php?oldid=110707428) |
| tomo8-pythagoras | 万物は数なり | ピタゴラス | [ピタゴラス・110579720](https://ja.wikipedia.org/w/index.php?oldid=110579720) |
| tomo8-hippocrates | 人生は短く、技術は長い | ヒポクラテス | [アルスロンガ、ウィータブレウィス・110396157](https://ja.wikipedia.org/w/index.php?oldid=110396157) |
| tomo8-martin-luther | 希望は、強い勇気であり、新たな意志である。 | マルティン・ルター | [マルティン・ルター・107866553](https://ja.wikipedia.org/w/index.php?oldid=107866553) |
| tomo8-lincoln | 人民の、人民による、人民のための政治 | エイブラハム・リンカーン | [エイブラハム・リンカーン・109960955](https://ja.wikipedia.org/w/index.php?oldid=109960955) |
| tomo8-louis-pasteur | 幸運は用意された心のみに宿る。 | ルイ・パスツール | [幸運は用意された心のみに宿る・107944450](https://ja.wikipedia.org/w/index.php?oldid=107944450) |
| keiziban-89407 | アイデアの秘訣は、執念である | 湯川秀樹 | [SKE48のおやすみ名言道場・107817888](https://ja.wikipedia.org/w/index.php?oldid=107817888) |
| keiziban-3101 | 今日も生涯の一日なり | 福沢諭吉 | [久恒啓一・110227840](https://ja.wikipedia.org/w/index.php?oldid=110227840) |
| keiziban-96946 | 積小為大 | 二宮尊徳 | [小田原市立桜井小学校・101672366](https://ja.wikipedia.org/w/index.php?oldid=101672366) |
| keiziban-2535 | 教えに来たのではありません。習いに来たのです | 野口英世 | [野口英世・110795852](https://ja.wikipedia.org/w/index.php?oldid=110795852) |
| keiziban-114908 | 雪は天から送られた手紙である | 中谷宇吉郎 | [中谷宇吉郎・110143758](https://ja.wikipedia.org/w/index.php?oldid=110143758) |
| keiziban-2542 | 是非に及ばず | 織田信長 | [是非に及ばず・110356399](https://ja.wikipedia.org/w/index.php?oldid=110356399) |
| keiziban-98693 | 私に頼ってくる人々を見捨てるわけにはいかない。 | 杉原千畝 | [杉原千畝・110546080](https://ja.wikipedia.org/w/index.php?oldid=110546080) |
| wiki-f5dfdc88efac | 腐敗した社会には、多くの法律がある。 | サミュエル・ジョンソン | [サミュエル・ジョンソン・105496178](https://ja.wikipedia.org/w/index.php?oldid=105496178) |
| wiki-94133c747f8f | 過ぎ行く時を捉えよ。時々刻々を善用せよ。 | サミュエル・ジョンソン | [サミュエル・ジョンソン・105496178](https://ja.wikipedia.org/w/index.php?oldid=105496178) |
| wiki-ea295044dc46 | 信頼なくして友情はない、誠実さなくして信頼はない。 | サミュエル・ジョンソン | [サミュエル・ジョンソン・105496178](https://ja.wikipedia.org/w/index.php?oldid=105496178) |
| wiki-dad63bc87db2 | 芝居の規則はお客様が決める。 | サミュエル・ジョンソン | [サミュエル・ジョンソン・105496178](https://ja.wikipedia.org/w/index.php?oldid=105496178) |
| wiki-f37c4c0bb19b | 無意識は一つの言語として構造化されている。 | ジャック・ラカン | [ジャック・ラカン・110375041](https://ja.wikipedia.org/w/index.php?oldid=110375041) |
| wiki-1d1a722df867 | 人間の欲望は他者の欲望である。 | ジャック・ラカン | [ジャック・ラカン・110375041](https://ja.wikipedia.org/w/index.php?oldid=110375041) |
| wiki-640355c10642 | 愛とは自分のもっていないものを与えることである。 | ジャック・ラカン | [ジャック・ラカン・110375041](https://ja.wikipedia.org/w/index.php?oldid=110375041) |
| wiki-f19f90ea7fdf | 志を立てて以って万事の源となす | 吉田松陰 | [吉田松陰・110587672](https://ja.wikipedia.org/w/index.php?oldid=110587672) |
| wiki-888d807e1901 | 己に真の志あれば、無志はおのずから引き去る | 吉田松陰 | [吉田松陰・110587672](https://ja.wikipedia.org/w/index.php?oldid=110587672) |
| wiki-301d40c95c3e | この世界に平和を創造するためには、一人の人間との理解を深めることから始めなければなりません。 | エレノア・ルーズベルト | [エレノア・ルーズベルト・110926076](https://ja.wikipedia.org/w/index.php?oldid=110926076) |
| wiki-d20970892e5b | 一つが切り抜けられたら、次には何でも切り抜けられるはずではないか。 | エレノア・ルーズベルト | [エレノア・ルーズベルト・110926076](https://ja.wikipedia.org/w/index.php?oldid=110926076) |
| wiki-44c05e1ede3c | 労働者が仕事をするように、芸術家も仕事をするべきだ | パブロ・ピカソ | [パブロ・ピカソ・110939161](https://ja.wikipedia.org/w/index.php?oldid=110939161) |
| wiki-186dac0a6949 | 誰でも子供のときは芸術家であるが、問題は大人になっても芸術家でいられるかどうかである | パブロ・ピカソ | [パブロ・ピカソ・110939161](https://ja.wikipedia.org/w/index.php?oldid=110939161) |
| wiki-283090703015 | 努力だ、勉強だ、それが天才だ。 | 野口英世 | [野口英世・110795852](https://ja.wikipedia.org/w/index.php?oldid=110795852) |
| wiki-513083fc31df | 私はこの世界に、何事かをなさんがために生まれてきた。 | 野口英世 | [野口英世・110795852](https://ja.wikipedia.org/w/index.php?oldid=110795852) |
| wiki-65c0390e54ff | 正直であることが最高の手段だ。 | 野口英世 | [野口英世・110795852](https://ja.wikipedia.org/w/index.php?oldid=110795852) |
| wiki-07f15d52af37 | あらゆる現実は歴史的な過程だ。 | ヘーゲル | [ゲオルク・ヴィルヘルム・フリードリヒ・ヘーゲル・110891025](https://ja.wikipedia.org/w/index.php?oldid=110891025) |
| wiki-e575be54a020 | 歴史を学ぶと、我々が歴史から学んでいないことが分かる。 | ヘーゲル | [ゲオルク・ヴィルヘルム・フリードリヒ・ヘーゲル・110891025](https://ja.wikipedia.org/w/index.php?oldid=110891025) |
| wiki-be2bf6ebc5cb | 賢さの次には勇気が私たちにとっての甚だ大切な特質である。 | ショーペンハウアー | [アルトゥル・ショーペンハウアー・110948708](https://ja.wikipedia.org/w/index.php?oldid=110948708) |
| wiki-843e6b4de048 | 大したことをしたわけではない。当然の事をしただけです | 杉原千畝 | [杉原千畝・110546080](https://ja.wikipedia.org/w/index.php?oldid=110546080) |

### 企業経営者

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| toyoda-research | 研究と創造に心を致し、常に時流に先んずべし | 豊田佐吉の遺訓・豊田綱領 | [トヨタ自動車・110910544](https://ja.wikipedia.org/w/index.php?oldid=110910544) |
| keiziban-44739 | 乗客は電車が創造する | 小林一三 | [小林一三・110438182](https://ja.wikipedia.org/w/index.php?oldid=110438182) |
| wiki-1ffd2f14355b | 裕福な人はその富を浪費するよりも、社会がより豊かになるために使うべきだ。 | アンドリュー・カーネギー | [アンドリュー・カーネギー・109335274](https://ja.wikipedia.org/w/index.php?oldid=109335274) |
| wiki-0c3feb733bf7 | 大人はかつての記憶を懐かしみ、若者は未来への挑戦と約束を味わうでしょう | ウォルト・ディズニー | [ウォルト・ディズニー・110511059](https://ja.wikipedia.org/w/index.php?oldid=110511059) |
| wiki-7244f7a0014b | いつでも掃除が行き届いていて、おいしいものが食べられる。そんな夢の世界を作りたい | ウォルト・ディズニー | [ウォルト・ディズニー・110511059](https://ja.wikipedia.org/w/index.php?oldid=110511059) |
| wiki-aa0a601d0961 | 奉仕を主とする事業は栄え、利得を主とする事業は衰える | ヘンリー・フォード | [ヘンリー・フォード・109758565](https://ja.wikipedia.org/w/index.php?oldid=109758565) |
| wiki-682267907687 | 芸術に中断は無い | 出光佐三 | [出光佐三・108825287](https://ja.wikipedia.org/w/index.php?oldid=108825287) |
| wiki-4cc5a5758a11 | 小さい会社を作って、またいろいろチャレンジしたいね | 井深大 | [井深大・110882374](https://ja.wikipedia.org/w/index.php?oldid=110882374) |
| wiki-8ecca9d84dcb | これは俺がもらったんじゃねえ。お前さんと二人でもらったんだ。これは二人のものだ | 本田宗一郎 | [本田宗一郎・109552308](https://ja.wikipedia.org/w/index.php?oldid=109552308) |
| wiki-a0289b721cac | 会社は個人の持ち物ではない | 本田宗一郎 | [本田宗一郎・109552308](https://ja.wikipedia.org/w/index.php?oldid=109552308) |
| wiki-5bcdcec4af31 | 落とし穴は、賛辞の中にある | 安藤百福 | [安藤百福・110468914](https://ja.wikipedia.org/w/index.php?oldid=110468914) |
| wiki-eb879fd2cec5 | 分かっているのに実行しないならば、分かっていない事と同じだ。 | 土光敏夫 | [土光敏夫・109114690](https://ja.wikipedia.org/w/index.php?oldid=109114690) |
| wiki-c938c770ac0d | テクノロジーの進化だけで、人の心の満足につながるのだろうか。 | 土光敏夫 | [土光敏夫・109114690](https://ja.wikipedia.org/w/index.php?oldid=109114690) |
| wiki-d4c50a10c63c | 人間には、人間にしかできない仕事を与えることが大切だ。 | 土光敏夫 | [土光敏夫・109114690](https://ja.wikipedia.org/w/index.php?oldid=109114690) |

### アニメ・漫画作品

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| animate-rei-protect | あなたは死なないわ。私が守るもの | 綾波レイ | [発表!全エヴァンゲリオン大投票・107369086](https://ja.wikipedia.org/w/index.php?oldid=107369086) |
| animate-allmight-hero | 君はヒーローになれる | オールマイト | [僕のヒーローアカデミア・110750823](https://ja.wikipedia.org/w/index.php?oldid=110750823) |
| animate-quattro-sponsor | 出資者は無理難題をおっしゃる | クワトロ・バジーナ | [ウォン・リー・109004045](https://ja.wikipedia.org/w/index.php?oldid=109004045) |
| animate-light-god | 新世界の神になる | 夜神月 | [夜神月・110016468](https://ja.wikipedia.org/w/index.php?oldid=110016468) |
| animate-raoh-life | わが生涯に一片の悔いなし!! | ラオウ | [ラオウ・109244673](https://ja.wikipedia.org/w/index.php?oldid=109244673) |
| animate-lockon-aim | 狙い撃つぜ！ | ロックオン・ストラトス | [機動戦士ガンダム00の登場人物・109918868](https://ja.wikipedia.org/w/index.php?oldid=109918868) |
| animate-sisters-dream | 夢だけど 夢じゃなかった！ | 草壁サツキ・草壁メイ | [となりのトトロ・110930844](https://ja.wikipedia.org/w/index.php?oldid=110930844) |
| animate-conan-truth | 真実はいつもひとつ | 江戸川コナン | [江戸川コナン・110939584](https://ja.wikipedia.org/w/index.php?oldid=110939584) |
| keiziban-144952 | ３分間待ってやる | ムスカ | [ムスカ・110509869](https://ja.wikipedia.org/w/index.php?oldid=110509869) |
| wiki-ad0d63ebb914 | 犯人はお前だ！ | 工藤新一『名探偵コナン』 | [江戸川コナン・110939584](https://ja.wikipedia.org/w/index.php?oldid=110939584) |
| wiki-c0806a36990e | 飛ばねぇ豚はただの豚だ | ポルコ・ロッソ『紅の豚』 | [紅の豚・110915718](https://ja.wikipedia.org/w/index.php?oldid=110915718) |
| wiki-ddb215d50342 | お前がナンバーワンだ | ベジータ『ドラゴンボール』 | [ベジータ・110934869](https://ja.wikipedia.org/w/index.php?oldid=110934869) |
| wiki-2eceda7b306a | 連邦のモビルスーツは化け物か！？ | シャア・アズナブル『機動戦士ガンダム』 | [シャア・アズナブル・110939330](https://ja.wikipedia.org/w/index.php?oldid=110939330) |
| wiki-f7c8eac94e58 | これでいいのだ！ | バカボンのパパ『天才バカボン』 | [天才バカボン・110807695](https://ja.wikipedia.org/w/index.php?oldid=110807695) |
| wiki-880122c65f2c | だが断る | 岸辺露伴『ジョジョの奇妙な冒険』 | [岸辺露伴・110747570](https://ja.wikipedia.org/w/index.php?oldid=110747570) |
| wiki-25824ee74beb | やれやれだぜ | 空条承太郎『ジョジョの奇妙な冒険』 | [空条承太郎・110860929](https://ja.wikipedia.org/w/index.php?oldid=110860929) |
| wiki-42fc8933f3b0 | 月に代わって、おしおきよ！ | 月野うさぎ『美少女戦士セーラームーン』 | [月野うさぎ・109934203](https://ja.wikipedia.org/w/index.php?oldid=109934203) |
| wiki-623729715789 | お前はもう死んでいる | ケンシロウ『北斗の拳』 | [北斗の拳・110688818](https://ja.wikipedia.org/w/index.php?oldid=110688818) |

### ゲームの名言

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| keiziban-43940 | 勇気は夢を叶える魔法 | リヒター・アーベント | [テイルズ オブ シンフォニア -ラタトスクの騎士-・110289897](https://ja.wikipedia.org/w/index.php?oldid=110289897) |
| keiziban-68327 | いいセンスだ。 | リボルバー・オセロット | [リボルバー・オセロット・109283126](https://ja.wikipedia.org/w/index.php?oldid=109283126) |
| entame-18305432-07 | 異議あり！ | 法廷での決め台詞 | [逆転裁判・110593494](https://ja.wikipedia.org/w/index.php?oldid=110593494) |
| entame-18517656-01 | 大丈夫だ、問題ない | イーノック | [エルシャダイ・110360330](https://ja.wikipedia.org/w/index.php?oldid=110360330) |
| entame-18517656-02 | それは違うぞ！ | 日向創 | [スーパーダンガンロンパ2 さよなら絶望学園・110816166](https://ja.wikipedia.org/w/index.php?oldid=110816166) |
| entame-18963855-13 | ばーいくまー！ | モノクマーズ | [ニューダンガンロンパV3 みんなのコロシアイ新学期・110659760](https://ja.wikipedia.org/w/index.php?oldid=110659760) |
| entame-19460221-13 | その目だれの目？ | 作品中のフレーズ | [CHAOS;HEAD・110786087](https://ja.wikipedia.org/w/index.php?oldid=110786087) |
| wiki-48f5a8d294cb | そんな装備で大丈夫か？ | ルシフェル『エルシャダイ』 | [エルシャダイ・110360330](https://ja.wikipedia.org/w/index.php?oldid=110360330) |
| wiki-5a32cc34edc3 | 一番いいのを頼む | イーノック『エルシャダイ』 | [エルシャダイ・110360330](https://ja.wikipedia.org/w/index.php?oldid=110360330) |
| wiki-d19b52639d3f | 誰かを助けるのに理由がいるかい？ | ジタン『ファイナルファンタジーIX』 | [ファイナルファンタジーIX・110245341](https://ja.wikipedia.org/w/index.php?oldid=110245341) |
| wiki-4f2bd9654ff0 | 思い出などなくてもまた作り直せば良い | フライヤ『ファイナルファンタジーIX』 | [ファイナルファンタジーIX・110245341](https://ja.wikipedia.org/w/index.php?oldid=110245341) |
| wiki-e45c6b16feb2 | あんたの息子は俺が守る。死んでも守ってやる | アーロン『ファイナルファンタジーX』 | [ファイナルファンタジーX・110694772](https://ja.wikipedia.org/w/index.php?oldid=110694772) |
| wiki-18d0c439e31d | ガードの人数は信頼できる人の数と同じ | ユウナ『ファイナルファンタジーX』 | [ファイナルファンタジーX・110694772](https://ja.wikipedia.org/w/index.php?oldid=110694772) |
| wiki-205a1e18696a | それは違うよ！ | 苗木誠『ダンガンロンパ』 | [ダンガンロンパ 希望の学園と絶望の高校生・110786176](https://ja.wikipedia.org/w/index.php?oldid=110786176) |
| wiki-fbf5f3364979 | これで証明するよ！ | 苗木誠『ダンガンロンパ』 | [ダンガンロンパ 希望の学園と絶望の高校生・110786176](https://ja.wikipedia.org/w/index.php?oldid=110786176) |
| wiki-e41f59cea608 | 学生は勉強のプロであるべき！ | 石丸清多夏『ダンガンロンパ』 | [ダンガンロンパ 希望の学園と絶望の高校生・110786176](https://ja.wikipedia.org/w/index.php?oldid=110786176) |
| wiki-0480b4887bbb | 上手に焼けました〜 | 『モンスターハンター』 | [モンスターハンター・109760458](https://ja.wikipedia.org/w/index.php?oldid=109760458) |
| wiki-46016c42870c | お休みの間、悪魔に肉体を乗っ取られぬようにお気をつけて…… | 『真・女神転生』 | [真・女神転生・110405187](https://ja.wikipedia.org/w/index.php?oldid=110405187) |
| wiki-cf64ef74cb30 | オマエ ハ ヤリスギタ。ヤリスギタノダ！ | ビッグ・ボス『メタルギア』 | [メタルギア・110852222](https://ja.wikipedia.org/w/index.php?oldid=110852222) |
| wiki-bd048ad70688 | もし、わしの味方になれば世界の半分を勇者にやろう | 竜王『ドラゴンクエスト』 | [ドラゴンクエスト・110700451](https://ja.wikipedia.org/w/index.php?oldid=110700451) |
| wiki-8824bb5e0904 | 大丈夫です！ | 王泥喜法介『逆転裁判』 | [逆転裁判・110593494](https://ja.wikipedia.org/w/index.php?oldid=110593494) |
| wiki-f80ff46c3b44 | 俺より強いヤツに会いに行く。 | 『ストリートファイターII』 | [ストリートファイターII・110874802](https://ja.wikipedia.org/w/index.php?oldid=110874802) |

### ことわざ

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| proverb-0c29c924fed8 | 七転び八起き | 日本のことわざ | [七転び八起き・109898563](https://ja.wikipedia.org/w/index.php?oldid=109898563) |
| proverb-673f55047f24 | 猫に小判 | 日本のことわざ | [猫に小判・108405358](https://ja.wikipedia.org/w/index.php?oldid=108405358) |
| proverb-66bfab3b947f | 石の上にも三年 | 日本のことわざ | [石の上にも三年・104814846](https://ja.wikipedia.org/w/index.php?oldid=104814846) |
| proverb-c63de776d12f | 花より団子 | 日本のことわざ | [花より団子・95411333](https://ja.wikipedia.org/w/index.php?oldid=95411333) |
| proverb-c32b4622ecc4 | 出る杭は打たれる | 日本のことわざ | [普通・107526362](https://ja.wikipedia.org/w/index.php?oldid=107526362) |
| proverb-cc562d8009fa | 時は金なり | 日本のことわざ | [時は金なり・110682824](https://ja.wikipedia.org/w/index.php?oldid=110682824) |
| proverb-c7b4b6dcac7b | どんぐりの背比べ | 日本のことわざ | [ドングリ・110052978](https://ja.wikipedia.org/w/index.php?oldid=110052978) |
| proverb-981a33c12250 | 一寸先は闇 | 日本のことわざ | [一寸先は闇・106045832](https://ja.wikipedia.org/w/index.php?oldid=106045832) |
| proverb-6cffa215aa23 | 仏の顔も三度 | 日本のことわざ | [仏の顔も三度まで・106253226](https://ja.wikipedia.org/w/index.php?oldid=106253226) |
| proverb-430f4d399a0d | 光陰矢の如し | 日本のことわざ | [光陰矢の如し・107222723](https://ja.wikipedia.org/w/index.php?oldid=107222723) |
| proverb-5374813cc1d5 | 勝って兜の緒を締めよ | 日本のことわざ | [勝って兜の緒を締めよ・109727029](https://ja.wikipedia.org/w/index.php?oldid=109727029) |
| proverb-592b52e80afa | 口は災いのもと | 日本のことわざ | [長柄橋・109579586](https://ja.wikipedia.org/w/index.php?oldid=109579586) |
| proverb-4cc44d4f5f9b | 善は急げ | 日本のことわざ | [善は急げ・105407311](https://ja.wikipedia.org/w/index.php?oldid=105407311) |
| proverb-69455bd90cdb | 嘘も方便 | 日本のことわざ | [嘘も方便・105264352](https://ja.wikipedia.org/w/index.php?oldid=105264352) |
| proverb-bbf51bca8c67 | 塵も積もれば山となる | 日本のことわざ | [塵・108225181](https://ja.wikipedia.org/w/index.php?oldid=108225181) |
| proverb-c8ec5db2aeef | 好きこそ物の上手なれ | 日本のことわざ | [好きこそ物の上手なれ・107584943](https://ja.wikipedia.org/w/index.php?oldid=107584943) |
| proverb-46f16ce80199 | 安物買いの銭失い | 日本のことわざ | [安物買いの銭失い・107512884](https://ja.wikipedia.org/w/index.php?oldid=107512884) |
| proverb-790630fcc701 | 弘法も筆の誤り | 日本のことわざ | [弘法も筆の誤り・108529851](https://ja.wikipedia.org/w/index.php?oldid=108529851) |
| proverb-90f42ba98340 | 二兎を追う者は一兎をも得ず | 日本のことわざ | [二兎を追う者は一兎をも得ず・108829617](https://ja.wikipedia.org/w/index.php?oldid=108829617) |
| proverb-29a2ce76e00c | 井の中の蛙 | 日本のことわざ | [井の中の蛙大海を知らず・109281164](https://ja.wikipedia.org/w/index.php?oldid=109281164) |
| proverb-5b09b42ce97e | 急がば回れ | 日本のことわざ | [急がば回れ・109100281](https://ja.wikipedia.org/w/index.php?oldid=109100281) |
| proverb-67c0a7e352f5 | 情けは人の為ならず | 日本のことわざ | [情けは人の為ならず・110931510](https://ja.wikipedia.org/w/index.php?oldid=110931510) |
| proverb-436b508a421d | 捕らぬ狸の皮算用 | 日本のことわざ | [タヌキ・110436856](https://ja.wikipedia.org/w/index.php?oldid=110436856) |
| proverb-41574b50a6f3 | 損して得取れ | 日本のことわざ | [かるた・110286123](https://ja.wikipedia.org/w/index.php?oldid=110286123) |
| proverb-fd784b7409c9 | 早起きは三文の徳 | 日本のことわざ | [早起きは三文の徳・105518958](https://ja.wikipedia.org/w/index.php?oldid=105518958) |
| proverb-2654030db652 | 朱に交われば赤くなる | 日本のことわざ | [朱に交われば赤くなる・102352489](https://ja.wikipedia.org/w/index.php?oldid=102352489) |
| proverb-0cc1c0f061a6 | 果報は寝て待て | 日本のことわざ | [果報は寝て待て・107882092](https://ja.wikipedia.org/w/index.php?oldid=107882092) |
| proverb-a60d5f11e42e | 歳月人を待たず | 日本のことわざ | [歳月人を待たず・108671557](https://ja.wikipedia.org/w/index.php?oldid=108671557) |
| proverb-cf80d1b50ff8 | 河童の川流れ | 日本のことわざ | [ことわざ・110402608](https://ja.wikipedia.org/w/index.php?oldid=110402608) |
| proverb-ffecac53d977 | 泣きっ面に蜂 | 日本のことわざ | [かるた・110286123](https://ja.wikipedia.org/w/index.php?oldid=110286123) |
| proverb-9f043645818a | 犬も歩けば棒に当たる | 日本のことわざ | [かるた・110286123](https://ja.wikipedia.org/w/index.php?oldid=110286123) |
| proverb-1f8d53941928 | 病は気から | 日本のことわざ | [病は気から (慣用句)・96491405](https://ja.wikipedia.org/w/index.php?oldid=96491405) |
| proverb-390154dc5e7e | 目くそ鼻くそを笑う | 日本のことわざ | [かったい・106168863](https://ja.wikipedia.org/w/index.php?oldid=106168863) |
| proverb-9824e886647c | 知らぬが仏 | 日本のことわざ | [かるた・110286123](https://ja.wikipedia.org/w/index.php?oldid=110286123) |
| proverb-fd5981dd6b59 | 継続は力なり | 日本のことわざ | [継続は力なり・105425455](https://ja.wikipedia.org/w/index.php?oldid=105425455) |
| proverb-ab57ded4f6e5 | 聞くは一時の恥 | 日本のことわざ | [長野県方言・110847061](https://ja.wikipedia.org/w/index.php?oldid=110847061) |
| proverb-4ae0adfaf402 | 蛙の子は蛙 | 日本のことわざ | [ことわざ・110402608](https://ja.wikipedia.org/w/index.php?oldid=110402608) |

### 百人一首

| ID | 言葉 | 人物・作品 | Wikipediaの確認版 |
|---|---|---|---|
| hyakunin-001 | 秋の田のかりほの庵のとまをあらみ ／ わが衣手は露にぬれつつ | 天智天皇 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-002 | 春すぎて夏きにけらし白妙の ／ 衣干すてふ天のかぐ山 | 持統天皇 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-003 | 足曳の山鳥の尾のしだり尾の ／ 長々し夜を獨りかも寝む | 柿本人麻呂 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-004 | 田子の浦にうち出でて見れば白妙の ／ 富士の高嶺に雪は降りつつ | 山辺赤人 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-005 | 奥山に紅葉踏み分け鳴く鹿の ／ 聲きく時ぞ秋はかなしき | 猿丸大夫 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-006 | 鵲の渡せる橋におく霜の ／ 白きを見れば夜ぞ更けにける | 中納言家持 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-007 | 天の原ふりさけ見れば春日なる ／ 三笠の山に出でし月かも | 安倍仲麻呂 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-008 | わが庵は都のたつみしかぞ住む ／ 世をうぢ山と人はいふなり | 喜撰法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-009 | 花の色は移りにけりないたづらに ／ わが身世にふるながめせしまに | 小野小町 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-010 | 是れやこの行くもかへるも別れては ／ 知るもしらぬも逢坂の関 | 蝉丸 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-011 | わたのはら八十島かけてこぎ出でぬと ／ 人には告げよあまの釣船 | 参議篁 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-012 | 天津風雲の通路ふきとぢよ ／ をとめの姿しばしとどめむ | 僧正遍昭 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-013 | 筑波嶺のみねより落つるみなの川 ／ 恋ぞつもりて淵となりぬる | 陽成院 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-014 | 陸奥のしのぶもぢずり誰故に ／ 乱れそめにしわれならなくに | 河原左大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-015 | 君がためはるの野に出でて若菜つむ ／ わが衣手に雪はふりつつ | 光孝天皇 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-016 | 立別れいなばの山の峰に生ふる ／ まつとしきかば今かへりこむ | 中納言行平 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-017 | 千早振る神代もきかず竜田川 ／ から紅に水くくるとは | 在原業平朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-018 | 住の江の岸に寄る波よるさへや ／ 夢の通ひ路人めよくらむ | 藤原敏行朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-019 | 難波がた短き蘆のふしの間も ／ 逢はで此世をすぐしてよとや | 伊勢 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-020 | 佗ぬれば今はたおなじなにはなる ／ みをつくしてもあはむとぞ思ふ | 元良親王 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-021 | 今来むといひしばかりに長月の ／ 有明の月を待出でつるかな | 素性法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-022 | 吹くからに秋の草木のしをるれば ／ むべ山風を嵐と云ふらむ | 文屋康秀 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-023 | 月見れば千々に物こそ悲しけれ ／ わが身一つの秋にはあらねど | 大江千里 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-024 | 此の度はぬさも取あへず手向山 ／ 紅葉のにしき神のまにまに | 菅家 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-025 | 名にしおはば逢坂山のさねかづら ／ 人に知られでくるよしもがな | 三条右大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-026 | 小倉山峰のもみぢ葉心あらば ／ 今一度のみゆきまたなむ | 貞信公 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-027 | みかの原わきてながるるいづみ川 ／ いつみきとてか恋しかるらむ | 中納言兼輔 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-028 | 山里は冬ぞ寂しさまさりける ／ 人めも草もかれぬと思へば | 源宗于朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-029 | 心あてにをらばやをらむはつしもの ／ 置きまどはせる白菊のはな | 凡河内躬恒 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-030 | 有明のつれなく見えし別れより ／ 暁ばかりうきものはなし | 壬生忠岑 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-031 | 朝ぼらけ有明の月と見るまでに ／ よしのの里に降れる白雪 | 坂上是則 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-032 | 山川に風のかけたる柵は ／ 流れもあへぬ紅葉なりけり | 春道列樹 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-033 | 久方の光のどけき春の日に ／ しづ心なく花の散るらむ | 紀友則 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-034 | 誰をかもしる人にせむ高砂の ／ 松も昔の友ならなくに | 藤原興風 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-035 | 人はいさ心も知らずふるさとは ／ 花ぞ昔の香に匂ひける | 紀貫之 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-036 | 夏の夜はまだ宵ながら明けぬるを ／ 雲のいづこに月やどるらむ | 清原深養父 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-037 | 白露に風の吹きしく秋の野は ／ つらぬきとめぬ玉ぞ散りける | 文屋朝康 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-038 | 忘らるる身をば思はずちかひてし ／ 人の命のをしくもあるかな | 右近 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-039 | 浅ぢふのをのの篠原しのぶれど ／ あまりてなどか人の恋しき | 参議等 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-040 | 忍ぶれど色に出でにけりわが恋は ／ 物や思ふと人の問ふまで | 平兼盛 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-041 | 戀すてふわが名はまだきたちにけり ／ 人知れずこそ思ひそめしか | 壬生忠見 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-042 | 契りきなかたみに袖をしぼりつつ ／ すゑの松山波こさじとは | 清原元輔 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-043 | 逢見ての後の心にくらぶれば ／ 昔は物を思はざりけり | 権中納言敦忠 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-044 | 逢ふことの 絶えてしなくは なかなかに ／ 人をも身をも 恨みざらまし | 中納言朝忠 | [藤原朝忠・109790334](https://ja.wikipedia.org/w/index.php?oldid=109790334) |
| hyakunin-045 | 哀ともいふべき人はおもほえで ／ 身のいたづらになりぬべきかな | 謙徳公 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-046 | 由良の門をわたる舟人かぢをたえ ／ ゆくへも知らぬ恋の道かな | 曾禰好忠 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-047 | 八重葎しげれる宿のさびしきに ／ 人こそ見えね秋は来にけり | 恵慶法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-048 | 風をいたみ岩うつ波のおのれのみ ／ くだけて物を思ふころかな | 源重之 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-049 | 御垣守衛士のたく火の夜はもえて ／ 昼は消えつつ物をこそ思へ | 大中臣能宣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-050 | 君がため惜しからざりし命さへ ／ ながくもがなと思ひけるかな | 藤原義孝 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-051 | かくとだにえやはいぶきのさしも草 ／ さしも知らじなもゆるおもひを | 藤原実方朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-052 | 明ぬれば暮るるものとは知りながら ／ 猶恨めしき朝ぼらけかな | 藤原道信朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-053 | なげきつつ独りぬる夜のあくるまは ／ いかに久しきものとかはしる | 右大将道綱母 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-054 | 忘れじの行末まではかたければ ／ 今日をかぎりの命ともがな | 儀同三司母 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-055 | 滝の音は絶えて久しくなりぬれど ／ 名こそ流れて猶聞こえけれ | 大納言公任 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-056 | あらざらむ此世の外の思ひ出に ／ 今ひとたびの逢ふ事もがな | 和泉式部 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-057 | 巡りあひて見しや夫ともわかぬまに ／ 雲がくれにし夜半の月かな | 紫式部 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-058 | 有馬山ゐなの笹原風ふけば ／ いでそよ人を忘れやはする | 大弐三位 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-059 | 安らはで寝なましものを小夜更けて ／ かたぶくまでの月を見しかな | 赤染衛門 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-060 | 大江山いく野の道の遠ければ ／ まだ文も見ず天のはし立 | 小式部内侍 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-061 | いにしへの奈良の都の八重桜 ／ けふ九重に匂ひぬるかな | 伊勢大輔 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-062 | 夜をこめて鳥の空音ははかるとも ／ 世に逢坂の関はゆるさじ | 清少納言 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-063 | 今はただ思ひ絶えなむとばかりを ／ 人づてならでいふよしもがな | 左京大夫道雅 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-064 | 朝ぼらけ宇治の川ぎりたえだえに ／ あらはれ渡る瀬々のあじろぎ | 権中納言定頼 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-065 | 恨みわびほさぬ袖だにあるものを ／ 恋に朽ちなむ名こそをしけれ | 相模 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-066 | もろともにあはれと思へ山桜 ／ 花より外に知る人もなし | 前大僧正行尊 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-067 | 春の夜の夢ばかりなる手枕に ／ かひなく立たむ名こそをしけれ | 周防内侍 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-068 | 心にもあらでうき世に長らへば ／ 恋しかるべき夜半の月かな | 三条院 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-069 | 嵐吹く三室の山のもみぢ葉は ／ 竜田の川のにしきなりけり | 能因法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-070 | 淋しさに宿を立ち出でてながむれば ／ いづこも同じ秋のゆふぐれ | 良暹法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-071 | 夕されば門田のいなばおとづれて ／ あしのまろやに秋風ぞふく | 大納言経信 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-072 | 音に聞くたかしの浜のあだ浪は ／ かけじや袖のぬれもこそすれ | 祐子内親王家紀伊 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-073 | 高砂の尾上の桜咲きにけり ／ 外山の霞たたずもあらなむ | 権中納言匡房 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-074 | 憂かりける人をはつせの山おろしよ ／ はげしかれとは祈らぬものを | 源俊頼朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-075 | 契りおきしさせもが露を命にて ／ あはれ今年の秋もいぬめり | 藤原基俊 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-076 | 和田の原こぎ出でて見れば久方の ／ 雲ゐにまがふ沖津白なみ | 法性寺入道前関白太政大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-077 | 瀬をはやみ岩にせかるる滝川の ／ われても末にあはむとぞ思ふ | 崇徳院 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-078 | 淡路島かよふ千鳥の鳴く声に ／ いく夜ねざめぬ須磨の関守 | 源兼昌 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-079 | 秋風にたなびく雲の絶間より ／ もれ出づる月の影のさやけさ | 左京大夫顕輔 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-080 | 長からむ心もしらず黒髪の ／ みだれて今朝はものをこそ思へ | 待賢門院堀河 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-081 | ほととぎすなきつる方をながむれば ／ ただ有明の月ぞ残れる | 後徳大寺左大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-082 | 思ひわびさても命はある物を ／ うきにたへぬは涙なりけり | 道因法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-083 | 世の中よ道こそなけれ思ひ入る ／ 山の奥にも鹿ぞなくなる | 皇太后宮大夫俊成 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-084 | 永らへばまた此頃やしのばれむ ／ うしと見し世ぞ今は恋しき | 藤原清輔朝臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-085 | 夜もすがら物思ふころは明けやらで ／ 閨の隙さへつれなかりけり | 俊恵法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-086 | 嘆けとて月やはものを思はする ／ かこち顔なるわが涙かな | 西行法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-087 | 村雨の露もまだひぬまきの葉に ／ 霧たちのぼる秋の夕ぐれ | 寂蓮法師 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-088 | 難波江の蘆のかり寝のひと夜ゆゑ ／ 身を尽てや戀わたるべき | 皇嘉門院別当 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-089 | 玉の緒よたえなばたえね永らへば ／ 忍ぶる事のよわりもぞする | 式子内親王 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-090 | 見せばやな雄島のあまの袖だにも ／ 濡れにぞぬれし色はかはらず | 殷富門院大輔 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-091 | きりぎりすなくや霜夜のさむしろに ／ 衣かたしき独りかもねむ | 後京極摂政前太政大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-092 | わがそでは潮干に見えぬ沖の石の ／ 人こそしらねかわく間もなし | 二条院讃岐 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-093 | 世の中は常にもがもな渚漕ぐ ／ 海士の小舟の綱でかなしも | 鎌倉右大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-094 | みよし野の山の秋風小夜更けて ／ ふる郷さむく衣うつなり | 参議雅経 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-095 | おほけなく浮世の民におほふかな ／ わがたつ杣に墨染の袖 | 前大僧正慈円 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-096 | 花さそふ嵐の庭の雪ならで ／ ふりゆくものはわが身なりけり | 入道前太政大臣 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-097 | 来ぬ人をまつほの浦の夕なぎに ／ やくや藻塩の身もこがれつつ | 権中納言定家 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-098 | 風そよぐならの小川の夕暮は ／ みそぎぞ夏のしるしなりける | 従二位家隆 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-099 | 人もをし人も恨めし味気なく ／ 世を思ふ故に物おもふ身は | 後鳥羽院 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |
| hyakunin-100 | 百敷や古き軒端のしのぶにも ／ 猶あまりある昔なりけり | 順徳院 | [百人一首・110843159](https://ja.wikipedia.org/w/index.php?oldid=110843159) |

