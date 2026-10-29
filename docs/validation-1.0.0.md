# ROCKETBOY 検証記録

検証日: 2026-09-09（JST）

## 環境

- NixOS / JDK 17
- Gradle 8.14.3 / Android Gradle Plugin 8.13.0 / Kotlin 2.2.20
- Android SDK 36、Nixで提供されるaapt2
- Android 15 / API 35 / x86_64 エミュレーター
- 通常表示: 1080 × 2400 / 420 dpi
- 小型表示: 720 × 1280 / 320 dpi
- IME: Gboard、日本語12キー
- 実機: この検証時点では接続なし。実機へのインストール・スピーカーでの聴感確認は未実施。

## ゲームルール: 16件

`GameEngineTest` 12件、`RanksTest` 4件。

- ロケット到達時に文字数ぶん加点、同一標的への二重発射と二重加点を防止。
- 3回目の着弾で終了、終了後の発射・加点を防止。
- 誤入力・未完成のことばで加点やダメージが生じない。
- 半角カタカナ、小書き文字、結合濁点を正規化。
- 一時停止、復元、飛行中のロケットの再開で得点が重複しない。
- 保存データとゲーム内オブジェクトが独立したコピーである。
- 地上に達する直前でも、先に発射した標的を迎撃できる。
- 時間に応じた難易度上昇と上限。
- お題169件がひらがなのみで、1〜10字の表示条件に収まる。
- 次のプレイで得点と残機をリセット。
- 同時出現することばの重複と、レーン入口の重なりを防止。
- 不正なフレーム時間で状態を壊さない。
- 25称号の4,000点刻みの全境界、99,999 / 100,000 / 100,001点。
- 最高記録も累積点ではなく、各プレイの最大値である。

## Android E2E: 12件

実際のActivity、ネイティブ入力欄、描画ループ、保存領域、音声出力APIを使用。

1. 起動、縦向き指定、入力欄の可視性、キーボード表示領域。
2. 「か → が」の変換中に発射せず、確定後に1点。
3. 未完成入力の無減点と、変換中の「ちきゅう」をゲーム内ボタンで迎撃して4点。
4. 半角「ｷﾞｬｸ」の確定で3点、重複した確定通知で再加点しない。
5. 3回の着弾 → GAME OVER → 再挑戦で0点・シールド3へ。
6. 一時停止中の時間停止と、Activity再作成後の保存ミッション復元。
7. ホームへ移動して停止し、明示的な再開操作で続行。
8. BGM再生時にAndroidのアクティブ再生情報が存在し、一時停止でなくなる。
9. Gboardの「あ」キーと送信キーを実際にタップし、迎撃で1点になる。
10. 99,999点の状態から1字迎撃し、100,000点で冥王星へ昇格。
11. 後半WAVEの長いことばを表示・入力して正しい文字数を加点。
12. 称号一覧、設定、BGM/効果音設定の永続化。

通常表示で12件を通し、小型表示でも長いことばの配置とGboardからの実入力を確認しました。テストの終了時はホームに移動してIMEと描画を止めてからActivityを破棄します。動き続けるゲームとIMEに対して、汎用UIの無期限の静止待ちを使わない構成です。

100,000点や後半WAVEのテストは、テストプロセス内で境界直前の状態や隕石配置を準備しています。数時間の手動プレイで100,000字を入力した結果ではありません。リリースアプリに外部から得点を設定する機能やテスト用のexportedコンポーネントはありません。

## ビルド・静的確認

```bash
./gradlew \
  :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest \
  :app:lintDebug :app:assembleRelease :app:bundleRelease :app:lintRelease \
  -Pandroid.aapt2FromMavenOverride=/absolute/path/to/nix/aapt2
```

- Debug APK、Release APK、Release AABの生成。
- ReleaseはR8による縮小・最適化を含む。
- Debug / Release lintはエラー0。固定縦向き、使用ライブラリの更新提案、日本語専用の文言、KTX記法などに関する警告は残る。
- Release用の署名鍵は作成・設定していない。Release APK/AABは未署名。
- Google Play Consoleへのアップロード・公開は行っていない。

## 音源

全6ファイルを検査。22050 Hz / 16-bit / モノラル、空の音源なし、波形のクリッピングなし。BGMは約45.714秒のループ。BGMと効果音はサンプル素材を使わず合成している。

AndroidでのBGM再生・停止はテスト済み。エミュレーターはホストへの音声出力を無効にしているため、実機スピーカーの音量感や聴感の評価とは区別する。

## 画面の証跡

- `screenshots/01-title.png`: 通常のタイトル画面。
- `screenshots/02-gameplay.png`: 後半WAVEの配置を使った長文表示の確認。
- `screenshots/validation/native-input.png`: Gboard実タップによる1点の迎撃。
- `screenshots/validation/interception.png`: 「ちきゅう」で4点の迎撃。
- `screenshots/validation/game-over.png`: 3回着弾後の結果。
- `screenshots/validation/pause.png`: 一時停止。
- `screenshots/compact/late-wave.png`: 小型画面で10字のお題を表示。
- `screenshots/compact/native-input.png`: 小型画面でGboardから迎撃。

画面内のスコア・WAVE・隕石配置には検証用の初期値を含む。画像はエミュレーターの実画面を取得しており、合成したUIモックではない。
