#!/usr/bin/env python3
"""Package the verified 1.5.0 update; never build, install, publish, or read keys."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[2]
REL = ROOT / 'artifacts/play-1.5.0'
VER = REL / 'verification'
STORE = ROOT / 'docs/store'
OUT = REL / 'GooglePlay-Package'
VERSION = '1.5.0'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def main():
    assert 'OK (1 test)' in (VER / 'aab-store-capture.log').read_text()
    assert 'versionCode=\'10\' versionName=\'1.5.0\'' in (VER / 'universal-badging.txt').read_text()
    assert 'e2d003608cc7d9536e0f36a7571b4fae06c7e5ecf4c08b18ce13943bba5ba11e' in (VER / 'universal-signature.txt').read_text()
    assert digest(ROOT / 'artifacts/ROCKETBOY-GooglePlay-1.4.1.zip') == '5db5aaafe90da37231acc1febf78779d15d8012d014b08358cf0b569b1abf01a'
    assert digest(STORE / 'quote-review.csv') == 'eb228d4eaec4d3b83bba17baa7a6ea5cf8e54c1f972dc3637ad0e0fa19d469ca'
    artifacts = json.loads((ROOT / 'docs/build-artifacts.json').read_text())
    assert artifacts['versionName'] == VERSION and artifacts['versionCode'] == 10
    for item in artifacts['artifacts']:
        assert digest(ROOT / item['file']) == item['sha256']

    shots = [
        ('01-earth', '地球の青緑', '地球ランクの通常モード。ひらがなの隕石と、2行目までの入力済み下線。'),
        ('02-mars', '火星の赤銅色', '火星ランクの通常モード。赤銅色の宇宙で隕石を迎撃。'),
        ('03-pluto', '冥王星の紫', '100000点の冥王星ランク。紫のゲーム画面と日本語キーボード。'),
        ('04-quote-moon', '名言モード', '月の配色で名言モード。デカルトの言葉の読みを入力。'),
        ('05-competitive-result', '競技結果を共有', '20000点・火星ランクの競技結果と、画像と #rocketboy を共有するボタン。'),
        ('06-collection', '6カテゴリの名言集', '全228件。歴史上の人物、企業経営者、アニメ・漫画、ゲーム、ことわざ、百人一首。'),
        ('07-music', '26曲のBGM試聴室', 'BGM試聴室で地球の曲を再生。全26曲を自由に選択。'),
        ('08-title', '最高記録が彩るタイトル', '自己ベスト100000点・冥王星に合わせた紫のROCKETBOYタイトル。'),
    ]
    images = STORE / 'ja-JP/images'
    records = []
    for name, title, alt in shots:
        source = VER / 'capture' / f'{name}.png'
        with Image.open(source) as im:
            assert im.size == (1080, 1920)
            if 'A' in im.getbands():
                assert im.getchannel('A').getextrema() == (255, 255)
            im.convert('RGB').save(images / source.name)
        with Image.open(images / source.name) as converted, Image.open(source) as original:
            assert converted.mode == 'RGB' and converted.tobytes() == original.convert('RGB').tobytes()
        records.append(dict(file=source.name, title=title, alt=alt, size=[1080, 1920], mode='RGB', sha256=digest(images / source.name)))
    # Remove only superseded, tool-generated 1.4.1 screenshots, after confirming the archive.
    for name in ['01-normal', '02-quote', '03-quote-interception', '04-competitive']:
        p = images / f'{name}.png'
        if p.exists():
            archived = ROOT / 'artifacts/play-1.4.1/store-original/ja-JP/images' / p.name
            assert digest(p) == digest(archived)
            p.unlink()
    manifest = dict(release='1.5.0 (10)', source='Signed AAB-generated universal APK; Android 15 API 35 disposable emulator; 1080x1920 / 360dpi',
                    captureTool='tools/play/release-checks',
                    fixtures='Saved scores and fixed meteor words/positions demonstrate ranks. Real input and Fire: normal 12000->12090, quote 16000->19636, competitive 19980->20000. No score or UI overlays; only lossless opaque RGB conversion. System UI demo mode hides notifications.',
                    screenshots=records, iconAlt='緑のドット絵のロケット。',
                    featureGraphicAlt='ROCKETBOY。ひらがなの隕石に向かうロケットと地球。',
                    branding='Icon and feature graphic reused unchanged from 1.4.1.', video=None)
    write_json(STORE / 'screenshots.json', manifest)
    for file, size, mode in [('icon-512.png', (512, 512), 'RGBA'), ('feature-graphic.png', (1024, 500), 'RGB')]:
        with Image.open(images / file) as im:
            assert im.size == size and im.mode == mode
        assert digest(images / file) == digest(ROOT / 'artifacts/play-1.4.1/store-original/ja-JP/images' / file)
    assert (images / 'icon-512.png').stat().st_size <= 1024 * 1024
    lengths = {}
    for name, limit in [('title', 30), ('short-description', 40), ('full-description', 4000), ('release-notes', 500)]:
        value = (STORE / f'ja-JP/{name}.txt').read_text().strip()
        lengths[name] = len(value)
        assert 0 < len(value) <= limit
    assert (STORE / 'ja-JP/release-notes-play-console.txt').read_text().strip() == '<ja-JP>\n' + (STORE / 'ja-JP/release-notes.txt').read_text().strip() + '\n</ja-JP>'

    board = Image.new('RGB', (1080, 1020), '#151827')
    draw = ImageDraw.Draw(board)
    font = ImageFont.truetype(str(ROOT / 'app/src/main/res/font/dot_gothic.ttf'), 18)
    for i, record in enumerate(records):
        with Image.open(images / record['file']) as im:
            x, y = (i % 4) * 270 + 9, (i // 4) * 510 + 9
            board.paste(im.resize((252, 448)), (x, y))
            draw.text((x, y + 461), record['title'], font=font, fill='#dbe7ff')
    board.save(REL / 'store-preview.png')

    if OUT.exists():
        shutil.rmtree(OUT)  # This script's dedicated generated staging directory only.
    OUT.mkdir(parents=True)

    def copy(src, dst=None):
        src = Path(src)
        target = OUT / (dst if dst is not None else src)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / src, target)

    for suffix in ['aab', 'apk']:
        copy(f'artifacts/1.5.0/ROCKETBOY-1.5.0-release.{suffix}', f'android/ROCKETBOY-1.5.0-release.{suffix}')
    copy('artifacts/1.5.0/mapping.txt', 'android/r8-mapping.txt')
    copy('artifacts/play-1.4.1/upload-certificate.pem', 'android/upload-certificate.pem')
    for name in ['PLAY_STORE.md', 'PLAY_VALIDATION.md', 'PLAY_VALIDATION-1.4.1.md', 'PLAY_DECLARATIONS.md', 'PLAY_CONTENT_REVIEW.md', 'ASSETS.md', 'SOUNDTRACK.md', 'PALETTES.md', 'validation-1.5.0.md', 'validation-1.5.0.json', 'build-artifacts.json']:
        copy(Path('docs') / name)
    copy('tools/play/README.md')
    for p in (ROOT / 'docs/screenshots/1.5.0').glob('*.png'):
        copy(p.relative_to(ROOT))
    for name in ['submission.json', 'screenshots.json', 'publication.json', 'public-urls-1.5.0.json', 'quote-review.csv', 'test-feedback-template.csv']:
        copy(Path('docs/store') / name)
    for p in (STORE / 'ja-JP').glob('*.txt'):
        copy(p.relative_to(ROOT))
    for name in [r['file'] for r in records] + ['icon-512.png', 'feature-graphic.png']:
        copy(Path('docs/store/ja-JP/images') / name)
    for p in (ROOT / 'app/src/main/assets/licenses').glob('*.txt'):
        copy(p.relative_to(ROOT), Path('licenses') / p.name)
    copy('app/src/main/assets/privacy-ja.txt', 'licenses/privacy-ja.txt')
    copy('LICENSE', 'licenses/ROCKETBOY-MIT.txt')
    for name in ['wikipedia.json', 'wikipedia-audit.json', 'wikipedia-verification.json']:
        copy(Path('data/quotes') / name, Path('provenance') / name)
    for name in ['aab-store-capture.log', 'universal-signature.txt', 'universal-badging.txt', 'universal-content.json', 'cold-launch.txt', 'cold-launch.xml', 'crash-log.txt']:
        copy((VER / name).relative_to(ROOT), Path('verification') / name)
    for name in ['update-acceptance.txt', 'verification-top10.png']:
        copy((VER / 'capture' / name).relative_to(ROOT), Path('verification') / name)
    for name in ['artifact-verification.json', 'aab-manifest.xml', 'aab-signature.txt', 'bundle-validate.txt', 'release-signature.txt', 'zipalign.txt', 'build-checks.log', 'release-palette.log', 'palette-render.log', 'palette-live.log', 'palette-flow.log', 'palette-competitive.log', 'palette-compact.log', 'underline-regression.log']:
        copy(Path('artifacts/palettes-1.5.0') / name, Path('verification') / name)
    if (VER / 'physical-update.json').exists():
        copy((VER / 'physical-update.json').relative_to(ROOT), 'verification/physical-update.json')
    copy((REL / 'store-preview.png').relative_to(ROOT), 'store-preview.png')
    (OUT / 'START_HERE.md').write_text('''# ROCKETBOY Google Play 更新用一式

1.5.0 / versionCode 10 / 2026-09-16

`docs/PLAY_STORE.md` に更新手順、`docs/PLAY_VALIDATION.md` に検証結果があります。
今回、Google Playへのアップロード・審査送信・公開操作は行っていません。

- `android/ROCKETBOY-1.5.0-release.aab`：既存アプリの更新に使うAAB。
- `android/ROCKETBOY-1.5.0-release.apk`：アップロード鍵で署名した直接インストール用APK。
- `android/r8-mapping.txt`：難読化対応表。`upload-certificate.pem` は公開証明書。
- `docs/store/ja-JP/*.txt`：掲載文と日本語の更新履歴。Console用の言語タグ付きも同梱。
- `docs/store/ja-JP/images/01-`〜`08-`：新配色を含む1080×1920 RGB PNG 8枚。
- 同じ画像フォルダーのアイコンとフィーチャーグラフィックは従来版を継続使用。
- `store-preview.png`：8枚の確認用一覧。ストアには個別画像を登録してください。
- `docs/store/screenshots.json`：掲載順、代替テキスト、撮影条件。
- `verification/`：署名・AAB・画面操作等の記録。
- `docs/PLAY_DECLARATIONS.md`、`docs/PLAY_CONTENT_REVIEW.md`：従来の申告案と内容確認表。
- `provenance/`、`licenses/`：出典・利用表示の控え。
- `SHA256SUMS.txt`：展開後の全ファイルのチェックサム。

サポート：acesmash@gmail.com
プライバシーポリシー：https://hatake716.github.io/rocketboy/privacy/

Console上でversionCode 10が未使用であることと、登録済みアップロード証明書を確認してください。
実機の開発用debug版とは署名が異なります。保存記録を消すアンインストールは行わず、同じ証明書のAPKで更新します。
秘密鍵・パスワード・debug APK・テストAPKは同梱していません。旧版動画も今回のZIPには含めていません。
資料にある `artifacts/` やアプリソースへの参照は開発リポジトリを基準にしています。
''')
    audit = dict(versionName=VERSION, versionCode=10, screenshotCount=8, textLengths=lengths,
                 opaquePixelColorsPreserved=True, oldPackageUnchanged=True, quoteReviewCsvUnchanged=True,
                 publicUrls=json.loads((STORE / 'public-urls-1.5.0.json').read_text()),
                 physical=json.loads((ROOT / 'docs/validation-1.5.0.json').read_text())['physical'],
                 playConsoleUploaded=False)
    write_json(OUT / 'verification/package-validation.json', audit)
    files = sorted(p for p in OUT.rglob('*') if p.is_file())
    for p in files:
        assert p.suffix not in ['.jks', '.keystore', '.p12', '.pfx']
        assert p.name not in ['keystore.properties', 'local.properties']
        assert 'debug.apk' not in p.name and 'test.apk' not in p.name
    (OUT / 'SHA256SUMS.txt').write_text(''.join(f'{digest(p)}  {p.relative_to(OUT)}\n' for p in files))
    archive = ROOT / 'artifacts/ROCKETBOY-GooglePlay-1.5.0.zip'
    prefix = 'ROCKETBOY-GooglePlay-1.5.0/'
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for p in sorted(OUT.rglob('*')):
            if p.is_file():
                z.write(p, prefix + str(p.relative_to(OUT)))
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for line in z.read(prefix + 'SHA256SUMS.txt').decode().splitlines():
            expected, name = line.split('  ', 1)
            assert hashlib.sha256(z.read(prefix + name)).hexdigest() == expected
    archive.with_suffix('.zip.sha256').write_text(digest(archive) + '  ' + archive.name + '\n')
    audit.update(zip=str(archive.relative_to(ROOT)), bytes=archive.stat().st_size, sha256=digest(archive), files=len(files) + 1, zipCrcAndAllChecksumsVerified=True)
    write_json(VER / 'package-validation.json', audit)
    print(json.dumps(audit, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
