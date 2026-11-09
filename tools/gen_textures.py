#!/usr/bin/env python3
"""Generates the mod's textures: Devil Fruit item, ability icons and particle sprites.

Icons are drawn at 4x and downsampled for clean anti-aliasing. Run from the repo root:
    python3 tools/gen_textures.py
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter

A = "src/main/resources/assets/onepiece/textures"
random.seed(3)

def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)

# ------------------------------------------------------------------ Devil Fruit item (16x16 pixel art)
def fruit(base, dark, light, path):
    S = 16
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()
    cx, cy, r = 7.5, 9.0, 6.2
    for y in range(S):
        for x in range(S):
            dx, dy = (x - cx) / r, (y - cy) / (r * 0.95)
            d = dx * dx + dy * dy
            if d <= 1.0:
                ang = math.atan2(dy, dx)
                swirl = math.sin(ang * 3 + math.sqrt(d) * 9.0)
                shade = 1.0 - 0.35 * (dx + dy + 1) / 2
                col = dark if swirl > 0.55 else base
                col = tuple(int(c * shade) for c in col)
                if d > 0.82: col = tuple(int(c * 0.6) for c in col)
                px[x, y] = col + (255,)
    for (x, y) in [(5, 6), (6, 5), (5, 5)]:
        px[x, y] = light + (255,)
    # stem curl
    for (x, y) in [(8, 3), (8, 2), (9, 1), (10, 1), (11, 2), (7, 3)]:
        px[x, y] = (70, 140, 50, 255)
    px[11, 3] = (50, 110, 40, 255)
    save(img, path)

fruit((142, 63, 209), (96, 30, 150), (230, 190, 255), f"{A}/item/gomu_gomu_no_mi.png")

# ------------------------------------------------------------------ ability icons (32x32)
SS = 128
def canvas(): return Image.new("RGBA", (SS, SS), (0, 0, 0, 0))
def finish(img, path, glow=None):
    if glow:
        g = Image.new("RGBA", img.size, (0, 0, 0, 0))
        mask = img.split()[3].filter(ImageFilter.GaussianBlur(10))
        g.paste(Image.new("RGBA", img.size, glow + (255,)), (0, 0), mask)
        img = Image.alpha_composite(g, img)
    save(img.resize((32, 32), Image.LANCZOS), path)

def fist(d, cx, cy, s, fill, outline=(40, 20, 10)):
    w = 2
    d.rounded_rectangle([cx - s, cy - s * 0.8, cx + s, cy + s * 0.8], radius=s * 0.45, fill=fill, outline=outline, width=int(s * 0.12) + w)
    for i in range(1, 4):  # knuckle lines
        x = cx - s + i * (2 * s / 4)
        d.line([x, cy - s * 0.8, x, cy - s * 0.1], fill=outline, width=int(s * 0.08) + 1)
    d.rounded_rectangle([cx - s * 1.15, cy - s * 0.1, cx - s * 0.55, cy + s * 0.55], radius=s * 0.2, fill=fill, outline=outline, width=int(s * 0.1) + 1)

SKIN = (255, 205, 160)
RED = (255, 110, 90)

def speed_lines(d, x0, x1, ys, col=(255, 255, 255), width=5):
    for y in ys: d.line([x0, y, x1, y], fill=col + (220,), width=width)

def icon_pistol(path, color, jet=False):
    img = canvas(); d = ImageDraw.Draw(img)
    speed_lines(d, 8, 60, [40, 56, 72, 88], (255, 255, 255) if not jet else (255, 200, 190))
    d.rectangle([20, 54, 70, 74], fill=color, outline=(40, 20, 10), width=4)  # stretched arm
    fist(d, 86, 64, 26, color)
    if jet:
        for i in range(6):
            x, y, r = random.randint(10, 60), random.randint(20, 110), random.randint(6, 12)
            d.ellipse([x - r, y - r, x + r, y + r], fill=(240, 240, 240, 170))
    finish(img, path, glow=(255, 80, 60) if jet else (255, 230, 180))

def icon_whip(path):
    img = canvas(); d = ImageDraw.Draw(img)
    d.arc([10, 20, 118, 128], start=200, end=340, fill=(255, 255, 255, 230), width=10)
    d.arc([18, 30, 110, 120], start=205, end=335, fill=SKIN + (255,), width=12)
    d.ellipse([92, 44, 120, 70], fill=(180, 60, 40), outline=(40, 20, 10), width=4)  # sandal/foot
    finish(img, path, glow=(255, 230, 180))

def icon_gatling(path, color, jet=False):
    img = canvas(); d = ImageDraw.Draw(img)
    for (x, y, s) in [(44, 36, 15), (82, 30, 15), (30, 74, 15), (70, 66, 17), (100, 80, 15), (56, 102, 14)]:
        fist(d, x, y, s, color)
    finish(img, path, glow=(255, 80, 60) if jet else (255, 230, 180))

def icon_rocket(path):
    img = canvas(); d = ImageDraw.Draw(img)
    d.polygon([(64, 6), (100, 52), (76, 52), (76, 96), (52, 96), (52, 52), (28, 52)], fill=(255, 220, 120), outline=(60, 30, 10))
    for x in (40, 64, 88): d.line([x, 104, x, 124], fill=(255, 255, 255, 220), width=5)
    finish(img, path, glow=(255, 200, 80))

def icon_bazooka(path, color, jet=False):
    img = canvas(); d = ImageDraw.Draw(img)
    for y in (36, 76):
        d.rounded_rectangle([60, y, 108, y + 30], radius=10, fill=color, outline=(40, 20, 10), width=4)
        d.rectangle([14, y + 8, 62, y + 22], fill=color, outline=(40, 20, 10), width=4)
    for r in (20, 34):
        d.arc([108 - r, 66 - r, 108 + r, 66 + r], start=-60, end=60, fill=(255, 255, 255, 230), width=5)
    finish(img, path, glow=(255, 80, 60) if jet else (255, 230, 180))

def icon_gear_second(path):
    img = canvas(); d = ImageDraw.Draw(img)
    for (x, y, r) in [(40, 70, 26), (66, 52, 30), (92, 72, 24), (64, 86, 26)]:
        d.ellipse([x - r, y - r, x + r, y + r], fill=(235, 235, 240, 235))
    d.ellipse([34, 34, 94, 94], fill=(255, 90, 80), outline=(90, 10, 10), width=5)
    d.text((52, 40), "2", fill=(255, 255, 255))
    # draw a big "2" manually (thick strokes)
    d.arc([48, 42, 80, 68], start=180, end=20, fill=(255, 255, 255), width=8)
    d.line([78, 58, 50, 86], fill=(255, 255, 255), width=8)
    d.line([48, 86, 82, 86], fill=(255, 255, 255), width=8)
    finish(img, path, glow=(255, 60, 50))

def icon_gigant(path):
    img = canvas(); d = ImageDraw.Draw(img)
    d.rectangle([4, 54, 34, 78], fill=SKIN, outline=(40, 20, 10), width=4)
    fist(d, 74, 64, 46, (255, 190, 120))
    d.arc([96, 88, 126, 118], start=0, end=360, fill=(255, 255, 255), width=6)
    d.line([104, 96, 118, 96], fill=(255, 255, 255), width=5); d.line([118, 96, 110, 103], fill=(255, 255, 255), width=5)
    d.line([110, 103, 118, 110], fill=(255, 255, 255), width=5); d.line([118, 110, 104, 110], fill=(255, 255, 255), width=5)
    finish(img, path, glow=(255, 170, 60))

G = f"{A}/gui/ability"
icon_pistol(f"{G}/gomu_pistol.png", SKIN)
icon_pistol(f"{G}/gomu_jet_pistol.png", RED, jet=True)
icon_whip(f"{G}/gomu_whip.png")
icon_gatling(f"{G}/gomu_gatling.png", SKIN)
icon_gatling(f"{G}/gomu_jet_gatling.png", RED, jet=True)
icon_rocket(f"{G}/gomu_rocket.png")
icon_bazooka(f"{G}/gomu_bazooka.png", SKIN)
icon_bazooka(f"{G}/gomu_jet_bazooka.png", RED, jet=True)
icon_gear_second(f"{G}/gomu_gear_second.png")
icon_gigant(f"{G}/gomu_gigant_pistol.png")

# ------------------------------------------------------------------ haki icons
DARK = (40, 20, 70)
def icon_armament(path):
    img = canvas(); d = ImageDraw.Draw(img)
    fist(d, 64, 64, 40, (25, 18, 40), outline=(150, 110, 255))
    d.arc([18, 18, 110, 110], start=200, end=250, fill=(200, 170, 255), width=5)  # sheen
    finish(img, path, glow=(120, 70, 255))

def icon_observation(path):
    img = canvas(); d = ImageDraw.Draw(img)
    d.ellipse([10, 36, 118, 92], fill=(10, 40, 60), outline=(120, 230, 255), width=6)
    d.ellipse([46, 42, 82, 86], fill=(75, 216, 255))
    d.ellipse([58, 54, 70, 74], fill=(5, 15, 25))
    for r in (52, 62):
        d.arc([64 - r, 64 - r, 64 + r, 64 + r], start=-30, end=30, fill=(120, 230, 255, 200), width=3)
        d.arc([64 - r, 64 - r, 64 + r, 64 + r], start=150, end=210, fill=(120, 230, 255, 200), width=3)
    finish(img, path, glow=(75, 216, 255))

def bolt_line(d, pts, w, col):
    for a, b in zip(pts, pts[1:]): d.line([a, b], fill=col, width=w)

def icon_emission(path):
    img = canvas(); d = ImageDraw.Draw(img)
    fist(d, 44, 64, 26, (25, 18, 40), outline=(150, 110, 255))
    for r in (18, 30, 42):
        d.arc([84 - r, 64 - r, 84 + r, 64 + r], start=-70, end=70, fill=(170, 130, 255), width=6)
    finish(img, path, glow=(120, 70, 255))

def icon_future(path):
    img = canvas(); d = ImageDraw.Draw(img)
    d.ellipse([10, 36, 118, 92], fill=(10, 40, 60), outline=(120, 230, 255), width=6)
    d.ellipse([46, 42, 82, 86], fill=(255, 255, 255))
    for k in range(3):
        x = 70 + k * 16
        d.polygon([(x, 20), (x + 12, 30), (x, 40)], fill=(120, 230, 255))
    finish(img, path, glow=(160, 240, 255))

def icon_conqueror(path, infusion=False):
    img = canvas(); d = ImageDraw.Draw(img)
    if infusion: fist(d, 64, 70, 30, (25, 18, 40), outline=(255, 60, 60))
    else: d.ellipse([40, 40, 88, 88], fill=(20, 0, 0), outline=(255, 50, 50), width=5)
    rnd = random.Random(5 if infusion else 9)
    for k in range(7):
        a = k * 2 * math.pi / 7 + 0.3
        pts = [(64, 64)]
        for j in range(1, 5):
            rr = 18 + j * 11
            pts.append((64 + math.cos(a) * rr + rnd.randint(-7, 7), 64 + math.sin(a) * rr + rnd.randint(-7, 7)))
        bolt_line(d, pts, 9, (200, 20, 20, 255))
        bolt_line(d, pts, 4, (10, 0, 10, 255))
    finish(img, path, glow=(255, 40, 40))

icon_armament(f"{G}/haki_armament.png")
icon_observation(f"{G}/haki_observation.png")
icon_emission(f"{G}/haki_emission.png")
icon_future(f"{G}/haki_future_sight.png")
icon_conqueror(f"{G}/haki_conqueror_wave.png")
icon_conqueror(f"{G}/haki_conqueror_infusion.png", infusion=True)

# ------------------------------------------------------------------ transformation icons
E = f"{A}/entity"
G4 = (35, 20, 30)
WHITE = (250, 250, 250)
icon_pistol(f"{G}/gomu_kong_gun.png", G4, jet=False)
icon_gatling(f"{G}/gomu_kong_organ.png", G4)
icon_bazooka(f"{G}/gomu_leo_bazooka.png", G4)
icon_gigant(f"{G}/gomu_king_kong_gun.png")
icon_pistol(f"{G}/gomu_jet_culverin.png", (120, 25, 45), jet=True)
icon_gatling(f"{G}/gomu_black_mamba.png", (120, 25, 45), jet=True)
icon_bazooka(f"{G}/gomu_king_cobra.png", (120, 25, 45), jet=True)
icon_pistol(f"{G}/gomu_dawn_gun.png", WHITE)
icon_whip(f"{G}/gomu_dawn_whip.png")
icon_rocket(f"{G}/gomu_dawn_rocket.png")
icon_gigant(f"{G}/gomu_bajrang_gun.png")

def recolor(path, tint, mix=0.75):
    im = Image.open(path).convert("RGBA"); px = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a: px[x, y] = tuple(int(c * (1 - mix) + t * mix) for c, t in zip((r, g, b), tint)) + (a,)
    im.save(path)
recolor(f"{G}/gomu_king_kong_gun.png", (60, 25, 30), 0.6)
recolor(f"{G}/gomu_dawn_whip.png", WHITE, 0.6)
recolor(f"{G}/gomu_dawn_rocket.png", WHITE, 0.6)
recolor(f"{G}/gomu_bajrang_gun.png", WHITE, 0.55)

def icon_form(path, body, accent, symbol):
    img = canvas(); d = ImageDraw.Draw(img)
    d.ellipse([12, 12, 116, 116], fill=body + (255,), outline=accent + (255,), width=7)
    d.text((0, 0), "")
    symbol(d)
    finish(img, path, glow=accent)
def sym_four(d):
    d.line([(74, 34), (44, 76), (84, 76)], fill=(255, 80, 70), width=11)
    d.line([(74, 34), (74, 96)], fill=(255, 80, 70), width=11)
def sym_snake(d):
    d.arc([36, 30, 76, 66], 90, 360, fill=(255, 80, 90), width=9)
    d.arc([52, 60, 92, 98], 270, 180, fill=(255, 80, 90), width=9)
    d.ellipse([84, 86, 98, 100], fill=(255, 80, 90))
def sym_five(d):
    for r in (20, 30):
        d.arc([64 - r, 50 - r, 64 + r, 50 + r], 200, 340, fill=(255, 255, 255), width=5)
    d.line([(46, 58), (82, 58)], fill=(60, 60, 60), width=8)
    d.line([(46, 58), (46, 76)], fill=(60, 60, 60), width=8)
    d.arc([40, 66, 86, 104], 270, 140, fill=(60, 60, 60), width=8)
icon_form(f"{G}/gomu_boundman.png", (30, 15, 20), (230, 60, 50), sym_four)
icon_form(f"{G}/gomu_snakeman.png", (40, 15, 25), (200, 40, 80), sym_snake)
icon_form(f"{G}/gomu_gear_fifth.png", (240, 240, 245), (255, 255, 255), sym_five)

def flame(d, cx, cy, s, col):
    d.polygon([(cx, cy - s), (cx + s * 0.55, cy), (cx + s * 0.3, cy + s * 0.7), (cx - s * 0.3, cy + s * 0.7), (cx - s * 0.55, cy)], fill=col)
BLUE = (70, 200, 255); DEEP = (20, 90, 220); GOLD = (255, 200, 60)
def icon_phoenix_wings(path, full):
    img = canvas(); d = ImageDraw.Draw(img)
    for side in (-1, 1):
        pts = [(64, 70)] + [(64 + side * (20 + k * 10), 70 - 34 + k * 8 + (k % 2) * 10) for k in range(5)] + [(64 + side * 12, 86)]
        d.polygon(pts, fill=BLUE + (255,), outline=DEEP + (255,))
    if full:
        d.ellipse([52, 52, 76, 92], fill=DEEP); d.ellipse([56, 36, 72, 54], fill=DEEP); d.polygon([(64, 30), (70, 40), (58, 40)], fill=GOLD)
        d.polygon([(64, 92), (52, 120), (64, 110), (76, 120)], fill=BLUE)
    else:
        d.ellipse([54, 40, 74, 60], fill=(255, 205, 160)); d.rectangle([56, 60, 72, 96], fill=(255, 255, 255))
    finish(img, path, glow=BLUE)
icon_phoenix_wings(f"{G}/phoenix_hybrid.png", False)
icon_phoenix_wings(f"{G}/phoenix_full.png", True)
def icon_simple(path, draw):
    img = canvas(); d = ImageDraw.Draw(img); draw(d); finish(img, path, glow=BLUE)
icon_simple(f"{G}/phoenix_talon.png", lambda d: ([d.line([(40, 30 + k * 6), (60 + k * 10, 96)], fill=GOLD, width=9) for k in range(3)], flame(d, 82, 70, 30, BLUE)))
icon_simple(f"{G}/phoenix_flame_wave.png", lambda d: [d.arc([20 + k * 8, 20 + k * 8, 108 - k * 8, 108 - k * 8], 300, 60, fill=(BLUE if k % 2 else DEEP), width=9) for k in range(4)])
icon_simple(f"{G}/phoenix_regeneration.png", lambda d: (flame(d, 64, 70, 48, BLUE), d.rectangle([58, 48, 70, 92], fill=(255, 255, 255)), d.rectangle([42, 64, 86, 76], fill=(255, 255, 255))))
icon_simple(f"{G}/phoenix_dive.png", lambda d: (d.polygon([(64, 118), (36, 40), (64, 60), (92, 40)], fill=BLUE, outline=DEEP), flame(d, 64, 26, 20, DEEP)))

# Phoenix Zoan fruit (16x16) and textures
fruit((70, 190, 245), (25, 90, 200), (210, 245, 255), f"{A}/item/tori_tori_no_mi_phoenix.png")
S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); px = img.load()
for y in range(S):
    for x in range(S):
        n = 0.5 + 0.5 * math.sin(x * 0.7 + math.sin(y * 0.5) * 2) * math.cos(y * 0.35)
        r, g, b = int(40 + 90 * n), int(150 + 90 * n), 255
        px[x, y] = (r, g, b, int(150 + 105 * n))
d = ImageDraw.Draw(img)
d.rectangle([48, 0, 63, 15], fill=GOLD + (255,))
save(img, f"{E}/phoenix.png")

# Gear Fourth markings on the skin UV map: red flame lines on body, arms and legs
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
rnd = random.Random(4)
def flames_in(x0, y0, x1, y1):
    for k in range(2):
        x = rnd.randint(x0, x1 - 1)
        pts = [(x, y1 - 1)]
        for yy in range(y1 - 2, y0 - 1, -2):
            x = max(x0, min(x1 - 1, x + rnd.choice((-1, 0, 1))))
            pts.append((x, yy))
        d.line(pts, fill=(255, 255, 255, 255), width=1)
for (x0, y0, x1, y1) in [(20, 20, 28, 32), (32, 20, 40, 32), (40, 20, 56, 32), (32, 52, 48, 64), (0, 20, 16, 32), (16, 52, 32, 64)]:
    flames_in(x0, y0, x1, y1)
save(img, f"{E}/gear_fourth_pattern.png")

# ------------------------------------------------------------------ particles
P = f"{A}/particle"
for i in range(4):
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(9 + i * 2):
        r = random.randint(5, 9 + i)
        x, y = random.randint(8, 24), random.randint(8, 24)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 90))
    img = img.filter(ImageFilter.GaussianBlur(2.2 + i * 0.6))
    px = img.load()
    for y in range(S):  # radial falloff so the puff never shows a square edge
        for x in range(S):
            r, g, b, a = px[x, y]
            k = max(0.0, 1.0 - (math.hypot(x - 15.5, y - 15.5) / 15.5) ** 2)
            px[x, y] = (r, g, b, int(min(255, a * 2.2) * k))
    save(img.resize((16, 16), Image.LANCZOS), f"{P}/steam_{i}.png")

S = 64
img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
for k in range(8):
    a = k * math.pi / 4
    L = 30 if k % 2 == 0 else 18
    d.polygon([(32 + math.cos(a) * L, 32 + math.sin(a) * L), (32 + math.cos(a + 0.25) * 6, 32 + math.sin(a + 0.25) * 6),
               (32, 32), (32 + math.cos(a - 0.25) * 6, 32 + math.sin(a - 0.25) * 6)], fill=(255, 255, 255, 255))
d.ellipse([22, 22, 42, 42], fill=(255, 255, 255, 255))
img = Image.alpha_composite(img.filter(ImageFilter.GaussianBlur(3)), img)
save(img.resize((32, 32), Image.LANCZOS), f"{P}/impact_spark.png")

S = 128
img = Image.new("RGBA", (S, S), (0, 0, 0, 0)); px = img.load()
for y in range(S):
    for x in range(S):
        r = math.hypot(x - 63.5, y - 63.5) / 63.5
        a = math.exp(-((r - 0.85) / 0.07) ** 2) + 0.35 * math.exp(-((r - 0.72) / 0.12) ** 2)
        px[x, y] = (255, 255, 255, int(255 * min(1, a)))
save(img.resize((64, 64), Image.LANCZOS), f"{P}/ring.png")
save(Image.new("RGBA", (4, 4), (255, 255, 255, 255)), f"{P}/white.png")

# ------------------------------------------------------------------ Armament Haki: flame aura + gloss map
E = f"{A}/entity"
# Tileable purple/black flame noise, scrolled by RenderType.energySwirl.
import numpy as _np
S = 64
yy, xx = _np.mgrid[0:S, 0:S] / S
rnd = random.Random(11)
field = _np.zeros((S, S))
for octave in range(1, 5):  # tileable sum of waves at several scales – fine detail so small UV regions still flicker
    for _ in range(3):
        fx, fy = rnd.randint(-octave * 6, octave * 6), rnd.randint(4, octave * 9)
        ph = rnd.uniform(0, 6.28)
        field += _np.sin(2 * _np.pi * (fx * xx + fy * yy) + ph + _np.sin(2 * _np.pi * 2 * yy) * 1.2) / octave
field = (field - field.min()) / (field.max() - field.min())
flame = _np.clip((field - 0.38) / 0.5, 0, 1) ** 1.2
rgba = _np.zeros((S, S, 4), dtype=_np.uint8)
rgba[..., 0] = _np.clip(90 + 160 * flame, 0, 255)
rgba[..., 1] = _np.clip(20 + 90 * flame ** 2, 0, 255)
rgba[..., 2] = 255
rgba[..., 3] = _np.clip(255 * flame * 1.4, 0, 255)
save(Image.fromarray(rgba, "RGBA"), f"{E}/haki_flame.png")

# Gloss highlights laid out on the 64x64 player-skin UV map (base + overlay layers).
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
def limb(u, v, w, d_, h):
    """Box UV at (u,v) with width w, depth d_, height h: faces right|front|left|back along the side strip."""
    sides = [(u, v + d_, d_), (u + d_, v + d_, w), (u + d_ + w, v + d_, d_), (u + 2 * d_ + w, v + d_, w)]
    for i, (fu, fv, fw) in enumerate(sides):
        sx = fu + max(0, fw // 3 - (1 if i % 2 else 0))
        for yy in range(fv, fv + h):  # vertical streak, fades toward the shoulder
            a = int(90 + 120 * (yy - fv) / max(1, h - 1))
            d.point((sx, yy), fill=(255, 255, 255, a))
        d.point((sx + 1, fv + h - 3), fill=(255, 255, 255, 255))  # knuckle shine
        d.point((sx + 1, fv + h - 2), fill=(255, 255, 255, 230))
    d.rectangle([u + d_ + w, v, u + d_ + w + 1, v + 1], fill=(255, 255, 255, 200))  # tip of the fist (bottom face)
for (u, v, w, dd, h) in [(40, 16, 4, 4, 12), (40, 32, 4, 4, 12), (32, 48, 4, 4, 12), (48, 48, 4, 4, 12),   # arms + sleeves
                         (0, 16, 4, 4, 12), (0, 32, 4, 4, 12), (16, 48, 4, 4, 12), (0, 48, 4, 4, 12),     # legs + pants
                         (16, 16, 8, 4, 12), (16, 32, 8, 4, 12)]:                                         # body + jacket
    limb(u, v, w, dd, h)
d.line([(10, 9), (10, 12)], fill=(255, 255, 255, 170))  # head
d.line([(42, 9), (42, 12)], fill=(255, 255, 255, 170))  # hat
save(img, f"{E}/armament_gloss.png")  # solid sprite for ribbons (black lightning)
print("textures done")
