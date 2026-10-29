# ROCKETBOY Google Play 更新ガイド

準備日：2026-09-16。**1.5.0 / versionCode 10** / `io.github.hatake716.rocketboy`。

星ごとの配色と複数行の入力下線修正を含む更新資料です。今回、Play Consoleへのアップロード・審査送信・公開操作は行っていません。Console上の現在の版番号・署名登録・申告状況は未確認です。

## 使用するファイル

| 用途 | 開発リポジトリ内のファイル |
|---|---|
| 更新用一式 | `artifacts/ROCKETBOY-GooglePlay-1.5.0.zip` |
| Playへアップロード | `artifacts/1.5.0/ROCKETBOY-1.5.0-release.aab` |
| 直接インストール用release APK | `artifacts/1.5.0/ROCKETBOY-1.5.0-release.apk` |
| R8対応表 | `artifacts/1.5.0/mapping.txt` |
| 掲載文 | [store/ja-JP](store/ja-JP/) の title / short-description / full-description `.txt` |
| 更新履歴 | [本文](store/ja-JP/release-notes.txt)、[Console用の言語タグ付き](store/ja-JP/release-notes-play-console.txt) |
| 新しい画面8枚 | [store/ja-JP/images](store/ja-JP/images/) の `01-`〜`08-` |
| 撮影順・代替テキスト・ハッシュ | [store/screenshots.json](store/screenshots.json) |
| 検証記録 | [PLAY_VALIDATION.md](PLAY_VALIDATION.md)、[配色の検証](validation-1.5.0.md) |
| 申告案・内容確認表 | [PLAY_DECLARATIONS.md](PLAY_DECLARATIONS.md)、[PLAY_CONTENT_REVIEW.md](PLAY_CONTENT_REVIEW.md) |
| 入力項目 | [store/submission.json](store/submission.json) |

ZIP内ではAAB・release APK・mapping・公開証明書は `android/`、文章と画像は `docs/store/ja-JP/` にあります。`START_HERE.md` から参照できます。実機の開発版へ上書きするdebug APKは提出ZIPに含めていません。

## 更新内容

- 訓練生と25天体の計26配色。プレイ中はその挑戦のスコアに合わせて色が変化。
- タイトル・メニューは自己ベスト、競技結果と共有PNGはそのプレイの結果に合わせた色。
- 新しい挑戦は緑から開始。途中再開・画面再生成で該当する配色を復元。
- 1.4.2で修正した、2行目以降の入力下線を含む。

配点、名言228件、BGM26曲、効果音5種、保存形式、権限、SDK、データの扱いは従来と同じです。

## Consoleでの更新手順

1. 既存のROCKETBOYを開き、パッケージ名 `io.github.hatake716.rocketboy` を確認します。
2. 全トラックの使用済みversionCodeを確認します。今回の候補は **10**。10が未使用で、更新対象より大きいことを確認します。既に10以上を使用している場合、さらに大きい番号で再ビルドします。
3. 登録済みの**アップロード鍵**の証明書と、下記のSHA-256を照合します。Play App Signingのアプリ署名証明書とは区別してください。
4. 内部テストの新しいリリースへ `ROCKETBOY-1.5.0-release.aab` を追加します。リリース名は `1.5.0 (10)`、更新履歴は言語タグ付きテキストを貼り付けます。
5. 日本語ストア掲載情報を新しい文章と8枚の画面に更新します。アイコンとフィーチャーグラフィックは従来と同じファイルです。
6. Playからのテスト配信で、既存データの引継ぎ、ランクアップ時の色、長い語の下線、名言・BGM・競技結果を確認します。Consoleの解析結果・事前レポートも確認します。
7. 既存のData safety・対象年齢・内容レーティング等との一致を確認し、通常の運用手順で製品版リリースへ進めます。公開割合や公開タイミングはConsoleで選択してください。

更新には同一パッケージ、より大きいversionCode、正しいアップロード署名が必要です。[Googleの更新手順](https://support.google.com/googleplay/android-developer/answer/9859350?hl=ja)。更新履歴は日本語本文500文字以内です。[リリースの準備](https://support.google.com/googleplay/android-developer/answer/9859348?hl=ja)。

## 画像と掲載文

スマートフォン用は **1080×1920、9:16、24bit RGB PNG、8枚**。番号順に登録します。

| 順序 | 画面 |
|---|---|
| 01 | 地球の青緑・複数行のひらがな入力 |
| 02 | 火星の赤銅色 |
| 03 | 冥王星の紫 |
| 04 | 月の配色で名言モード |
| 05 | 火星ランクの競技結果・共有ボタン |
| 06 | 6カテゴリの名言コレクション |
| 07 | BGM試聴室・地球の曲を再生中 |
| 08 | 自己ベスト冥王星のタイトル |

AABから生成したrelease APKの画面を一時エミュレーターで撮影しました。各ランクを示すため保存スコア・隕石配置を設定しています。入力と加点は実際のアプリで確認し、画面への文字や架空の機能の追加はしていません。撮影後は不透明PNGをRGB形式へ変換しています。

ZIP直下の `store-preview.png` は一覧確認用です。ストアには個別の8枚を登録してください。アイコンは512×512 RGBA PNG、フィーチャーグラフィックは1024×500 RGB PNGです。掲載文はアプリ名30文字、短い説明は全角40文字相当以内、詳しい説明4,000文字以内で作成しています。[プレビュー素材の仕様](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ja)。

既存の30秒動画は1.4.0の映像のため、今回のZIPには含めていません。

## 連絡先と公開URL

- サポートメール：acesmash@gmail.com
- 対象年齢の方針：子どもを含む全年齢。Consoleの現在の年齢レーティングは未確認。
- [プライバシーポリシー](https://hatake716.github.io/rocketboy/privacy/)
- [サポートページ](https://hatake716.github.io/rocketboy/support/)
- [Webサイト](https://hatake716.github.io/rocketboy/)

3ページとも2026-09-16にHTTPS 200応答・ローカルHTMLとのバイト一致を再確認しました。[確認記録](store/public-urls-1.5.0.json)。今回Webページの再公開は行っていません。

価格、国・地域、IARC、アカウントごとの公開条件はConsoleの既存設定を確認してください。以前の[名言確認表](store/quote-review.csv)は維持しています。今回、未確認の権利・対象年齢の項目を解消済みと判定していません。

## 署名と再現

アップロード証明書のSHA-256：

`e2d003608cc7d9536e0f36a7571b4fae06c7e5ecf4c08b18ce13943bba5ba11e`

ZIPには公開証明書 `upload-certificate.pem` を含め、秘密鍵・パスワード・debug APK・テストAPKは含めていません。release APKと実機の開発用debug APKは署名が異なります。実機は同じ署名のAPKでデータを残して更新します。

再撮影とパッケージ生成は [tools/play/README.md](../tools/play/README.md) を参照してください。旧版ZIP `ROCKETBOY-GooglePlay-1.4.1.zip` は変更せず保持しています。
