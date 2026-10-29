# ROCKETBOY Google Play 公開準備

準備日：2026-09-09。対象：1.4.1 / versionCode 8 / `io.github.hatake716.rocketboy`。

**署名付きAAB・APK、ストア素材、入力用資料を準備する段階です。Play Consoleへのアップロード・申告送信・本番公開は未実施です。** 子どもを含む全年齢を対象とする方針と、サポートメール `acesmash@gmail.com` は開発者の指定です。IARCレーティングを取得したという意味ではありません。

## 最初に使うファイル

| 用途 | ファイル |
|---|---|
| 提出物一式 | `artifacts/ROCKETBOY-GooglePlay-1.4.1.zip` |
| Play Consoleにアップロードするバンドル | `artifacts/play-1.4.1/ROCKETBOY-1.4.1-release.aab` |
| ローカル確認用の署名付きAPK | `artifacts/play-1.4.1/ROCKETBOY-1.4.1-release.apk` |
| ストア入力文章 | [store/ja-JP](store/ja-JP/) の title / short-description / full-description / release-notes `.txt` |
| ストア画像 | [store/ja-JP/images](store/ja-JP/images/) |
| Data safety・広告・アクセス等の回答案 | [PLAY_DECLARATIONS.md](PLAY_DECLARATIONS.md) |
| 収録内容・権利・子ども向け対応の確認事項 | [PLAY_CONTENT_REVIEW.md](PLAY_CONTENT_REVIEW.md) |
| 名言全228件の出典と確認欄 | [store/quote-review.csv](store/quote-review.csv)（UTF-8 BOM、Excel等で閲覧可） |
| バージョン・入力項目・未決事項 | [store/submission.json](store/submission.json) |
| ビルド・署名・UI検証結果 | [PLAY_VALIDATION.md](PLAY_VALIDATION.md) |

APKはアップロード鍵で署名しています。Googleが別のアプリ署名鍵を生成するPlay App Signingを選ぶ場合、Playから配信されるAPKの署名はこのローカルAPKと異なります。開発用debug版とも署名が異なります。**既存の実機のdebug版をアンインストールしていません。** 上書きできない場合に記録を消して進めないでください。

## ストア基本情報

- アプリ名：ROCKETBOY
- デフォルト言語：日本語（ja-JP）
- 種類：ゲーム。カテゴリ案：言葉。Consoleに表示される選択肢で確定します。
- サポートメール：acesmash@gmail.com
- 対象：子どもを含む全年齢を意図。ただし5歳以下を含め各年齢層で、日本語キーボードの操作・制限時間・名言内容の適切性を確認してから選択します。
- 料金：未決定。無料/有料を開発者が確定します。広告とアプリ内課金は実装していません。
- 配信地域：未決定。引用・翻訳等の権利確認の対象地域と整合させて確定します。
- 対象端末：Android 8.0以降の縦画面スマートフォン。タブレット・Chromebook・PC・TV・Wear・XR向けの操作検証や専用素材は今回の範囲に含みません。Consoleの自動配信設定も確認してください。
- プライバシーポリシーURL：`https://hatake716.github.io/rocketboy/privacy/`
- サポートURL：`https://hatake716.github.io/rocketboy/support/`
- WebサイトURL：`https://hatake716.github.io/rocketboy/`

**上記3ページは2026-09-09に公開済みです。未ログインのHTTPSアクセスで200応答と、ローカルHTMLとのバイト一致を確認しました。** Pagesは `gh-pages` ブランチのルート `/` を配信します。公開コミットは `1184f3caa04a8f9a24f6b7ce89985812501c6e0f`。公開元の対応は `docs/index.html` → `index.html`、`docs/privacy/index.html` → `privacy/index.html`、`docs/support/index.html` → `support/index.html` です。更新時はリンク先の画像と `.nojekyll` も含めて同ブランチへ反映してください。mainブランチ上のHTML変更だけではPagesに反映されません。アプリ内には同じポリシー本文を同梱し、オフラインで閲覧できます。検証記録は [store/publication.json](store/publication.json) にあります。

## 画像・動画

