# ROCKETBOY 検証結果

現在の版は **1.5.0 / versionCode 10**。到達した天体のランクに合わせて、ゲーム内の配色が変わるようになりました。訓練生の緑＋25天体の専用配色です。

- [1.5.0の変更・検証内容・APK](validation-1.5.0.md)
- [全26配色の一覧](PALETTES.md)
- [最新の機械可読結果](validation-results.json)
- [ビルド成果物・署名・ハッシュ](build-artifacts.json)
- [Google Play更新資料とAAB由来APKの追加確認](PLAY_VALIDATION.md)

単体テスト44件、Androidテスト7種類・画面サイズを変えた再実行を含め8件成功。全26配色の実描画、日本語の変換中テキストを保った配色切り替え、昇格・複数ランク上昇・再開・再挑戦、競技結果と共有画像、release APKを確認。さらにAAB由来APKでストア撮影と操作確認の1件が成功し、コールド起動後の最高記録を確認しました。lintエラー0。Pixel 10aへ1.5.0を上書きインストールし、保存データのバイト一致・正常起動を確認しました。実機へのゲーム入力は行っていません。

以前の検証：[1.4.2の下線修正・実機更新](validation-1.4.2.md)、[1.4.1の公開準備](PLAY_VALIDATION-1.4.1.md)、[1.4.0の競技・共有・実機更新](validation-1.4.0.md)、[1.3.1](validation-1.3.1.md)、[1.3.0](validation-1.3.0.md)、[1.2.1](validation-1.2.1.md)、[1.2.0](validation-1.2.0.md)、[1.0.0](validation-1.0.0.md)。
