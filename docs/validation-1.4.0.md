# ROCKETBOY 1.4.0 検証記録

2026-09-09 / versionCode 7 / Android 15エミュレーター、Pixel 10a。

## 変更

- 名言を迎撃すると、二重の衝撃波、8方向の光線、40個の破片、ひらがなの破片、大きな得点表示を約1.6秒描画。通常・名言・競技の全モード共通。ランクバナーとの重なりを回避。
- 競技モードを追加。通常モードの初期条件からWAVE 1・0点・残機1に固定。通常隕石・名言の落下速度を同じWAVEの1.5倍にする。通常の出現規則と開始WAVE 1の+1％を維持。
- 競技終了時に専用の結果・共有画面を表示。表示中のスコアカードと同じ960×1080px PNGと、得点・称号・WAVE・#rocketboyを含む投稿文を共有。途中終了は画像と文章に明記。
- 共有先から戻る、Activityを再生成する場合も結果を保持。再挑戦はWAVE 1・0点・残機1。保存済みの競技ミッションとTOP 10はモード情報を保持。

## 確認結果

単体テスト**41件成功**。競技の速度倍率（通常・名言・速度上限到達後）、残機1、保存復元、加点タイミング、終了後の加点防止に加え、既存ルール・名言・称号・スコア履歴を確認。lintは**0 errors / 35 warnings**。警告は既存のローカライズ・アイコン等と、カスタムViewコンストラクター・KTX推奨等。

Androidの確認5項目は、それぞれ別のinstrumentation起動で成功。

1. 競技設定→日本語IMEの確定→名言迎撃と演出→1回着弾→結果→画面再生成→画像共有→再挑戦。
2. 競技を中断して再生成・再開。速度を再度掛けずに復元。通常・競技・名言を切り替え、元のWAVE設定と残機数を確認。
3. 通常モードは3回着弾で終了し、再挑戦で初期化。
4. 通常モードのゲーム名言を迎撃し、収集・再起動後の保存を確認。
5. 名言で昇格したときのBGM切替・中断再開・新規ミッションの曲を確認。

同じ競技・共有の一連の操作を**720×1280 / 320dpi**でも実施して成功。標準画面は1080×2400 / 420dpi。試験後にエミュレーターの画面設定を元に戻した。

共有ボタンから発行した実際のIntentのPNG URI、MIME、本文、ClipData、読み取り権限を確認。同じペイロードを**別UIDで動くテスト用アプリ**に渡し、960×1080の画像と本文を読み取れることを確認。Androidの共有シートでも画像プレビューと本文を目視確認した。実機のXには投稿とXChatの画像受信Activityがあることをパッケージ照会で確認したが、**X投稿画面内の操作・実投稿は未実施**。Xの構成によって投稿とXChatの選択が表示される可能性がある。

画像はキャッシュ内の専用ディレクトリのみを公開し、共有相手へ一時的な読み取り権限を渡す。ストレージ・インターネット権限の追加なし。実装の参照：[Androidの共有](https://developer.android.com/training/sharing/send)、[FileProviderの設定](https://developer.android.com/training/secure-file-sharing/setup-sharing)。

## 成果物と実機

- APK: `artifacts/1.4.0/ROCKETBOY-1.4.0-debug.apk`
- サイズ: 33,689,832 bytes
- SHA-256: `8e972cacc35a8ccd57aba90222acf96752e5a32e98a02d7f3c29d3db051432f8`
- Android debug署名、APK Signature Scheme v2検証成功。
- Pixel 10aとエミュレーターへ同じAPKを更新インストール。インストール済みAPKのSHA-256一致、versionCode 7 / versionName 1.4.0、起動成功を確認。
- Pixel 10aのSharedPreferencesを更新前後で比較して**バイト単位で一致**。物理端末へのタップ・キー入力は注入していない。
- BGM26曲・効果音5個・名言リソースを1.3.1のAPKと比較し、バイト単位の一致を確認。

## 画面と録画

- [名言撃破](screenshots/1.4.0/quote-burst.png)
- [競技結果](screenshots/1.4.0/competitive-result.png)
- [共有するPNG](screenshots/1.4.0/shared-score.png)
- [Android共有シート](screenshots/1.4.0/android-share-sheet.png)
- [小型画面の演出](screenshots/1.4.0/compact-quote-burst.png)
- [小型画面の共有](screenshots/1.4.0/compact-share-sheet.png)

短い実行録画は `artifacts/1.4.0/ROCKETBOY-1.4.0-quote-effect.mp4`（無音）。再現可能な検証のため名言と着弾位置を配置し、Android IMEの確定を通じて実際のエンジンとレンダラーを動かした記録。通常の長時間プレイを装った映像ではない。

初回の失敗はテスト専用ActivityのKotlinランタイム不足と、画面遷移中の入力タイミング。テスト側を修正して最終成功を確認し、途中ログも `artifacts/1.4.0` に保持。前版の記録：[1.3.1](validation-1.3.1.md)。
