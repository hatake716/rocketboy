#!/usr/bin/env python3
"""Rebuild the 30 s portrait trailer from emulator recordings and the game's own audio.
Requires Pillow and FFmpeg. Run from any directory; outputs stay in artifacts/promo-30s.
"""
from pathlib import Path
import subprocess, json, math, random, hashlib
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'artifacts/promo-30s'
SOURCE = OUT / 'source'
LAYERS = OUT / 'layers'
RENDERS = OUT / 'segments'
FONT = ROOT / 'app/src/main/res/font/dot_gothic.ttf'
INK, DARK, MID, LIGHT = '#101e19', '#27412f', '#739453', '#d4ed9b'
SCENES = [
    dict(id='01-hook', source='title', start=.15, duration=2.5, hero=True, label='EARTH DEFENSE / 01', title=['その一文字が、','地球を救う。'], sub='日本語タイピング × 地球防衛', foot='TYPE. LAUNCH. SAVE THE WORLD.'),
    dict(id='02-type', source='typing', start=.25, duration=5.5, label='01 / TYPE & INTERCEPT', title=['打つ。飛ばす。','撃ち落とす。'], sub='ひらがなを確定すると、ロケット発射。', foot='スマホのキーボードが、地球の武器になる。'),
    dict(id='03-quote', source='quote', start=.30, duration=5, label='02 / QUOTATION BURST', title=['名言を、','撃ち抜け。'], sub='長い言葉ほど、大きなスコアに。', foot='名言は 1文字 × 300点 ＋ ボーナス'),
    dict(id='04-collect', source='collection', start=.10, duration=3, label='03 / WORD COLLECTION', title=['撃ち抜いた言葉が、','コレクションに。'], sub='6カテゴリの名言に、解説付き。', foot='歴史・経営者・アニメ漫画・ゲーム・ことわざ・百人一首'),
    dict(id='05-compete', source='competitive', start=0, duration=5, label='04 / COMPETITIVE MODE', title=['一度のミスで、','終わる。'], sub='残機 1 ／ 落下スピード 1.5倍', foot='いつものタイピングに、緊張感を。'),
    dict(id='06-share', source='competitive', start=4.05, duration=2, label='05 / SHARE YOUR SCORE', title=['その記録を、','Xへ。'], sub='スコア画像 ＋ #rocketboy', foot='競技の結果を、画像付きで共有。'),
    dict(id='07-music', source='music', start=.05, duration=3, label='06 / CHIPTUNE ADVENTURE', title=['宇宙を駆ける、','26曲の電子音。'], sub='レトロなドット絵と、ゲームボーイ風BGM。', foot='天体25曲 ＋ 訓練生のテーマ ／ 自由に試聴'),
    dict(id='08-end', source='title', start=.2, duration=4, hero=True, label='YOUR NEXT MISSION', title=['目指せ、','冥王星ランク。'], sub='一文字ずつ、宇宙の先へ。', foot='Android / 日本語タイピングゲーム'),
]

def run(args, logfile=None):
    with (open(logfile, 'w') if logfile else open('/dev/null', 'w')) as log:
        subprocess.run([str(a) for a in args], check=True, stdout=log, stderr=subprocess.STDOUT)

