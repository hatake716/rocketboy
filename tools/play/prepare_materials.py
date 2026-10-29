#!/usr/bin/env python3
"""Generate store copy, public HTML and review CSV from shipped content. No uploads."""
from pathlib import Path
import collections,csv,html,json,re,shutil
ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs'; STORE=DOC/'store'; JA=STORE/'ja-JP'
JA.mkdir(parents=True,exist_ok=True)
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s.rstrip()+'\n',encoding='utf-8')
write(JA/'title.txt','ROCKETBOY')
write(JA/'short-description.txt','ひらがなを打って隕石を迎撃。名言を集め、天体の称号とレトロな電子音を楽しむ縦画面タイピングゲーム')
write(JA/'full-description.txt','''その一文字が、地球を救う。

空から降ってくる、ひらがなの隕石。
スマホの日本語キーボードで読みを入力すると、地球からロケットが発射されます。
着弾する前に迎撃して、宇宙の果ての称号を目指しましょう。

【よんで、うって、迎撃】
隕石に表示された読みをひらがなで入力。ことばの確定でロケットが自動発射されます。変換中は「発射」ボタンでも迎撃できます。短いことばから始まり、時間がたつほど隕石の数・速さ・ことばの長さが増していきます。

【3つのモード】
・通常モード：短いことばと、ときどき現れる名言に挑戦。着弾3回でゲームオーバー。
・名言モード：名言だけを続けて入力するモード。長い言葉を打ちきった瞬間、ドットの光が画面に広がります。
・競技モード：開始WAVE 1、残機1、落下速度1.5倍。結果画面からスコア画像と #rocketboy を外部の共有アプリへ渡せます。

【打つほど広がる名言コレクション】
歴史上の人物、企業経営者、アニメ・漫画作品、ゲーム、ことわざ、百人一首の6カテゴリに228件を収録。迎撃した名言はコレクションに加わり、解説とWikipediaの出典を読めます。名言の基本点は読み1文字につき300点、通常のことばは1文字10点です。

【25天体の称号と26曲の音楽】
1プレイのスコアで4,000点ごとに昇格。100,000点で最上位の冥王星へ到達します。代表的な25天体をもとにしたゲーム独自の称号です。
天体ごとに変わるオリジナルBGMと、訓練生のテーマを収録。BGM試聴室では全26曲を自由に聴けます。4色のドット絵と電子音が、宇宙の冒険を彩ります。

【自分のペースでも、記録への挑戦でも】
通常・名言モードは開始WAVEを1〜16から選択できます。スコアは0点から始まり、開始WAVE×1％のボーナスが迎撃ごとに加算されます（1点未満切り捨て）。
自己ベストトップ10を端末に保存。途中で一時停止して、後から再開することもできます。

【Androidで、縦画面で】
Android 8.0以降に対応。日本語入力ができるキーボードをご利用ください。
ゲーム本体はオフラインで遊べます。広告・アプリ内課金・アカウント登録はありません。外部への共有や出典サイトの閲覧には、対応するアプリやインターネット接続が必要になる場合があります。

記録と設定は端末内に保存され、Androidの設定によりバックアップや端末移行の対象になります。開発者への自動送信はありません。

お問い合わせ：acesmash@gmail.com''')
write(JA/'release-notes.txt','''ROCKETBOYを公開します。
・ひらがなで隕石を迎撃する、縦画面のタイピングゲーム
・通常、名言、競技の3モード
・解説付きの名言228件と6カテゴリのコレクション
・25天体の称号、オリジナルBGM26曲と試聴室
・自己ベストトップ10、開始WAVE設定、途中再開
・競技スコア画像の外部共有
・アプリ内プライバシーポリシー''')
qdata=json.loads((ROOT/'data/quotes/wikipedia.json').read_text());authors={a['id']:a for a in qdata['authors']}
flags={'death':['死','亡','命を','最後の闘い'], 'violence':['戦闘','戦士','戦場','狙撃','狙い撃','軍事','秘孔','おしおき','闘い'], 'fear':['悪魔','不安','不穏','恐怖','その目'], 'crime':['犯人','裁判','裁こう'], 'alcohol':['酒','酔'], 'romance':['恋','愛する','愛し','逢','契り','忍ぶ']}
rows=[]
for q in qdata['quotes']:
 body=q['text']+' '+q['explanation']+' '+authors[q['authorId']]['biography']
 found=[k for k,words in flags.items() if any(w in body for w in words)]
 rows.append({'id':q['id'],'category':q['category'],'author_or_work':authors[q['authorId']]['name'],'text':q['text'],'reading':q['reading'],'explanation':q['explanation'],'source_url':q['sourceUrl'],'source_attribution':q['source'],'wording':q['wording'],'license_notice':q['license'],'rights_status':'要確認：原典・翻訳・配信地域と利用条件（古典/慣用句）' if q['category'] in ['hyakunin','proverb'] else '要確認：原著作物・翻訳・台詞等のアプリ利用条件','content_review_tags':','.join(found) or '自動語句検出なし（人手確認は必要）','rights_evidence':'','reviewer':'','review_date':''})
