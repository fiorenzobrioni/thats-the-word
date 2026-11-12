#!/usr/bin/env python3
"""Draws the game's launcher mark: the adaptive icon's three layers in app/src/main/res/drawable.

    python3 tools/draw_launcher_icon.py

PROVISIONAL (Phase 0, 12 Nov 2026): the mark and the app's colour are the owner's to choose
(PLANNING.md §15, Open 4). This first drawing follows the family's series of 9 Nov 2026 so the APK
has a mark of the family's kind: a white speech bubble holding a word of three tiles, two still
empty (cut out, the ground shows through) and the last one lit in amber, the letter just found.
Looked at, it reads like a messaging app's "typing" bubble: a placeholder until the owner chooses.

The series' rules, the same in every app of the family:
- the badge's ground is the app's own colour, from its light tone at the upper left to its deep
  tone at the lower right; the symbol is white with one amber accent;
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to a darker one at the lower right, over the box's larger side; no shadow (the store and
  the launchers add their own);
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches it.

Re-running this script IS the drawing: ic_launcher_background.xml, ic_launcher_foreground.xml and
ic_launcher_monochrome.xml are its output and are not edited by hand. Only the standard library is
used, so any Python 3 runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"

CENTRE = 54.0
KEYLINE = 28.0

# The family's palette, the tones this mark uses: (light, base, deep). BERRY is the provisional
# app colour: away from the sisters' green, coral and indigo (PLANNING.md §15, Open 4).
BERRY = ("D86BBE", "AE3790", "7E1D68")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")

# The drawing, on the 108 grid.
BUBBLE = (32.0, 31.0, 76.0, 61.0, 11.0)  # left, top, right, bottom, corner
TAIL = ((39.0, 59.0), (35.5, 71.0), (51.0, 59.0))  # the bubble's tail, at its lower left
TAIL_ROUND = 2.5
TILE = 9.5
TILE_GAP = 3.0
TILE_CORNER = 2.4
TILES_Y = 46.0  # the tiles' middle
MONO_INSET = 2.6  # in the one-ink layer, the lit tile is a smaller solid square in its hole

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def lit(box):
    x0, y0, x1, y1 = box
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2
    return (cx - r, cy - r), (cx + r, cy + r)


def ramp(attr, box, light, deep, indent="    "):
    (sx, sy), (ex, ey) = lit(box)
    return (
        f'{indent}<aapt:attr name="android:{attr}">\n'
        f'{indent}    <gradient android:type="linear" android:startX="{f(sx)}" android:startY="{f(sy)}" '
        f'android:endX="{f(ex)}" android:endY="{f(ey)}">\n'
        f'{indent}        <item android:offset="0" android:color="#FF{light}"/>\n'
        f'{indent}        <item android:offset="1" android:color="#FF{deep}"/>\n'
        f"{indent}    </gradient>\n{indent}</aapt:attr>\n"
    )


def rrect(x0, y0, x1, y1, r):
    return (
        f"M {f(x0 + r)},{f(y0)} H {f(x1 - r)} A {f(r)},{f(r)} 0 0 1 {f(x1)},{f(y0 + r)} "
        f"V {f(y1 - r)} A {f(r)},{f(r)} 0 0 1 {f(x1 - r)},{f(y1)} H {f(x0 + r)} "
        f"A {f(r)},{f(r)} 0 0 1 {f(x0)},{f(y1 - r)} V {f(y0 + r)} A {f(r)},{f(r)} 0 0 1 {f(x0 + r)},{f(y0)} Z"
    )


def tiles():
    width = 3 * TILE + 2 * TILE_GAP
    left = CENTRE - width / 2
    top = TILES_Y - TILE / 2
    return [(left + i * (TILE + TILE_GAP), top, left + i * (TILE + TILE_GAP) + TILE, top + TILE) for i in range(3)]


def tail():
    (ax, ay), (bx, by), (cx, cy) = TAIL
    return f"M {f(ax)},{f(ay)} L {f(bx)},{f(by)} L {f(cx)},{f(cy)} Z"


def vector(body, size=108, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}</vector>\n"
    )


def reach():
    """The symbol's farthest point from the centre: the bubble's corners and the tail's tip."""
    x0, y0, x1, y1, r = BUBBLE
    corners = [(x0 + r, y0 + r), (x1 - r, y0 + r), (x1 - r, y1 - r), (x0 + r, y1 - r)]
    far = max(math.hypot(x - CENTRE, y - CENTRE) + r for x, y in corners)
    tip = max(math.hypot(x - CENTRE, y - CENTRE) + TAIL_ROUND / 2 for x, y in TAIL)
    return max(far, tip)


