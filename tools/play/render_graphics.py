#!/usr/bin/env python3
"""Draw store graphics from ROCKETBOY's original vector and pixel glyph definitions."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import re,random,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]; OUT=ROOT/'docs/store/ja-JP/images';OUT.mkdir(parents=True,exist_ok=True)
INK,DARK,MID,LIGHT='#101e19','#27412f','#739453','#d4ed9b'
FONT=ROOT/'app/src/main/res/font/dot_gothic.ttf'
ns='{http://schemas.android.com/apk/res/android}'
paths=ET.parse(ROOT/'app/src/main/res/drawable/rocket_icon.xml').getroot()
def shape(draw,path,fill,scale=1,dx=0,dy=0):
    parts=re.findall(r'[MmHhVvZz]|-?\d+(?:\.\d+)?',path);i=0;x=y=0;points=[];cmd=''
    def finish():
        if points:draw.polygon([(round(dx+a*scale),round(dy+b*scale)) for a,b in points],fill=fill)
    while i<len(parts):
        if parts[i].isalpha():cmd=parts[i];i+=1
        if cmd in 'Zz':finish();points=[];cmd='';continue
        n=float(parts[i]);i+=1
        if cmd in 'Mm':
            y2=float(parts[i]);i+=1
            if points:finish();points=[]
            if cmd=='M':x,y=n,y2
            else:x,y=x+n,y+y2
        elif cmd=='h':x+=n
        elif cmd=='H':x=n
        elif cmd=='v':y+=n
        elif cmd=='V':y=n
        points.append((x,y))
    finish()
icon=Image.new('RGBA',(512,512),INK);d=ImageDraw.Draw(icon)
for p in paths:shape(d,p.get(ns+'pathData'),p.get(ns+'fillColor'),512/108)
icon.save(OUT/'icon-512.png')
source=(ROOT/'app/src/main/java/io/github/hatake716/rocketboy/PixelScene.kt').read_text()
glyphs={letter:re.findall(r'"([01]+)"',body) for letter,body in re.findall(r"'(.)' to listOf\((.*?)\)",source)}
img=Image.new('RGB',(512,250),INK);d=ImageDraw.Draw(img)
rng=random.Random(716)
for i in range(100):
 x,y=rng.randrange(512),rng.randrange(250);s=2 if i%7==0 else 1
 d.rectangle((x,y,x+s-1,y+s-1),fill=MID if i%7==0 else DARK)
def pixel(text,x,y,size,color):
 for j,ch in enumerate(text):
  for row,line in enumerate(glyphs[ch]):
   for col,v in enumerate(line):
    if v=='1':d.rectangle((x+(j*6+col)*size,y+row*size,x+(j*6+col+1)*size-1,y+(row+1)*size-1),fill=color)
def txt(s,x,y,size,color=LIGHT):d.text((x,y),s,font=ImageFont.truetype(str(FONT),size),fill=color,stroke_width=0)
# The title mirrors the in-game pixel lettering. All art below uses the same four-color palette.
pixel('ROCKET',37,62,7,DARK);pixel('ROCKET',34,59,7,LIGHT)
pixel('BOY',163,123,7,DARK);pixel('BOY',160,120,7,LIGHT)
txt('その一文字が、地球を救う。',35,183,16,LIGHT)
txt('HIRAGANA × ORBITAL DEFENSE',35,219,8,MID)
# A stepped planetary horizon and an ascending rocket, with hiragana meteors.
d.ellipse((303,191,541,429),fill=MID);d.ellipse((309,198,535,424),fill=DARK)
for r in [(336,208,358,218),(347,218,384,236),(379,238,401,259),(433,204,461,218),(447,218,479,230)]:d.rectangle(r,fill=MID)
for y in range(136,193,9):d.rectangle((384,y,386,y+4),fill=MID)
for p in list(paths)[1:]:shape(d,p.get(ns+'pathData'),p.get(ns+'fillColor'),.8,343,62)
txt('ほ',306,35,25);txt('し',446,83,23)
for x,y in [(317,23),(311,16),(451,68),(444,61)]:d.rectangle((x,y,x+2,y+5),fill=MID)
d.rectangle((22,22,36,23),fill=MID);d.rectangle((22,22,23,36),fill=MID)
d.rectangle((476,226,490,227),fill=MID);d.rectangle((489,212,490,227),fill=MID)
img.resize((1024,500),Image.Resampling.NEAREST).save(OUT/'feature-graphic.png')
print('Rendered 512 RGBA icon and 1024x500 RGB feature graphic.')