with (STORE/'quote-review.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,rows[0].keys());w.writeheader();w.writerows(rows)
counts=collections.Counter(q['category'] for q in qdata['quotes'])
meta={'prepared':'2026-09-09','applicationId':'io.github.hatake716.rocketboy','versionName':'1.4.1','versionCode':8,'language':'ja-JP','contactEmail':'acesmash@gmail.com','audienceIntent':'子どもを含む全年齢（ユーザー指定）','contentRating':'IARC未取得','categorySuggestion':'ゲーム / 言葉','price':'未決定（広告・アプリ内課金は実装なし）','countries':'未決定','minimumAndroid':'8.0 / API 26','targetSdk':36,'quoteCounts':dict(counts),'quoteTotal':len(rows),'soundtrackCount':26,'rankCount':25,'privacyUrlPlanned':'https://hatake716.github.io/rocketboy/privacy/','websiteUrlPlanned':'https://hatake716.github.io/rocketboy/','publicUrlsVerified':False,'youtubeUrl':None,'playConsoleUploaded':False,'githubPublished':False,'releaseReviewOpenItems':['名言の原著作物・翻訳等のアプリ内利用条件確認','子どもを含む対象年齢に対する収録内容と外部共有のFamilies対応確認','公開Webページの配置とURL検証','料金・配信国・対象端末とPlayアカウント条件の確定','IARC質問票・Data safety申告・該当アカウントのクローズドテスト']}
write(STORE/'submission.json',json.dumps(meta,ensure_ascii=False,indent=2))
privacy=(ROOT/'app/src/main/assets/privacy-ja.txt').read_text()
write(STORE/'privacy-policy-ja.txt',privacy)
css='''*{box-sizing:border-box}body{margin:0;background:#101e19;color:#d4ed9b;font:17px/1.9 system-ui,sans-serif}main{max-width:840px;margin:auto;padding:48px 24px}a{color:#d4ed9b;text-underline-offset:4px}h1,h2{line-height:1.5}h1{font-size:36px}h2{font-size:22px;margin-top:40px;color:#b9d18c}p{white-space:pre-line}nav{display:flex;gap:24px;flex-wrap:wrap;margin:24px 0}footer{color:#a4bb84;border-top:1px solid #739453;margin-top:40px;padding-top:20px;font-size:14px}img{max-width:100%;height:auto;border:1px solid #739453}'''
def page(title,body,up=''):
 return f'<!doctype html>\n<html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>{html.escape(title)}</title><meta name="description" content="ROCKETBOYの公式サポートとプライバシーポリシー"><style>{css}</style></head><body><main><nav><a href="{up}index.html">ROCKETBOY</a><a href="{up}privacy/index.html">プライバシー</a><a href="{up}support/index.html">サポート</a></nav>{body}<footer>ROCKETBOY / hatake716<br>お問い合わせ：<a href="mailto:acesmash@gmail.com">acesmash@gmail.com</a></footer></main></body></html>'
parts=privacy.strip().split('\n\n'); privacybody='<h1>'+html.escape(parts[0].split('\n')[0])+'</h1><p>'+html.escape(parts[0].split('\n')[1])+'</p>'
for part in parts[1:]:
 lines=part.split('\n',1)
 if re.match(r'^\d+\.',lines[0]): privacybody+='<h2>'+html.escape(lines[0])+'</h2><p>'+html.escape(lines[1])+'</p>'
 else:privacybody+='<p>'+html.escape(part)+'</p>'
write(DOC/'privacy/index.html',page('ROCKETBOY プライバシーポリシー',privacybody,'../'))
write(DOC/'index.html',page('ROCKETBOY — その一文字が、地球を救う。','''<h1>ROCKETBOY</h1><p>その一文字が、地球を救う。</p><img src="store/ja-JP/images/feature-graphic.png" width="1024" height="500" alt="ひらがなの隕石へ向かうドット絵のロケットと地球"><h2>よんで、うって、迎撃。</h2><p>スマホの日本語キーボードで、落ちてくる隕石の読みを入力する縦画面タイピングゲーム。通常・名言・競技の3モード、228件の名言コレクション、天体をモチーフにした音楽が宇宙の冒険を彩ります。</p><p>Android 8.0以降。日本語入力キーボードが必要です。ゲーム本体はオフライン対応、広告・アプリ内課金・アカウント登録なし。</p><p>配信準備中です。Google Playへの公開後、このページにストアへのリンクを掲載します。</p>'''))
write(DOC/'support/index.html',page('ROCKETBOY サポート','''<h1>ROCKETBOY サポート</h1><p>お問い合わせ：<a href="mailto:acesmash@gmail.com">acesmash@gmail.com</a></p><h2>不具合の連絡</h2><p>アプリのバージョン、端末名、Androidのバージョン、発生した画面と操作手順をお知らせください。バージョンは「遊び方・設定」のCREDITSに表示されます。パスワードや不要な個人情報は送らないでください。</p><h2>文字を入力する</h2><p>端末の設定で日本語キーボードを有効にしてください。隕石の読みをひらがなで入力し、確定または「発射」を押します。漢字に変換する必要はありません。</p><h2>音が聞こえない</h2><p>端末のメディア音量と、ゲームの「遊び方・設定」にあるBGM・効果音の設定をご確認ください。BGM試聴室はメニューから開けます。</p><h2>記録と再開</h2><p>途中のミッション、名言コレクション、最高得点とトップ10、設定を端末に保存します。アプリのストレージ消去やアンインストールで記録が失われます。Androidのバックアップや端末移行については端末の設定をご確認ください。</p><h2>スコア共有</h2><p>競技モードの結果画面で、ゲーム内スコア画像と文章を外部アプリへ渡せます。対応する共有アプリが必要です。投稿を完了する操作は共有先で行います。お子さまは保護者と共有内容・共有先を確認してください。</p><h2>名言・出典の連絡</h2><p>名言の本文、カテゴリ、出典ページ、訂正が必要な点をお知らせください。原著作物の権利に関するご連絡も上記メールで受け付けます。名言の詳細画面から、その出典と解説を確認できます。</p>''','../'))
write(DOC/'.nojekyll','')
# Publication is a separate explicit operation; only retain verification for unchanged pages.
publication_path=STORE/'publication.json'
if publication_path.is_file():
    import hashlib
    publication=json.loads(publication_path.read_text())
    checks=publication.get('checks',[])
    if checks and all(c.get('status')==200 and hashlib.sha256((ROOT/c['localPath']).read_bytes()).hexdigest()==c['sha256'] for c in checks):
        meta.update(publicUrlsVerified=True,githubPagesPublished=True,privacyUrl=meta['privacyUrlPlanned'],websiteUrl=meta['websiteUrlPlanned'],supportUrl='https://hatake716.github.io/rocketboy/support/',publicationRecord='publication.json')
        meta['releaseReviewOpenItems']=[item for item in meta['releaseReviewOpenItems'] if item!='公開Webページの配置とURL検証']
        write(STORE/'submission.json',json.dumps(meta,ensure_ascii=False,indent=2))
lengths={name:len((JA/f'{name}.txt').read_text().strip()) for name in ['title','short-description','full-description','release-notes']}
assert lengths['title']<=30 and lengths['short-description']<=80 and lengths['full-description']<=4000 and lengths['release-notes']<=500
write(STORE/'copy-validation.json',json.dumps(lengths,indent=2))
print(json.dumps({'counts':counts,'lengths':lengths},ensure_ascii=False))