def background():
    body = f'<path android:pathData="M 0,0 H 108 V 108 H 0 Z">\n{ramp("fillColor", (0, 0, 108, 108), BERRY[0], BERRY[2])}</path>\n'
    return HEADER.format(what="The badge's ground: the app's colour, lit from the upper left.") + vector(body)


def foreground():
    x0, y0, x1, y1, r = BUBBLE
    box = (x0, y0, x1, TAIL[1][1])
    holes = " ".join(rrect(*t, TILE_CORNER) for t in tiles()[:2])
    bubble = (
        f'<path android:pathData="{rrect(x0, y0, x1, y1, r)} {holes}" android:fillType="evenOdd">\n'
        f'{ramp("fillColor", box, WHITE[0], WHITE[2])}</path>\n'
    )
    tail_path = (
        f'<path android:pathData="{tail()}" android:strokeWidth="{f(TAIL_ROUND)}" android:strokeLineJoin="round">\n'
        f'{ramp("fillColor", box, WHITE[0], WHITE[2])}{ramp("strokeColor", box, WHITE[0], WHITE[2])}</path>\n'
    )
    lit_tile = tiles()[2]
    amber = f'<path android:pathData="{rrect(*lit_tile, TILE_CORNER)}">\n{ramp("fillColor", lit_tile, SUN[0], SUN[2])}</path>\n'
    what = "The mark (provisional): a speech bubble holding a word, its last tile lit in amber."
    return HEADER.format(what=what) + vector(bubble + tail_path + amber)


def monochrome():
    x0, y0, x1, y1, r = BUBBLE
    holes = " ".join(rrect(*t, TILE_CORNER) for t in tiles())
    lx0, ly0, lx1, ly1 = tiles()[2]
    inner = rrect(lx0 + MONO_INSET, ly0 + MONO_INSET, lx1 - MONO_INSET, ly1 - MONO_INSET, TILE_CORNER / 2)
    body = (
        f'<path android:pathData="{rrect(x0, y0, x1, y1, r)} {holes}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF"/>\n'
        f'<path android:pathData="{tail()}" android:fillColor="#FFFFFFFF" android:strokeColor="#FFFFFFFF" '
        f'android:strokeWidth="{f(TAIL_ROUND)}" android:strokeLineJoin="round"/>\n'
        f'<path android:pathData="{inner}" android:fillColor="#FFFFFFFF"/>\n'
    )
    return HEADER.format(what="The themed icon's layer: the same mark in one ink, the lit tile a solid square.") + vector(body, aapt=False)


def main():
    measured = reach()
    assert measured <= KEYLINE, f"the symbol reaches {measured:.1f}, past the keyline {KEYLINE}"
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_background.xml").write_text(background(), encoding="utf-8", newline="\n")
    (RES / "ic_launcher_foreground.xml").write_text(foreground(), encoding="utf-8", newline="\n")
    (RES / "ic_launcher_monochrome.xml").write_text(monochrome(), encoding="utf-8", newline="\n")
    print(f"launcher mark written; the symbol reaches {measured:.1f} of {KEYLINE}")


if __name__ == "__main__":
    main()