- アイコン：512×512、RGBA PNG、1MB以下。角丸マスクやストアのバッジは焼き込んでいません。
- フィーチャーグラフィック：1024×500、RGB PNG。
- スマートフォン画面：1080×1920、9:16、RGB PNG、8枚。番号順に使用します。最初の3枚で実際のゲーム画面を示します。再現用の保存データを使い、実リリース版の画面を撮影しています。得点は入力後にゲームが算出したものです。
- 代替テキストと撮影条件：[store/screenshots.json](store/screenshots.json)。ゲームに存在しない機能や架空のオンラインランキングを画像へ追加していません。
- 30秒ショート：同梱の `video/ROCKETBOY-30s-Short-1080x1920.mp4`。実際のv1.4.0エミュレーター映像を編集した紹介動画で、1.4.1でも紹介するゲーム機能は同じです。
- Playのプレビュー動画欄はMP4の直接アップロードではなくYouTube URLです。動画ファイルをYouTubeへ公開または限定公開で配置し、広告・年齢制限などの要件を確認してからURLを登録します。YouTubeへの投稿は未実施です。

画像の要件は[Google Playのプレビュー素材ガイド](https://support.google.com/googleplay/android-developer/answer/9866151)、文字数上限は[アプリ作成・設定](https://support.google.com/googleplay/android-developer/answer/9859152)で確認しました。掲載文はアプリ名30字・短い説明80字・詳しい説明4,000字の範囲内です。

## Consoleへ進む順序

1. [内容確認表](PLAY_CONTENT_REVIEW.md)の未確認事項を解消し、必要な実装修正や収録内容の調整があれば版番号を上げて再ビルドします。
2. 公開済みのプライバシーポリシーURLをConsoleへ登録し、掲載内容との一致を確認します。
3. 開発者アカウント・本人/端末確認・支払設定・料金・配信地域を確定します。既に同じapplicationIdが登録されている場合は、登録済みアップロード証明書と今回の証明書を照合します。
4. 新規アプリとして登録し、Play App Signingを設定します。今回のAABを内部テストへアップロードし、Consoleのバンドル解析・事前レポートを確認します。
5. ストア文章と8枚の画面、アイコン、フィーチャーグラフィック、連絡先、公開済みポリシーURLを登録します。
6. アプリアクセス、広告、Data safety、対象年齢、IARC等を実態に合わせて回答します。テンプレートは送信済み申告ではありません。
7. 対象アカウントで必要なクローズドテストを実施し、不具合修正とフィードバックを記録します。
8. 本番アクセスを取得後、公開範囲・公開タイミングを決めて審査へ提出します。

2026-08-31以降、新規のスマホアプリ・更新はtarget API 36以上が必要です。この版は36です。[対象API要件](https://support.google.com/googleplay/android-developer/answer/11926878)

2023-11-13より後に作成した個人アカウントには、12人以上が連続14日間オプトインするクローズドテスト等が必要です。アカウントの該当性・本番アクセス状況は未確認です。[個人アカウントのテスト要件](https://support.google.com/googleplay/android-developer/answer/14151465)

## 署名・再現

アップロード鍵は `/home/takeshi/.local/share/rocketboy-signing/rocketboy-upload.jks`。パスワードを含む設定の予備は同ディレクトリの `keystore.properties`、ビルド用設定はリポジトリ直下の無視対象 `keystore.properties`。いずれも秘密ファイルは所有者のみが読める設定です。暗号化した保管先へ鍵とパスワードを別管理でバックアップしてください。**秘密鍵・パスワードは提出ZIPやGitに含めません。** ZIPには公開証明書だけを含めます。

署名設定なしでもCIはunsignedのreleaseをビルドできますが、その成果物は提出用ではありません。公開準備には署名設定と `apksigner` / `jarsigner` による確認が必要です。`keystore.properties.example` 参照。

素材を再生成するには `python3 tools/play/prepare_materials.py` と Pillow環境で `tools/play/render_graphics.py` を実行します。検証・撮影方法は [tools/play/README.md](../tools/play/README.md) にあります。
