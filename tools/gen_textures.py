#!/usr/bin/env python3
"""Generates every texture in the mod as original pixel art.

Run from the repo root:  python3 tools/gen_textures.py

The queen's box list below mirrors QueenBeeModel.createBodyLayer() exactly (texture offsets and
sizes), so each face gets painted where Minecraft will look for it. Change one, change the other.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src")
ASSETS = os.path.join(ROOT, "main", "resources", "assets", "hivemind", "textures")

rng = random.Random(1337)


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def jitter(c, amount=10):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (c[3],)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))


# Palette picked from the reference image.
PURPLE = hexc("#3b2752")
PURPLE_DARK = hexc("#28193a")
PURPLE_LIGHT = hexc("#54396f")
GOLD = hexc("#bf9a32")
GOLD_DARK = hexc("#8c6d1c")
GOLD_LIGHT = hexc("#e0c160")
EYE = hexc("#7a5cc4")
EYE_DARK = hexc("#4b3590")
EYE_SHINE = hexc("#d6c8ff")
GEM = hexc("#e3851c")
GEM_LIGHT = hexc("#ffc24d")
GEM_DARK = hexc("#a65610")
SKIN = hexc("#d8ccb2")
SKIN_DARK = hexc("#a99c84")
FUR = hexc("#b08c2e")
FUR_DARK = hexc("#7d6118")
BLACK = hexc("#1d1424")
WING = hexc("#b7a3ea", 150)
WING_EDGE = hexc("#8e76d6", 210)
WING_VEIN = hexc("#7c64c4", 190)


class Tex:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def set(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.px[x, y] = c

    def rect(self, x, y, w, h, fn):
        """fn(i, j, w, h) -> color, with (i, j) local to the rectangle."""
        for j in range(h):
            for i in range(w):
                self.set(x + i, y + j, fn(i, j, w, h))

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


def solid(c, n=8):
    return lambda i, j, w, h: jitter(c, n)


class Box:
    """A Minecraft model cube's texture footprint. Faces are named from the entity's point of view."""

    def __init__(self, tex, u, v, w, h, d):
        self.t, self.u, self.v, self.w, self.h, self.d = tex, u, v, w, h, d

    def top(self, fn):
        self.t.rect(self.u + self.d, self.v, self.w, self.d, fn)

    def bottom(self, fn):
        self.t.rect(self.u + self.d + self.w, self.v, self.w, self.d, fn)

    def right(self, fn):
        self.t.rect(self.u, self.v + self.d, self.d, self.h, fn)

    def front(self, fn):
        self.t.rect(self.u + self.d, self.v + self.d, self.w, self.h, fn)

    def left(self, fn):
        self.t.rect(self.u + self.d + self.w, self.v + self.d, self.d, self.h, fn)

    def back(self, fn):
        self.t.rect(self.u + 2 * self.d + self.w, self.v + self.d, self.w, self.h, fn)

    def sides(self, fn):
        self.right(fn)
        self.left(fn)
        self.back(fn)

    def all(self, fn):
        for f in (self.top, self.bottom, self.right, self.front, self.left, self.back):
            f(fn)


# ---------------------------------------------------------------------------------------------
# Queen bee (128x128). Offsets and sizes match QueenBeeModel.
# ---------------------------------------------------------------------------------------------
def queen():
    t = Tex(128, 128)

    # Head 8x8x8 at (0,0): purple hood all round, a gold mask over the brow, pale muzzle below.
    head = Box(t, 0, 0, 8, 8, 8)
    head.all(solid(PURPLE))
    head.top(lambda i, j, w, h: jitter(GOLD if 3 <= i <= 4 or j <= 1 else PURPLE, 8))

    def face(i, j, w, h):
        if j <= 2:
            return jitter(GOLD_LIGHT if j == 0 else GOLD, 6)
        if j <= 5:
            return jitter(GOLD_DARK if i in (3, 4) else PURPLE_DARK, 6)
        if j == 6 and i in (3, 4):
            return SKIN_DARK
        return jitter(SKIN, 6) if 2 <= i <= 5 else jitter(PURPLE, 6)

    head.front(face)
    head.right(lambda i, j, w, h: jitter(GOLD if j <= 1 and i <= 3 else PURPLE, 8))
    head.left(lambda i, j, w, h: jitter(GOLD if j <= 1 and i >= 4 else PURPLE, 8))

    # Compound eyes 3x3x1.
    def eye(i, j, w, h):
        if (i, j) == (1, 0) or (i, j) == (0, 0):
            return EYE_SHINE
        return jitter(EYE if j < 2 else EYE_DARK, 10)

    for u in (32, 40):
        b = Box(t, u, 0, 3, 3, 1)
        b.all(lambda i, j, w, h: jitter(EYE_DARK, 6))
        b.front(eye)

    # Forehead and collar gems 2x2x1.
    for u in (48, 80):
        b = Box(t, u, 0, 2, 2, 1)
        b.all(solid(GEM_DARK, 4))
        b.front(lambda i, j, w, h: GEM_LIGHT if (i, j) == (0, 0) else GEM)

    # Antenna stalks 1x6x1 and hooked tips 3x1x1.
    for u in (56, 60):
        Box(t, u, 0, 1, 6, 1).all(lambda i, j, w, h: jitter(GOLD_DARK if j > 1 else BLACK, 6))
    for u in (64, 72):
        Box(t, u, 0, 3, 1, 1).all(solid(BLACK, 4))

    # Stinger 1x1x2.
    Box(t, 86, 0, 1, 1, 2).all(solid(BLACK, 4))

    # Chest 8x6x5 at (0,16): dark suit with a gold breastplate.
    chest = Box(t, 0, 16, 8, 6, 5)
    chest.all(solid(PURPLE))

    def breastplate(i, j, w, h):
        # Gold plate that narrows toward the top, purple shoulders either side.
        inset = max(0, 2 - j)
        if inset <= i < w - inset:
            if j == 5 or i in (inset, w - inset - 1):
                return jitter(GOLD_DARK, 6)
            return jitter(GOLD_LIGHT if j <= 1 else GOLD, 6)
        return jitter(PURPLE_LIGHT if j == 0 else PURPLE, 6)

    chest.front(breastplate)
    chest.back(lambda i, j, w, h: jitter(PURPLE_DARK if i in (3, 4) else PURPLE, 6))

    # Waist 6x6x4 at (0,28): segmented gold belly.
    waist = Box(t, 0, 28, 6, 6, 4)
    waist.all(solid(PURPLE))
    waist.front(lambda i, j, w, h: jitter(GOLD_DARK if j % 2 == 1 or i in (0, 5) else GOLD, 6))
    waist.right(lambda i, j, w, h: jitter(GOLD_DARK if i == 3 and j % 2 == 0 else PURPLE, 6))
    waist.left(lambda i, j, w, h: jitter(GOLD_DARK if i == 0 and j % 2 == 0 else PURPLE, 6))

    # Hip band 7x2x5 at (28,16).
    hips = Box(t, 28, 16, 7, 2, 5)
    hips.all(lambda i, j, w, h: jitter(GOLD if j == 0 else GOLD_DARK, 6))

    # Bee abdomen 5x5x7 at (28,24): black and gold stripes running back.
    abd = Box(t, 28, 24, 5, 5, 7)
    stripe = lambda k: GOLD if (k // 2) % 2 == 0 else BLACK
    abd.top(lambda i, j, w, h: jitter(stripe(j), 6))
    abd.bottom(lambda i, j, w, h: jitter(stripe(j), 6))
    abd.right(lambda i, j, w, h: jitter(stripe(w - 1 - i), 6))
    abd.left(lambda i, j, w, h: jitter(stripe(i), 6))
    abd.front(solid(BLACK, 4))
    abd.back(solid(GOLD_DARK, 6))

    # Arms 3x12x3: gold sleeve on top, dark purple glove below.
    for u in (56, 68):
        arm = Box(t, u, 16, 3, 12, 3)
        arm.all(lambda i, j, w, h: jitter(GOLD if j < 5 else (PURPLE_DARK if j >= 9 else PURPLE), 7))
        arm.bottom(solid(PURPLE_DARK, 4))

    # Pauldrons 5x3x5.
    for u in (80, 100):
        p = Box(t, u, 16, 5, 3, 5)
        p.all(lambda i, j, w, h: jitter(GOLD_LIGHT if j == 0 else GOLD, 6))
        p.top(lambda i, j, w, h: jitter(GOLD_LIGHT if (i + j) % 3 else GOLD, 6))
        p.bottom(solid(GOLD_DARK, 4))

    # Thighs 4x7x4: purple with gold knee guards.
    for u in (0, 16):
        th = Box(t, u, 40, 4, 7, 4)
        th.all(lambda i, j, w, h: jitter(GOLD if j >= 5 else PURPLE, 7))
        th.front(lambda i, j, w, h: jitter(GOLD_LIGHT if j >= 5 else (GOLD_DARK if j == 0 else PURPLE), 7))

    # Shins 3x5x3.
    for u in (32, 44):
        Box(t, u, 40, 3, 5, 3).all(lambda i, j, w, h: jitter(PURPLE_DARK if j >= 3 else PURPLE, 7))

    # Fluffy ankles 4x3x4.
    for u in (56, 72):
        Box(t, u, 40, 4, 3, 4).all(lambda i, j, w, h: jitter(FUR if rng.random() < 0.7 else FUR_DARK, 14))

    # Hooves 3x1x4.
    for u in (88, 102):
        Box(t, u, 40, 3, 1, 4).all(solid(GOLD_DARK, 6))

    # Wings 9x22 planes at (0,64) and (20,64). Long and translucent, like the reference's drape.
    def wing(mirror):
        def paint(i, j, w, h):
            x = (w - 1 - i) if mirror else i
            # Teardrop outline: narrow at the shoulder, widest near the bottom third.
            t_ = j / (h - 1)
            half = 1.5 + 7.0 * (t_ ** 0.6) * (1.0 - max(0.0, t_ - 0.75) * 2.2)
            if x > half or j == h - 1 and x > 2:
                return (0, 0, 0, 0)
            if x >= half - 1 or j == h - 1:
                return WING_EDGE
            if x == 2 or (j > 4 and (j - x) % 7 == 0):
                return WING_VEIN
            return jitter(WING, 6)

        return paint

    # Right wing (x -9..0): its root edge is on the texture right on the front face. Left wing is the reverse.
    for u, mirror in ((0, True), (20, False)):
        b = Box(t, u, 64, 9, 22, 0)
        b.front(wing(mirror))
        b.back(wing(not mirror))

    t.save(os.path.join(ASSETS, "entity", "queen_bee.png"))


# ---------------------------------------------------------------------------------------------
# Guard bee gear (64x32): helmet, plume and spear. The bee itself uses the vanilla bee texture.
# ---------------------------------------------------------------------------------------------
def guard_gear():
    t = Tex(64, 32)
    BRONZE = hexc("#b5832e")
    BRONZE_DARK = hexc("#7c5418")
    BRONZE_LIGHT = hexc("#e6b65a")
    WOOD = hexc("#7a5631")
    WOOD_DARK = hexc("#553a1f")
    STEEL = hexc("#c9c4b8")
    STEEL_DARK = hexc("#8d877a")

    helmet = Box(t, 0, 0, 8, 2, 7)
    helmet.all(lambda i, j, w, h: jitter(BRONZE_LIGHT if j == 0 else BRONZE, 8))
    helmet.top(lambda i, j, w, h: jitter(BRONZE_DARK if i in (3, 4) else (BRONZE_LIGHT if j == 0 else BRONZE), 8))
    helmet.front(lambda i, j, w, h: jitter(BRONZE_DARK if j == 1 else BRONZE_LIGHT, 6))

    plume = Box(t, 32, 0, 1, 3, 6)
    plume.all(lambda i, j, w, h: jitter(PURPLE if (i + j) % 3 else PURPLE_LIGHT, 10))

    shaft = Box(t, 0, 12, 1, 1, 12)
    shaft.all(lambda i, j, w, h: jitter(WOOD if (i + j) % 4 else WOOD_DARK, 8))

    tip = Box(t, 36, 12, 2, 2, 2)
    tip.all(lambda i, j, w, h: jitter(STEEL if (i + j) % 2 == 0 else STEEL_DARK, 6))

    t.save(os.path.join(ASSETS, "entity", "guard_bee_gear.png"))


# ---------------------------------------------------------------------------------------------
# Items, block and effect icon (16x16 / 18x18).
# ---------------------------------------------------------------------------------------------
def shrinking_honey():
    t = Tex(16, 16)
    GLASS = hexc("#c8e4f0", 150)
    GLASS_EDGE = hexc("#e9f6fb", 220)
    HONEY = hexc("#f2a71b")
    HONEY_LIGHT = hexc("#ffd257")
    CORK = hexc("#8a5a2b")
    SPARK = hexc("#fff4c0")
    # Bottle silhouette rows: (start, end) columns.
    rows = {3: (6, 9), 4: (6, 9), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (3, 12), 13: (4, 11), 14: (5, 10)}
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            edge = x in (a, b) or y in (3, 14)
            if y >= 8 and not edge:
                c = HONEY_LIGHT if x <= a + 2 and y <= 10 else jitter(HONEY, 6)
            else:
                c = GLASS_EDGE if edge else GLASS
            t.set(x, y, c)
    for x in range(6, 10):
        t.set(x, 1, CORK)
        t.set(x, 2, CORK)
    for x, y in ((12, 2), (13, 3), (11, 1), (2, 5), (14, 7)):
        t.set(x, y, SPARK)
    t.save(os.path.join(ASSETS, "item", "shrinking_honey.png"))


def spawn_egg(name, base, spots):
    t = Tex(16, 16)
    for y in range(16):
        for x in range(16):
            # Egg: an ellipse a bit wider at the bottom.
            dx = (x - 7.5) / (5.0 if y > 7 else 4.4)
            dy = (y - 8.5) / 6.8
            if dx * dx + dy * dy <= 1.0:
                shade = 0.0 if x < 9 else 0.25
                c = mix(base, hexc("#000000"), shade)
                if (x * 7 + y * 13) % 11 == 0 or (x * 3 + y * 5) % 17 == 0:
                    c = spots
                t.set(x, y, c)
    t.set(5, 4, hexc("#ffffff", 180))
    t.set(5, 5, hexc("#ffffff", 140))
    t.save(os.path.join(ASSETS, "item", name + ".png"))


def hive_exit():
    t = Tex(16, 16)
    BRIGHT = hexc("#ffe08a")
    MID = hexc("#f5b52e")
    DARK = hexc("#c07812")
    for y in range(16):
        for x in range(16):
            # Hex cell pattern with glowing centres.
            cx = (x + (4 if (y // 4) % 2 else 0)) % 8 - 3.5
            cy = y % 4 - 1.5
            d = abs(cx) * 0.5 + abs(cy)
            c = BRIGHT if d < 1.2 else MID if d < 2.2 else DARK
            t.set(x, y, jitter(c, 6))
    t.save(os.path.join(ASSETS, "block", "hive_exit.png"))


def shrunk_icon():
    t = Tex(18, 18)
    HONEY = hexc("#f2a71b")
    DARK = hexc("#3b2752")
    # Two inward arrows pointing at a small bee dot.
    for i in range(5):
        t.set(1 + i, 1 + i, HONEY)
        t.set(16 - i, 16 - i, HONEY)
        t.set(16 - i, 1 + i, HONEY)
        t.set(1 + i, 16 - i, HONEY)
    for x in range(7, 11):
        for y in range(7, 11):
            t.set(x, y, HONEY if y % 2 == 0 else DARK)
    t.save(os.path.join(ASSETS, "mob_effect", "shrunk.png"))


if __name__ == "__main__":
    queen()
    guard_gear()
    shrinking_honey()
    spawn_egg("guard_bee_spawn_egg", hexc("#e7b52f"), hexc("#7c5418"))
    spawn_egg("queen_bee_spawn_egg", hexc("#3b2752"), hexc("#e0c160"))
    hive_exit()
    shrunk_icon()
    print("textures written")