def layer(scene, index):
    canvas = Image.new('RGB', (360,640), INK)
    d = ImageDraw.Draw(canvas); d.fontmode = '1'
    rng = random.Random(716)
    for _ in range(110):
        x,y = rng.randrange(8,352), rng.randrange(42,632)
        d.rectangle((x,y,x+1,y+1), fill=DARK)
    # Pixel-stepped orbits and navigation marks share the game's own green palette.
    for i in range(130):
        a = i * math.pi / 65
        x,y = int(180+174*math.cos(a)), int(355+236*math.sin(a))
        if i%3 == 0: d.rectangle((x,y,x+1,y+1), fill=DARK)
    def txt(value, y, size, color=LIGHT, center=True, x=180):
        font=ImageFont.truetype(str(FONT), size)
        width=d.textlength(value,font=font)
        assert width<=328, (scene['id'],value,width)
        d.text((x,y),value,font=font,fill=color,anchor='mm' if center else 'lm')
    d.line((18,35,342,35), fill=MID, width=1)
    txt('ROCKETBOY',20,12,center=False,x=18)
    txt('ANDROID / TYPING',21,8,MID,center=False,x=235)
    txt(scene['title'][0],63,28 if len(scene['title'][0])>9 else 32)
    txt(scene['title'][1],100,32)
    txt(scene['sub'],129,10,MID)
    if scene.get('hero'):
        if index == 0:
            txt('スマホで守れ、この星を。',490,18)
            d.rectangle((46,523,314,564),fill=LIGHT)
            txt('ROCKET READY',544,20,INK)
            txt('RETRO LOOK. NEW MISSION.',586,10,MID)
        else:
            txt('君の一文字で、地球を救え。',490,16)
            d.rectangle((46,522,314,565),fill=LIGHT)
            txt('#rocketboy',545,24,INK)
            txt('ROCKETBOY',585,20)
    else:
        x,y,w,h=48,147,264,440
        d.rectangle((x-3,y-3,x+w+2,y+h+2), outline=DARK, width=1)
        for px,py,sx,sy in [(x-4,y-4,1,1),(x+w+3,y-4,-1,1),(x-4,y+h+3,1,-1),(x+w+3,y+h+3,-1,-1)]:
            d.line((px,py,px+sx*12,py),fill=LIGHT,width=2)
            d.line((px,py,px,py+sy*12),fill=LIGHT,width=2)
        txt(scene['foot'],609,8 if len(scene['foot'])>35 else 10,MID)
    txt(scene['label'],scene.get('hero') and 610 or 629,8,MID)
    # The demonstrations are staged in the real renderer, not fabricated game screenshots.
    if scene.get('hero'):
        txt(scene['foot'],632,8,MID)
    else:
        txt('DEMO',136,6,MID,center=False,x=297)
    path=LAYERS/f"{scene['id']}.png"
    canvas.resize((1080,1920),Image.Resampling.NEAREST).save(path)
    return path

def render_scene(scene,index):
    bg=layer(scene,index)
    bounds=json.loads((SOURCE/'promo/title-bounds.json').read_text())
    if scene.get('hero'):
        crop=f"crop={bounds['width']}:{bounds['height']}:{bounds['x']}:{bounds['y']}"
        w=936; h=round(bounds['height']*w/bounds['width']/2)*2
        x,y=72,450
    else:
        crop='crop=1080:1800:0:72'; w,h=792,1320; x,y=144,441
    # Extend a static Android screen's last encoded frame to the requested edit duration.
    clip=(f"[1:v]trim=start={scene['start']},setpts=PTS-STARTPTS,{crop},"
          f"scale={w}:{h}:flags=neighbor,setsar=1,fps=30,tpad=stop_mode=clone:stop_duration=10,"
          f"trim=duration={scene['duration']}[clip];"
          f"[0:v][clip]overlay=x={x}:y={y}:shortest=1")
    if index > 0: clip+=",fade=t=in:st=0:d=0.066667"
    if index==len(SCENES)-1:clip+=f",fade=t=out:st={scene['duration']-.25}:d=0.25"
    clip+=',format=yuv420p[v]'
    target=RENDERS/f"{scene['id']}.mp4"
    run(['ffmpeg','-y','-hide_banner','-loglevel','warning','-loop','1','-framerate','30','-i',bg,
         '-i',SOURCE/f"{scene['source']}.mp4",'-filter_complex_threads','2','-filter_complex',clip,
         '-map','[v]','-an','-frames:v',round(scene['duration']*30),'-r','30','-c:v','libx264',
         '-preset','medium','-crf','18','-threads','4','-movflags','+faststart',target],OUT/f"{scene['id']}-render.log")
    print(scene['id'],'rendered',flush=True)
    return target

