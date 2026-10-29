#!/usr/bin/env python3
"""Original four-voice GB-style soundtrack. Python 3 + NumPy + FFmpeg.
Two pulse voices, 32-sample/4-bit wave RAM, one shared LFSR percussion voice.
GB period quantization and legal duties; not a cycle-accurate APU emulator.
Per-note ramps and an output low-pass tame clicks/aliasing for phone playback.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import wave
import numpy as np
from scorebook import CHORDS, FORM, MINOR, scores

ROOT = Path(__file__).resolve().parents[2]
RATE = 44100
OVERSAMPLE = 2
BARS = 64
# Each world has its own arpeggio contour, bass accents, and kick pattern.
# Arpeggio numbers index root / third / fifth / upper root.
GROOVES = {
 'spark':      ('01232102', '00207040', [0,6,8,11]),
 'mist':       ('02123210', '00074070', [0,7,10]),
 'sail':       ('01212321', '00704070', [0,6,8,14]),
 'hop':        ('03021201', '0-70-470', [0,6,10]),
 'march':      ('02020102', '00007040', [0,2,8,10]),
 'chase':      ('01023212', '00700074', [0,3,8,11,14]),
 'skip':       ('10302012', '0-407-40', [0,7,10,14]),
 'stone':      ('01202120', '00400070', [0,8,11]),
 'storm':      ('03210232', '00047040', [0,3,8,10,14]),
 'eruption':   ('01230123', '07000704', [0,2,6,8,11,14]),
 'crystal':    ('32101203', '0-704-70', [0,8,14]),
 'fortress':   ('02031302', '00700470', [0,6,8,10]),
 'echo':       ('02103012', '000-4070', [0,8,10]),
 'orbit':      ('01232123', '07407040', [0,6,8,11]),
 'fountain':   ('00122332', '00700740', [0,3,8,14]),
 'frost':      ('10212032', '00-470-4', [0,7,8,14]),
 'haze':       ('21012302', '00074004', [0,6,10]),
 'contrast':   ('01013232', '00-07740', [0,3,8,11]),
 'tilt':       ('01201232', '00700704', [0,3,6,8,11,14]),
 'slalom':     ('02132312', '04070740', [0,3,8,14]),
 'crown':      ('00112232', '00407740', [0,6,8,11]),
 'beacon':     ('02013021', '000-0740', [0,8,14]),
 'gale':       ('01233210', '00704074', [0,3,6,8,11,14]),
 'retrograde': ('32102120', '07400740', [0,6,8,10]),
 'horizon':    ('01203212', '00074070', [0,6,8,14]),
}
LIGHT = {'mist','crystal','echo','haze','beacon'}
BOUNCE = {'hop','skip','frost','slalom'}
HEAVY = {'march','storm','eruption','fortress','crown','gale'}


def lfsr(width):
    state = (1 << width) - 1
    result = []
    for _ in range((1 << width) - 1):
        result.append(1.0 if state & 1 else -1.0)
        state = (state >> 1) | (((state ^ (state >> 1)) & 1) << (width - 1))
    return np.array(result, dtype=np.float32)


NOISE = {7: lfsr(7), 15: lfsr(15)}
# Valid CH4 clocks = 262144 / divider / 2**shift (divider zero represents 0.5).
NOISE_CLOCKS = np.array(sorted({262144 / d / (2**s) for d in [.5,1,2,3,4,5,6,7] for s in range(14)}))


def wave_ram(groove):
    triangle = np.r_[np.arange(16), np.arange(15, -1, -1)]
    if groove in LIGHT:
        table = np.round(7.5 + 7.5 * np.sin(2*np.pi*np.arange(32)/32))
    elif groove in HEAVY:
        table = np.round(.8*triangle + .2*np.roll(triangle, 7))
    else:
        table = triangle
    assert len(table) == 32 and np.all((0 <= table) & (table <= 15))
    return table.astype(int)


def compose(score):
    """Make a deterministic note-event score; starts/lengths are sixteenth ticks."""
    events, harmony = [], []
    groove = score['groove']
    arp, bass, kicks = GROOVES[groove]

    def midi(degree, octave=0):
        return score['tonic'] + 12*(degree//7 + octave) + MINOR[degree % 7]

    def emit(ch, start, length, pitch, volume, duty=.25, gate=.9):
        events.append(dict(ch=ch, start=start, length=length, pitch=pitch,
                           volume=volume, duty=duty, gate=gate))

    for bar in range(BARS):
        section = bar // 8
        theme, development = FORM[section]
        local = bar % 4
        root, roman = CHORDS[local]
        # F-G-A bass movement in A minor, rather than dropping A an octave.
        voiced_root = root or 7
        chord = [voiced_root, voiced_root+2, voiced_root+4, voiced_root+7]
        harmony.append(dict(bar=bar+1, roman=roman, root_midi=midi(voiced_root,-2)))
        melody = list(score['themes'][ord(theme)-ord('A')][local])
        # Repeat the hook with a different last response, not unrelated pitches.
        if bar % 8 >= 4 and local == 3:
            last, length = melody[-1]
            if last is not None:
                melody[-1] = (7 if theme == 'B' else 0, length)
        if development == 1:
            # A short diatonic pickup at the end of held tones; the beginning of
            # each phrase retains its identity and its written strong-beat notes.
            new = []
            for j, (degree, length) in enumerate(melody):
                if degree is not None and length >= 6 and j < len(melody)-1:
                    next_degree = melody[j+1][0]
                    neighbour = degree + (1 if next_degree is not None and next_degree > degree else -1)
                    new.extend([(degree,length-1),(neighbour,1)])
                else:
                    new.append((degree,length))
            melody = new
        lead_shift = 7 if section == 6 else 0
        if bar == 63:
            # An open fifth pickup anticipates the first bVI without a fade-out.
            melody = [(7,4),(4,4),(2,4),(4,2),(6,2)]
        duty = score['duty']
        if theme == 'C': duty = .125 if duty == .5 else .5
        lead_level = .235 if theme == 'B' or section == 6 else .205
        if theme == 'C': lead_level *= .87
        pos = bar*16
        for degree, length in melody:
            if degree is not None:
                emit(0,pos,length,midi(degree+lead_shift),lead_level,duty,
                     .77 if groove in BOUNCE else .94 if length >= 4 else .86)
            pos += length
        assert pos == (bar+1)*16

        # Pulse 2: arpeggios in A; quieter broken chords/answer figures in B;
        # spacious two-note calls in C. It always remains a single voice.
        grid = [0,2,4,6,8,10,12,14]
        if groove == 'tilt': grid = [0,3,6,8,11,14]
        if theme == 'C': grid = [0,4,8,12] if bar%2==0 else [2,6,10,14]
        elif groove in BOUNCE and section in (0,1): grid = [2,6,10,14]
        elif groove in LIGHT and section == 0: grid = [0,4,8,12]
        for j, p in enumerate(grid):
            next_p = grid[j+1] if j+1 < len(grid) else 16
            ix = int(arp[(j+(2 if theme=='B' else 0)) % len(arp)])
            degree = chord[ix]
            emit(1,bar*16+p,next_p-p,midi(degree,-1),.098 if theme=='C' else .086,
                 .5 if theme=='B' else .25,.58 if groove in BOUNCE else .76)

        # Wave RAM bass, with written accents rather than a root repeated every tick.
        bass_grid = [0,2,4,6,8,10,12,14]
        if groove == 'tilt': bass_grid = [0,3,6,8,11,14]
        if theme == 'C' and section == 4: bass_grid = [0,4,8,12]
        for j, p in enumerate(bass_grid):
            tone = bass[j % len(bass)]
            if tone == '-': continue
            next_p = bass_grid[j+1] if j+1 < len(bass_grid) else 16
            degree = voiced_root + int(tone)
            # Keep the root at every chord change, making the progression audible.
            if p == 0: degree = voiced_root
            emit(2,bar*16+p,next_p-p,midi(degree,-2),.37 if groove in HEAVY else .33,
                 gate=.82 if groove in BOUNCE else .92)

        # Hats, kick and snare share CH4 with strict priority; no hidden fifth voice.
        hits = {p:'hat' for p in range(0,16,2)}
        hits.update({p:'kick' for p in kicks})
        hits.update({4:'snare',12:'snare'})
        if theme == 'C' and section == 4:
            hits = {0:'kick',4:'hat',8:'snare',12:'hat',14:'hat'}
        if bar % 8 == 7:
            hits.update({12:'snare',14:'snare',15:'snare'})
        positions = sorted(hits)
        for j,p in enumerate(positions):
            kind = hits[p]
            next_p = positions[j+1] if j+1<len(positions) else 16
            length = min(next_p-p,.55 if kind=='hat' else 1.15)
            level = {'kick':.32,'snare':.19,'hat':.055}[kind]
            if theme=='C': level *= .75
            emit(3,bar*16+p,length,kind,level,gate=1)

    # Verify event intervals, including silent sample crossings that an audio-array
    # overlap check could miss. At most one active event per physical-style voice.
    for ch in range(4):
        voice = sorted((e for e in events if e['ch']==ch),key=lambda e:e['start'])
        end = 0
        for e in voice:
            assert e['start'] >= end-1e-9, (score['id'],ch,e)
            assert e['length'] > 0 and 0 < e['gate'] <= 1
            end = e['start']+e['length']*e['gate']
        assert end <= BARS*16
    return events, harmony


def render(score, export_dir, package=True):
    step = 60/score['bpm']/4
    n = round(BARS*16*step*RATE)
    voices = np.zeros((4,n),dtype=np.float32)
    events, harmony = compose(score)
    table = wave_ram(score['groove'])/7.5-1
    for e in events:
        ch = e['ch']
        a = round(e['start']*step*RATE)
        b = min(n,round((e['start']+e['length']*e['gate'])*step*RATE))
        count = b-a
        if count <= 0: continue
        t = np.arange(count*OVERSAMPLE)/(RATE*OVERSAMPLE)
        duration = count/RATE
        if ch == 3:
            kind = e['pitch']
            width = 7 if kind=='kick' else 15
            clock = {'kick':1800,'snare':16384,'hat':32768}[kind]
            target = clock*np.exp(-np.floor(t*128)/128*24) if kind=='kick' else np.full(len(t),clock)
            upper = np.clip(np.searchsorted(NOISE_CLOCKS,target),1,len(NOISE_CLOCKS)-1)
            indices = np.where(target-NOISE_CLOCKS[upper-1] < NOISE_CLOCKS[upper]-target,upper-1,upper)
            ix = np.cumsum(NOISE_CLOCKS[indices]/(RATE*OVERSAMPLE)).astype(int) % len(NOISE[width])
            raw = NOISE[width][ix]
            env = np.floor(15*(np.maximum(0,1-t/duration)**(2.4 if kind=='hat' else 1.5)))/15
        else:
            hz = 440*2**((e['pitch']-69)/12)
            # Timer formula from Pan Docs; stepped vibrato on longer lead notes.
            cents = 8*np.sin(2*np.pi*5.4*np.floor(t*64)/64)*np.clip((t-.10)/.08,0,1) if ch==0 and e['length']>=4 else 0
            target = hz*2**(cents/1200)
            clock = 65536 if ch==2 else 131072
            period = np.clip(np.round(2048-clock/target),0,2047)
            frequency = clock/(2048-period)
            if np.ndim(frequency)==0: phase = t*frequency
            else: phase = np.cumsum(frequency/(RATE*OVERSAMPLE))
            if ch == 2:
                raw = table[np.floor((phase%1)*32).astype(int)]
                env = np.ones_like(t)
            else:
                raw = np.where(phase%1<e['duty'],1.,-1.)-(2*e['duty']-1)
                # Discrete 4-bit envelope, updated at a 64-Hz frame rate.
                env = np.maximum(6,15-np.floor(np.floor(t*64)/max(2,round(duration*64/7))))/15
        edge = np.minimum(1,t/.001)*np.minimum(1,(duration-t)/.003)
        sample = (raw*env*edge*e['volume']).reshape(count,OVERSAMPLE).mean(axis=1)
        voices[ch,a:b] = sample
    mix = voices.sum(axis=0)
    # Filter a periodic extension so filter state is settled at the loop boundary.
    guard = RATE//8
    padded = np.concatenate([mix[-guard:],mix,mix[:guard]]).astype('<f4')
    proc = subprocess.run(['ffmpeg','-v','error','-f','f32le','-ar',str(RATE),'-ac','1','-i','pipe:0',
        '-af','highpass=f=30,lowpass=f=7200','-f','f32le','pipe:1'],input=padded.tobytes(),stdout=subprocess.PIPE,check=True)
    filtered = np.frombuffer(proc.stdout,dtype='<f4')[guard:guard+n].copy()
    rms = np.sqrt(np.mean(filtered**2))
    # Gentle output saturation catches coincident pulse transients without making
    # sparse/bouncy tracks much quieter than sustained ones. Four source voices.
    mix = .86*np.tanh(filtered*(.20/rms)/.86)
    pcm = np.round(mix*32767).astype('<i2')
    wav = export_dir/f"{score['rank']:02d}-{score['id']}.wav"
    with wave.open(str(wav),'wb') as out:
        out.setnchannels(1);out.setsampwidth(2);out.setframerate(RATE);out.writeframes(pcm.tobytes())
    dest = (ROOT/'app/src/main/res/raw' if package else export_dir)/f"bgm_{score['id']}.ogg"
    subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-map_metadata','-1','-c:a','libvorbis','-q:a','5',
        '-fflags','+bitexact','-flags:a','+bitexact',str(dest)],check=True)
    decoded = subprocess.check_output(['ffmpeg','-v','error','-i',str(dest),'-f','f32le','-ac','1','-ar',str(RATE),'-'])
    d = np.frombuffer(decoded,dtype='<f4')
    assert abs(len(d)-n)<RATE*.02 and np.isfinite(d).all()
    assert np.max(np.abs(d))<.98 and .15<np.sqrt(np.mean(d*d))<.24, (
        score['id'], float(np.max(np.abs(d))), float(np.sqrt(np.mean(d*d))))
    assert np.sqrt(np.mean(d[:RATE]**2))>.05 and np.sqrt(np.mean(d[-RATE:]**2))>.05
    assert abs(d[0]-d[-1])<.08, 'Loop click'
    metadata = {k:v for k,v in score.items() if k!='themes'}
    metadata.update(bars=BARS,meter='4/4',progression=['bVI','bVII','i','i'],form=[f'{t}{v}' for t,v in FORM],
        duration_seconds=round(n/RATE,6),sample_rate=RATE,channels=1,resource=dest.stem,
        pcm_sha256=hashlib.sha256(pcm.tobytes()).hexdigest(),ogg_sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),
        score_sha256=hashlib.sha256(json.dumps(score,sort_keys=True).encode()).hexdigest(),bytes=dest.stat().st_size,
        decoded_peak=round(float(np.max(np.abs(d))),4),decoded_rms=round(float(np.sqrt(np.mean(d*d))),4),
        loop_edge_jump=round(float(abs(d[0]-d[-1])),6),events=len(events),wave_ram=wave_ram(score['groove']).tolist())
    (export_dir/f"{score['rank']:02d}-{score['id']}.events.json").write_text(json.dumps(dict(harmony=harmony,events=events),indent=2)+'\n')
    print(f"{score['rank']:02} {score['id']:12} {n/RATE:6.2f}s {len(events)} events peak {metadata['decoded_peak']} rms {metadata['decoded_rms']}",flush=True)
    return metadata


if __name__=='__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--export-dir',type=Path,default=ROOT/'artifacts/1.3.1/soundtrack')
    ap.add_argument('--only',nargs='+',help='Preview selected worlds only in the export directory')
    args=ap.parse_args();args.export_dir.mkdir(parents=True,exist_ok=True)
    chosen=[s for s in scores() if args.only is None or s['id'] in args.only]
    if args.only:
        unknown = set(args.only)-{s['id'] for s in scores()}
        if unknown: ap.error('Unknown worlds: '+', '.join(sorted(unknown)))
    tracks=[render(s,args.export_dir,package=args.only is None) for s in chosen]
    if args.only is None:
        assert len({s['pcm_sha256'] for s in tracks})==25
        (ROOT/'data/music/tracks.json').write_text(json.dumps(dict(title='CELESTIAL ADVENTURE',version='1.3.1',
            license='MIT',progression_reference='https://harimusic.net/pdf/trial002.pdf#page=6',
            synthesis='GB-style four-voice software synthesis, not a cycle-accurate hardware recording',
            tracks=tracks),ensure_ascii=False,indent=2)+'\n')
