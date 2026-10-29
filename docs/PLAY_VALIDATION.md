# Google Play更新候補 1.5.0 検証結果

2026-09-16 / `io.github.hatake716.rocketboy` / **1.5.0（10）** / minSdk 26 / targetSdk 36。

## アプリの検証

| 項目 | 結果 |
|---|---|
| 単体テスト | 44件成功、失敗・エラー・スキップ0 |
| lintRelease | エラー0、警告35 |
| debug / release APK / AAB | ビルド成功。releaseはR8・リソース縮小を適用 |
| 配色・入力・遷移のAndroidテスト | 7種類、画面サイズ変更を含む8実行成功。全26配色、IMEの変換維持、下線、ランクアップ、再開・再生成、競技共有画像を検証 |
| 今回追加のAAB動作確認 | AABから生成したuniversal APKで、撮影を兼ねた1テスト成功 |
| APK / AAB署名 | apksigner / jarsigner検証済み。従来のアップロード証明書と一致 |
| AAB構造 | bundletool validate成功。公式bundletool 1.18.3でuniversal APKを生成 |
| 配布APK | zipalign検証済み。AAB由来APKは非debuggable、縦画面 |
| 音源・名言・ポリシー | 元の3成果物に加え、AAB由来APKの31音源・3つの名言TSV・assets全5件がソースとバイト一致 |
| ネイティブライブラリ | AAB由来APKに.soなし |
| 権限 | INTERNET・AD_IDなし。AndroidXの内部レシーバー用signature権限のみ |
| 再起動 | AAB由来APKを強制終了してコールド起動、保存した最高得点100000をUIで再確認 |
| クラッシュ | 撮影・確認区間のAndroidクラッシュバッファに記録なし |

配色の詳しい確認は [validation-1.5.0.md](validation-1.5.0.md)。今回の追加試験は `ReleaseStoreCaptureTest#capture150StoreUpdate`。配布アプリとは別の署名付き検証用APKを使い、R8後の内部クラスへ直接依存しない画面操作で確認しました。検証用APKは提出ZIPに含めていません。

## 新しいストア画面

API 35 / Android 15の一時エミュレーターを `-read-only -no-snapshot` で起動し、1080×1920 / 360dpi、Gboard日本語12キーで撮影しました。

- 地球12000点、火星20000点、冥王星100000点のプレイ画面。長い語の2行目まで入力済み下線を表示。
- 通常モードは実際の入力・発射で12000→12090点。
- 月の配色の名言モードでデカルトの読みを入力し、16000→19636点。名言コレクションへの保存を確認。
- 競技モードで19980→20000点、1回の着弾で結果表示。火星色の画像、共有ボタン、#rocketboyを確認。外部への投稿は行っていません。
- 228件・6カテゴリの名言集、BGM試聴室の地球曲の再生・停止、冥王星色のタイトル、トップ10を確認。

保存スコア・隕石の語と配置は撮影用に設定しています。自然なプレイで0点から100000点まで到達した録画ではありません。撮影した画面に得点や機能を追加せず、不透明PNGを24bit RGB PNGに変換しました。8枚の一覧と代表画面の原寸画像を目視確認しています。最初の3枚はゲーム画面で、色の違いと入力下線が分かる順序です。代替テキスト・SHA-256は [store/screenshots.json](store/screenshots.json)。

## 成果物

| ファイル | bytes | SHA-256 |
|---|---:|---|
| ROCKETBOY-1.5.0-release.aab | 30224641 | `69ffe98c818e64df6094ee189f6d6eda342444ce0e17d8f544bf696e575669b4` |
| ROCKETBOY-1.5.0-release.apk | 31057104 | `9b97f93e652db05c10cbc44e5a781b5ef9095db26e0d6b9db8bfd0b8919a5530` |
| ROCKETBOY-1.5.0-debug.apk（実機開発版の更新用、提出ZIP外） | 33573516 | `93c8d628208169ff0aaf7af46ac7db2478863d77d58355bba0289f9f6eb9306d` |

アップロード証明書SHA-256：`e2d003608cc7d9536e0f36a7571b4fae06c7e5ecf4c08b18ce13943bba5ba11e`。

AAB由来のuniversal APK：`51e5d2cd81bd29827417c352feef545c9506814c5d44c8a4b66b9dda287b5a78`。検証用に保持し、提出ZIPには含めていません。

記録は `artifacts/palettes-1.5.0/` と `artifacts/play-1.5.0/verification/`。配布物のチェックサムはZIP内 `SHA256SUMS.txt`、ZIP自体のハッシュは隣の `.zip.sha256`。旧版1.4.1 ZIPはSHA-256 `5db5aaafe90da37231acc1febf78779d15d8012d014b08358cf0b569b1abf01a` のまま保持しています。名言確認CSVも変更していません。秘密鍵・パスワード・debug/test APKを提出ZIPから除外しています。

## 実機と公開状況

<!-- physical-status-start -->
Pixel 10a（端末申告Android 17）へ、既存の1.4.2 / code 9と同じdebug証明書のAPKで **1.5.0 / code 10を上書きインストール済み**。インストール済みAPKのSHA-256が検証済みdebug APKと一致し、起動・前面表示を確認しました。

初回起動前の保存データ `rocketboy.xml` は更新前後でバイト単位に一致。SHA-256は `abbb4943c64290fa4d0f13dc8ef6c17e5049e7dbfae508c84ed0d61e468eadf3`。アンインストール・データ消去・ゲームへの入力操作は行っていません。実機での手動プレイの受入確認は利用者側で行えます。検証記録はZIP内 `verification/physical-update.json` に同梱しています。
<!-- physical-status-end -->

今回、GitHubへのアプリ更新やGoogle Playへのアップロード・審査送信は行っていません。Console上の使用済みversionCode・アップロード証明書・申告状況は未確認です。プライバシー・サポート・Webサイトの3ページはHTTPS 200とローカルHTML一致を再確認済み。

名言の内容と音源を変更していないため、Wikipedia記事の再取得や全曲の長時間再生試験は行っていません。従来の内容確認・申告資料を引き継いでいます。過去の検証は [1.4.1の記録](PLAY_VALIDATION-1.4.1.md) に保持しました。