def soundtrack():
    captured={s['name']:s['cues'] for s in json.loads((SOURCE/'promo/capture.json').read_text())}
    effects=[]; timeline=0
    delays={'typing':[.240,.220,.160], 'quote':[.115], 'competitive':[.200]}
    for scene in SCENES:
        if scene['id'] in ['02-type','03-quote','05-compete']:
            launch_index=0
            for cue in captured[scene['source']]:
                kind=cue['event']
                if kind not in ['launch','intercept','game-over']:continue
                seconds=cue['seconds']-.10 # Screenrecord's startup versus the first encoded frame.
                if kind=='launch':
                    seconds-=delays[scene['source']][launch_index];launch_index+=1
                local=seconds-scene['start']
                if 0<=local<scene['duration']:
                    effects.append((timeline+local, {'launch':'launch','intercept':'intercept','game-over':'impact'}[kind]))
        timeline+=scene['duration']
    effects.append((26.05,'rank_up'))
    args=['ffmpeg','-y','-hide_banner','-loglevel','warning','-i',ROOT/'app/src/main/res/raw/bgm_earth.ogg']
    for _,name in effects:args+=['-i',ROOT/f'app/src/main/res/raw/{name}.wav']
    filters=['[0:a]atrim=0:30,asetpts=PTS-STARTPTS,aresample=48000,volume=.9,afade=t=in:st=0:d=0.08,afade=t=out:st=28.5:d=1.5[music]']
    for i,(seconds,name) in enumerate(effects,1):
        volume=.65 if name=='intercept' else .5
        filters.append(f'[{i}:a]aresample=48000,volume={volume},adelay={round(seconds*1000)}:all=1[s{i}]')
    channels='[music]'+''.join(f'[s{i}]' for i in range(1,len(effects)+1))
    filters.append(channels+f'amix=inputs={len(effects)+1}:normalize=0:duration=first,alimiter=limit=.95:level=false,loudnorm=I=-15:TP=-1.5:LRA=8,aresample=48000[a]')
    args+=['-filter_complex',';'.join(filters),'-map','[a]','-t','30','-ac','2','-c:a','pcm_s24le',OUT/'soundtrack.wav']
    run(args,OUT/'audio-render.log')
    (OUT/'sound-cues.json').write_text(json.dumps(effects,indent=2)+'\n')

def main():
    LAYERS.mkdir(parents=True,exist_ok=True);RENDERS.mkdir(parents=True,exist_ok=True)
    assert sum(s['duration'] for s in SCENES)==30
    outputs=[(RENDERS/f"{s['id']}.mp4") if '--reuse-segments' in __import__('sys').argv and (RENDERS/f"{s['id']}.mp4").is_file() else render_scene(s,i) for i,s in enumerate(SCENES)]
    listing=OUT/'concat.txt'
    listing.write_text(''.join("file '"+str(p)+"'\n" for p in outputs))
    run(['ffmpeg','-y','-hide_banner','-loglevel','warning','-f','concat','-safe','0','-i',listing,
         '-c','copy',OUT/'picture.mp4'],OUT/'concat.log')
    soundtrack()
    final=OUT/'ROCKETBOY-30s-Short-1080x1920.mp4'
    run(['ffmpeg','-y','-hide_banner','-loglevel','warning','-i',OUT/'picture.mp4','-i',OUT/'soundtrack.wav',
         '-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','192k','-ar','48000',
         '-t','30','-movflags','+faststart','-metadata','title=ROCKETBOY｜その一文字が、地球を救う。',
         '-metadata','comment=ROCKETBOY 1.4.0 emulator demo footage. Original in-game music and effects.',final],OUT/'mux.log')
    (OUT/'edit.json').write_text(json.dumps({'duration':30,'canvas':[1080,1920],'fps':30,'scenes':SCENES,
        'music':'地球 / 青い星からの出航 (172 BPM)','source':'ROCKETBOY 1.4.0 Android emulator capture',
        'sha256':hashlib.sha256(final.read_bytes()).hexdigest()},ensure_ascii=False,indent=2)+'\n')
    print(final,flush=True)

if __name__=='__main__':main()
