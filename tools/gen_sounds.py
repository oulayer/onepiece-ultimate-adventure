#!/usr/bin/env python3
"""Procedurally synthesises the mod's placeholder sound effects (mono 44.1 kHz Ogg Vorbis).

Every sound event has its own file so a resource pack can replace them one by one.
Run from the repository root:  python3 tools/gen_sounds.py   (needs numpy and ffmpeg)
"""
import json, os, subprocess, tempfile, wave
import numpy as np

SR = 44100
OUT = "src/main/resources/assets/onepiece/sounds"
rng = np.random.default_rng(7)

def t(d): return np.arange(int(SR * d)) / SR
def env(n, a=0.005, r=0.1, curve=4.0):
    x = np.linspace(0, 1, n)
    att = np.clip(x * (n / SR) / max(a, 1e-4), 0, 1)
    dec = np.exp(-curve * x)
    return att * dec
def noise(d): return rng.standard_normal(int(SR * d))
def lowpass(x, cutoff):
    # one-pole low-pass, cutoff may be an array (sweeps)
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x); acc = 0.0
    for i in range(len(x)):
        acc = (1 - a[i]) * x[i] + a[i] * acc
        y[i] = acc
    return y
def highpass(x, cutoff): return x - lowpass(x, cutoff)
def sine_sweep(f0, f1, d, curve=1.0):
    tt = t(d); k = (tt / d) ** curve
    f = f0 + (f1 - f0) * k
    return np.sin(2 * np.pi * np.cumsum(f) / SR)
def norm(x, peak=0.9): return x / (np.max(np.abs(x)) + 1e-9) * peak

def whoosh(d, lo, hi, peak=0.8):
    n = noise(d); tt = t(d)
    sweep = lo + (hi - lo) * np.sin(np.pi * tt / d)
    shaped = lowpass(highpass(n, lo * 0.3), sweep)
    return norm(shaped * np.sin(np.pi * tt / d) ** 1.5, peak)

def thump(freq, d, click=0.4, peak=0.95):
    tt = t(d)
    body = np.sin(2 * np.pi * freq * tt * (1 - 0.35 * tt / d)) * np.exp(-tt * 18 / d)
    c = lowpass(noise(d), 3500) * np.exp(-tt * 140) * click
    return norm(np.tanh(2.2 * (body + c)), peak)

