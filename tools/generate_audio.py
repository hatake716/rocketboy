#!/usr/bin/env python3
"""ROCKETBOY original score: 'ORBIT RUNNER', 168 BPM, E minor, 32 bars.
Two pulse voices, triangle bass and deterministic noise percussion. No samples.
Run with Python 3; outputs reproducible 22050 Hz mono PCM WAV assets.
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
BEAT = 60 / 168
STEP = BEAT / 4
NOTES = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}

def frequency(note):
    if isinstance(note, (int, float)):
        return note
    midi = NOTES[note[0]] + (1 if '#' in note else 0) + 12 * (int(note[-1]) + 1)
    return 440 * 2 ** ((midi - 69) / 12)

def tone(buf, start, duration, note, volume=0.15, voice='pulse', duty=0.25, sweep=0):
    first = round(start * RATE)
    count = round(duration * RATE)
    hz = frequency(note)
    phase = 0.0
    for i in range(count):
        if first + i >= len(buf):
            break
        t = i / RATE
        phase += hz * (1 + sweep * i / count) / RATE
        p = phase % 1
        sample = (1 if p < duty else -1) if voice == 'pulse' else 1 - 4 * abs(p - .5)
        envelope = min(1, t / .003) * min(1, (duration - t) / .012)
        # Gentle decay leaves the pulse articulation audible without a hard click.
        envelope *= .8 + .2 * (1 - i / count)
        buf[first + i] += volume * sample * envelope

def noise(buf, start, duration, volume, seed):
    rng = random.Random(seed)
    first, count = round(start * RATE), round(duration * RATE)
    hold = 0.0
    for i in range(count):
        if first + i >= len(buf):
            break
        if i % 3 == 0:
            hold = rng.choice((-1, 1))
        buf[first + i] += volume * hold * (1 - i / count) ** 2

def write(name, buf):
    peak = max(abs(x) for x in buf) or 1
    gain = min(1, .91 / peak)
    with wave.open(str(OUT / (name + '.wav')), 'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(RATE)
        f.writeframes(b''.join(struct.pack('<h', round(x * gain * 32767)) for x in buf))
    print(f'{name}: {len(buf) / RATE:.2f}s, peak={peak * gain:.3f}')

# Each bar contains eight articulated eighth notes; B section changes contour and register.
melodies = [
    'E5 B5 A5 G5 B5 E6 D6 B5', 'G5 E5 G5 B5 A5 G5 E5 D5',
    'C5 G5 B5 C6 E6 D6 C6 G5', 'D5 A5 F#5 A5 D6 C6 A5 F#5',
    'E5 G5 B5 E6 G6 E6 D6 B5', 'C6 B5 G5 E5 G5 A5 B5 G5',
    'A5 B5 D6 F#6 E6 D6 A5 F#5', 'B5 F#5 D#5 F#5 A5 F#5 D#5 B4',
    'E6 B5 G5 A5 B5 E6 G6 F#6', 'E6 D6 B5 G5 A5 B5 D6 B5',
    'G5 C6 E6 G6 E6 D6 C6 B5', 'A5 D6 F#6 A6 F#6 E6 D6 A5',
    'B5 E6 D6 G6 F#6 E6 B5 G5', 'C6 E6 G6 E6 D6 C6 B5 G5',
    'D6 F#6 A6 F#6 E6 D6 A5 F#5', 'D#6 F#6 B6 A6 F#6 D#6 B5 F#5',
]
roots = ['E3', 'E3', 'C3', 'D3', 'E3', 'C3', 'D3', 'B2']
chords = [('E4', 'G4', 'B4'), ('E4', 'G4', 'B4'), ('C4', 'E4', 'G4'), ('D4', 'F#4', 'A4'),
          ('E4', 'G4', 'B4'), ('C4', 'E4', 'G4'), ('D4', 'F#4', 'A4'), ('B3', 'D#4', 'F#4')]
buf = [0.0] * round(32 * 4 * BEAT * RATE)
for bar in range(32):
    start = bar * 4 * BEAT
    phrase = melodies[(bar % 16 if bar < 16 else (bar + 8) % 16)].split()
    for n, note in enumerate(phrase):
        tone(buf, start + n * STEP * 2, STEP * 1.65, note, .15, duty=.125 if bar >= 16 else .25)
    for n in range(16):
        tone(buf, start + n * STEP, STEP * .70, chords[bar % 8][n % 3], .045, duty=.5)
    for n in range(8):
        tone(buf, start + n * STEP * 2, STEP * 1.7, frequency(roots[bar % 8]) * (2 if n in (3, 7) else 1), .23, voice='triangle')
    for n in range(8):
        noise(buf, start + n * BEAT / 2, .028, .048, bar * 16 + n)
    for n in (0, 2):
        tone(buf, start + n * BEAT, .12, 125, .29, voice='triangle', sweep=-.74)
    for n in (1, 3):
        noise(buf, start + n * BEAT, .12, .18, 1100 + bar * 4 + n)
    if bar % 8 == 7:
        for n in (13, 14, 15):
            noise(buf, start + n * STEP, .065, .12, n + bar)
write('orbit_runner', buf)

buf = [0.0] * int(.20 * RATE)
tone(buf, 0, .17, 300, .30, duty=.125, sweep=4)
write('launch', buf)
buf = [0.0] * int(.36 * RATE)
for i, note in enumerate(['E6', 'B6', 'E7']):
    tone(buf, i * .055, .055, note, .24, duty=.25)
noise(buf, .08, .20, .25, 7)
write('intercept', buf)
buf = [0.0] * int(.6 * RATE)
noise(buf, 0, .50, .45, 42)
tone(buf, 0, .42, 170, .25, voice='triangle', sweep=-.8)
write('impact', buf)
buf = [0.0] * int(1.6 * RATE)
for i, note in enumerate(['E5', 'D5', 'B4', 'G4', 'E4', 'B3', 'E3']):
    tone(buf, i * .18, .22, note, .20)
write('game_over', buf)
buf = [0.0] * int(1.1 * RATE)
for i, note in enumerate(['E5', 'G5', 'B5', 'E6', 'G6', 'B6', 'E7']):
    tone(buf, i * .11, .13, note, .22)
write('rank_up', buf)
