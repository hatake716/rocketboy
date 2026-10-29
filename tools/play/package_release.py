#!/usr/bin/env python3
"""Validate and package prepared local Play assets, excluding all private signing material."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import csv,hashlib,json,re,shutil,subprocess,zipfile,xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[2];REL=ROOT/'artifacts/play-1.4.1';VER=REL/'verification';STORE=ROOT/'docs/store';OUT=REL/'GooglePlay-Package'
assert 'OK (1 test)' in (VER/'release-ui.log').read_text()
assert 'OK (1 test)' in (VER/'aab-release-ui.log').read_text()
hashof=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
shots=[('01-normal','ひらがなで隕石を迎撃','通常モード。日本語キーボードで、そら・ほしの隕石を迎撃する画面。'),('02-quote','名言に挑戦','名言モード。デカルトの言葉の読みと獲得できるポイントを表示。'),('03-quote-interception','打ち切った瞬間に光が広がる','名言の迎撃に成功し、3,636点と光のエフェクトを表示。'),('04-competitive','残機1の競技モード','競技モードのゲーム画面。残機1、落下速度1.5倍のルール。'),('05-competitive-result','スコア画像を共有','競技結果の画像と共有ボタン。得点、称号、WAVEと #rocketboy を表示。'),('06-collection','6カテゴリの名言集','歴史上の人物、企業経営者、アニメ・漫画、ゲーム、ことわざ、百人一首のカテゴリ一覧。'),('07-music','26曲のBGM試聴室','BGM試聴室で地球の曲を再生。訓練生と天体の曲を自由に選択できる。'),('08-title','その一文字が、地球を救う','ROCKETBOYのタイトル画面。ドット絵のロケットと地球、ミッション開始ボタン。')]
records=[]
for name,title,alt in shots:
 p=VER/'capture'/f'{name}.png';image=Image.open(p);assert image.size==(1080,1920)
 dest=STORE/'ja-JP/images'/f'{name}.png';image.convert('RGB').save(dest)
 records.append({'file':dest.name,'title':title,'alt':alt,'size':[1080,1920],'sha256':hashof(dest)})
manifest={'release':'1.4.1 (8)','source':'Signed AAB generated universal APK, Android 15 API 35 disposable emulator, 1080x1920 / 360dpi','captureTool':'tools/play/release-checks','fixtures':'Deterministic saved missions with fixed words/positions/speeds; scores start at zero and are calculated by the shipping engine after entering hiragana and pressing the actual Fire button. One quote is genuinely unlocked in the demo save. No fabricated game UI or score text overlays. System UI demo mode hides notifications.','screenshots':records,'featureGraphicAlt':'ROCKETBOY。ひらがなの隕石に向かうドット絵のロケットと、地球の地平線。','iconAlt':'緑のドット絵で描いたロケット。','video':'Existing 30-second trailer uses actual version 1.4.0 emulator recordings; described gameplay remains the same in 1.4.1.'}
(STORE/'screenshots.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
icon=Image.open(STORE/'ja-JP/images/icon-512.png');assert icon.size==(512,512) and icon.mode=='RGBA';assert (STORE/'ja-JP/images/icon-512.png').stat().st_size<=1024*1024
feature=Image.open(STORE/'ja-JP/images/feature-graphic.png');assert feature.size==(1024,500) and feature.mode=='RGB'
for name,limit in [('title',30),('short-description',80),('full-description',4000),('release-notes',500)]:assert len((STORE/f'ja-JP/{name}.txt').read_text().strip())<=limit
# Contact sheet is a review artifact, not one of the uploaded screenshots.
board=Image.new('RGB',(1080,1050),'#101e19');d=ImageDraw.Draw(board);font=ImageFont.truetype(str(ROOT/'app/src/main/res/font/dot_gothic.ttf'),16)
for i,(name,title,alt) in enumerate(shots):
 im=Image.open(STORE/f'ja-JP/images/{name}.png');im.thumbnail((252,448));x=(i%4)*270+9;y=(i//4)*525+12;board.paste(im,(x,y));d.text((x,y+465),title,font=font,fill='#d4ed9b')
board.save(REL/'store-preview.png')
# Only this tool-owned staging directory is rebuilt.
if OUT.exists():shutil.rmtree(OUT)
OUT.mkdir()
def copy(src,dst):
 dst=OUT/dst;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/src,dst)
for name in ['ROCKETBOY-1.4.1-release.aab','ROCKETBOY-1.4.1-release.apk','upload-certificate.pem']:copy(Path('artifacts/play-1.4.1')/name,Path('android')/name)
copy(Path('app/build/outputs/mapping/release/mapping.txt'),'android/r8-mapping.txt')
for name in ['PLAY_STORE.md','PLAY_DECLARATIONS.md','PLAY_CONTENT_REVIEW.md','PLAY_VALIDATION.md','ASSETS.md','SOUNDTRACK.md']:
 copy(Path('docs')/name,Path('docs')/name)
shutil.copytree(STORE,OUT/'docs/store')
for name in ['index.html','privacy/index.html','support/index.html','.nojekyll']:copy(Path('docs')/name,Path('website')/name)
copy(Path('docs/store/ja-JP/images/feature-graphic.png'),'website/store/ja-JP/images/feature-graphic.png')
for name in ['wikipedia.json','wikipedia-audit.json','wikipedia-verification.json']:
 copy(Path('data/quotes')/name,Path('provenance')/name)
for p in (ROOT/'app/src/main/assets/licenses').glob('*.txt'):copy(p.relative_to(ROOT),Path('licenses')/p.name)
copy(Path('app/src/main/assets/privacy-ja.txt'),'licenses/privacy-ja.txt');copy(Path('LICENSE'),'licenses/ROCKETBOY-MIT.txt')
copy(Path('artifacts/promo-30s/ROCKETBOY-30s-Short-1080x1920.mp4'),'video/ROCKETBOY-30s-Short-1080x1920.mp4')
copy(Path('artifacts/promo-30s/cover.png'),'video/cover.png');copy(Path('artifacts/play-1.4.1/store-preview.png'),'store-preview.png')
for name in ['artifact-audit.json','source-sha256.json','apk-signature.txt','aab-signature.txt','bundle-validation.txt','bundle-manifest.xml','zipalign.txt','universal-apk-signature.txt','runtime-dependencies.txt','release-ui.log','aab-release-ui.log','competition-framing.log','relaunch.xml','relaunch.png','aab-relaunch.txt','final-crash-log.txt','wikipedia-verification.txt','gradle.log','gradle-final.log']:
 copy(Path('artifacts/play-1.4.1/verification')/name,Path('verification')/name)
for name in ['verification-privacy.png','verification-top10.png','acceptance.txt']:copy(Path('artifacts/play-1.4.1/verification/capture')/name,Path('verification')/name)
(OUT/'START_HERE.md').write_text('''# ROCKETBOY Google Play 公開準備パッケージ

1.4.1 / versionCode 8 / 準備日 2026-09-09

まず `docs/PLAY_CONTENT_REVIEW.md` と `docs/PLAY_STORE.md` を開いてください。
**資料と署名付き候補を準備した状態です。Google Playへ公開・アップロードしていません。**
対象は子どもを含む全年齢、連絡先は acesmash@gmail.com です。

- `android/*.aab`：Play Consoleへアップロードするバンドル。
- `android/*.apk`：直接インストールして確認する署名付きAPK。既存のdebug版へ上書きできません。記録の削除を伴うアンインストールはしないでください。
- `android/upload-certificate.pem`：公開証明書。秘密鍵ではありません。
- `docs/store/ja-JP/*.txt`：アプリ名・短い説明・詳しい説明・リリースノート。
- `docs/store/ja-JP/images/`：アイコン、フィーチャーグラフィック、スマートフォン画面8枚。
- `docs/PLAY_DECLARATIONS.md`：Data safety・広告・アクセス・IARC等の回答案。
- `docs/store/quote-review.csv`：名言全228件の出典・解説・権利/内容確認欄。
- `website/`：公開済みのプライバシーとサポートを含む静的Webページの控え。URLと検証記録は docs/store/publication.json。
- `video/`：30秒紹介動画。Playの動画欄にはYouTube URLを登録するため、別途投稿が必要です。
- `verification/`：署名・バンドル・データ・リリースUIの検証結果。
- `provenance/`、`licenses/`：名言の出典記録とライセンス説明。
- `SHA256SUMS.txt`：ファイル整合性チェック。

公開前の残作業：名言のアプリ内利用条件、子ども向けの内容/外部共有への対応、料金・配信地域・対象端末の決定、IARCなどの申告、アカウント条件に応じたクローズドテストと審査。年齢レーティング・Families準拠・権利処理完了を認定するパッケージではありません。

アップロード鍵・パスワード・debug版・テスト用APKは同梱していません。開発環境の鍵は `/home/takeshi/.local/share/rocketboy-signing/` で管理しています。資料中の `artifacts/` やソースへのパスは開発リポジトリを基準にしています。ZIP内のファイルの場所は上記一覧を参照してください。
''',encoding='utf-8')
files=sorted(p for p in OUT.rglob('*') if p.is_file())
for p in files:
 assert p.suffix not in ['.jks','.keystore'] and p.name not in ['keystore.properties','local.properties']
 assert 'debug.apk' not in p.name and 'ui-test.apk' not in p.name
(OUT/'SHA256SUMS.txt').write_text(''.join(f'{hashof(p)}  {p.relative_to(OUT)}\n' for p in files))
zip_path=ROOT/'artifacts/ROCKETBOY-GooglePlay-1.4.1.zip'
with zipfile.ZipFile(zip_path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(OUT.rglob('*')):
  if p.is_file():z.write(p,Path('ROCKETBOY-GooglePlay-1.4.1')/p.relative_to(OUT))
with zipfile.ZipFile(zip_path) as z:assert z.testzip() is None
(ROOT/'artifacts/ROCKETBOY-GooglePlay-1.4.1.zip.sha256').write_text(hashof(zip_path)+'  '+zip_path.name+'\n')
print(json.dumps({'zip':str(zip_path),'bytes':zip_path.stat().st_size,'sha256':hashof(zip_path),'files':len(files)+1,'screenshots':len(records)},ensure_ascii=False,indent=2))