SOUNDS = {
    "combat/swing_light": lambda: whoosh(0.16, 600, 3500, 0.6),
    "combat/swing_heavy": lambda: whoosh(0.32, 250, 1800, 0.8),
    "combat/hit_light": lambda: thump(140, 0.14, 0.7),
    "combat/hit_heavy": lambda: norm(thump(70, 0.42, 1.0) + 0.35 * lowpass(noise(0.42), 900) * np.exp(-t(0.42) * 9)),
    "combat/block": lambda: norm(thump(220, 0.12, 1.2) + 0.2 * np.sin(2 * np.pi * 900 * t(0.12)) * np.exp(-t(0.12) * 50)),
    "combat/parry": lambda: norm(sum(np.sin(2 * np.pi * f * t(0.6)) * np.exp(-t(0.6) * k) for f, k in
                                     [(1180, 6), (1870, 8), (2630, 11), (3410, 14)]) + 0.6 * highpass(noise(0.6), 3000) * np.exp(-t(0.6) * 60), 0.7),
    "combat/dash": lambda: whoosh(0.22, 900, 5000, 0.6),
    "combat/slam": lambda: norm(np.tanh(3 * (sine_sweep(90, 32, 0.9, 0.5) * np.exp(-t(0.9) * 4)))
                                + 0.5 * lowpass(noise(0.9), 500) * np.exp(-t(0.9) * 3.5)),
    "combat/charge": lambda: norm(sine_sweep(180, 620, 0.55, 1.4) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * t(0.55)))
                                  * np.sin(np.pi * t(0.55) / 0.55) ** 0.5, 0.45),
    "gomu/stretch": lambda: norm((sine_sweep(160, 520, 0.38, 0.7) * (1 + 0.5 * np.sin(2 * np.pi * 26 * t(0.38))))
                                 * np.sin(np.pi * t(0.38) / 0.38), 0.6),
    "gomu/snap": lambda: norm(sine_sweep(700, 120, 0.15, 0.4) * np.exp(-t(0.15) * 18) + 0.3 * highpass(noise(0.15), 2000) * np.exp(-t(0.15) * 80), 0.7),
    "gomu/inflate": lambda: norm(lowpass(noise(0.6), 700 + 1200 * t(0.6)) * np.sin(np.pi * t(0.6) / 0.6) + 0.25 * sine_sweep(90, 160, 0.6), 0.6),
    "gomu/gear_second": lambda: norm(highpass(noise(1.1), 2500) * (np.abs(np.sin(np.pi * 3 * t(1.1) / 1.1)) ** 2) * np.exp(-t(1.1) * 1.2), 0.6),
    "gomu/bounce": lambda: norm(np.sin(2 * np.pi * np.cumsum(260 + 160 * np.sin(2 * np.pi * 14 * t(0.45)) * np.exp(-t(0.45) * 5)) / SR)
                                * np.exp(-t(0.45) * 6), 0.7),
    "destruction/rumble": lambda: norm(lowpass(noise(1.6), 160) * np.sin(np.pi * t(1.6) / 1.6) ** 0.6, 0.9),
    "fruit/eat": lambda: norm(sum(np.sin(2 * np.pi * f * t(1.0)) * np.exp(-((t(1.0) - d) ** 2) / 0.02) for f, d in
                                  [(392, 0.1), (523, 0.25), (659, 0.4), (784, 0.55), (1046, 0.7)]), 0.5),
    # ---------------- Haki
    "haki/awaken": lambda: norm(sum(np.sin(2 * np.pi * f * t(1.6)) for f in (110, 165, 220, 277, 330)) * np.sin(np.pi * t(1.6) / 1.6) ** 0.8
                                + 0.2 * highpass(noise(1.6), 4000) * np.sin(np.pi * t(1.6) / 1.6), 0.6),
    "haki/armament": lambda: norm(sum(np.sin(2 * np.pi * f * t(0.7)) * np.exp(-t(0.7) * k) for f, k in [(180, 4), (513, 6), (1290, 9), (2210, 13)])
                                  + 0.5 * lowpass(noise(0.7), 2000) * np.exp(-t(0.7) * 60), 0.7),
    "haki/observation": lambda: norm(sine_sweep(2400, 900, 0.9, 0.6) * np.exp(-t(0.9) * 3) + 0.4 * np.sin(2 * np.pi * 1760 * t(0.9)) * np.exp(-t(0.9) * 5), 0.45),
    "haki/dodge": lambda: norm(whoosh(0.3, 1200, 6000, 0.6) + 0.4 * np.sin(2 * np.pi * 2093 * t(0.3)) * np.exp(-t(0.3) * 12), 0.6),
    "haki/emission": lambda: norm(np.tanh(3 * sine_sweep(110, 38, 0.8, 0.4) * np.exp(-t(0.8) * 4)) + 0.4 * lowpass(noise(0.8), 300) * np.exp(-t(0.8) * 3)),
    "haki/conqueror_charge": lambda: norm(lowpass(noise(1.6), 120 + 500 * t(1.6) / 1.6) * (t(1.6) / 1.6) ** 0.7
                                          + 0.5 * highpass(noise(1.6) * (rng.random(int(SR * 1.6)) > 0.997), 3000), 0.8),
    "haki/conqueror_wave": lambda: norm(np.tanh(4 * sine_sweep(70, 28, 2.0, 0.3) * np.exp(-t(2.0) * 1.5))
                                        + 0.6 * lowpass(noise(2.0), 400) * np.exp(-t(2.0) * 1.2)
                                        + 0.5 * highpass(noise(2.0) * (rng.random(int(SR * 2.0)) > 0.993), 2500) * np.exp(-t(2.0) * 1.0)),
    "haki/lightning": lambda: norm(highpass(noise(0.5) * (rng.random(int(SR * 0.5)) > 0.97), 1800) * np.exp(-t(0.5) * 5)
                                   + 0.3 * lowpass(noise(0.5), 200) * np.exp(-t(0.5) * 8), 0.7),
}

def write_ogg(path, data):
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.unlink(tmp.name)

def main():
    events = {}
    for name, fn in SOUNDS.items():
        path = os.path.join(OUT, name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        write_ogg(path, fn())
        key = name.replace("/", ".")
        events[key] = {"subtitle": "subtitles.onepiece." + key, "sounds": [{"name": "onepiece:" + name}]}
    with open("src/main/resources/assets/onepiece/sounds.json", "w") as f:
        json.dump(events, f, indent=2)
    print("wrote", len(events), "sounds")

if __name__ == "__main__":
    main()
