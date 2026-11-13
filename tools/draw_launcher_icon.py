#!/usr/bin/env python3
"""Draws That's The Word's launcher mark: the adaptive icon's three layers in
app/src/main/res/drawable.

    python3 tools/draw_launcher_icon.py

The mark is THE WORD BEING COMPOSED (the owner's idea and choice, 12 Nov 2026; PLANNING.md §15,
«The mark»): four letter tiles spell WO / RD, three white and the last, the D, amber, in its place
in the grid but still turned by 10 degrees, as if it had just been set down. The name, the game and
the moment of the win in one drawing: the last letter lands, and that's the word.

The series' rules, the same in every app of the family (9 Nov 2026):
- the badge's ground is the app's own colour, here the berry, from its light tone at the upper left
  to its deep tone at the lower right; the symbol is white with one amber accent;
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to a darker one at the lower right, over the box's larger side; no shadow (the store and
  the launchers add their own); the three white tiles are lit as one shape;
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches it.

The letters are cut out of their tiles (the ground shows through them, in the themed icon too):
their outlines are Google Sans Bold's, the family's face (SIL Open Font License 1.1, licenses/),
with its overlapping contours merged, written below in font units centred on their bounds, so the
script needs only Python's standard library.

Re-running this script IS the drawing: ic_launcher_background.xml, ic_launcher_foreground.xml and
ic_launcher_monochrome.xml are its output and are not edited by hand; so is the daily reminder's
status-bar icon, core/reminder/src/main/res/drawable/ic_stat_word.xml (the same tiles in one ink, on
the 24-unit grid).
"""

import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
STATUS = ROOT / "core/reminder/src/main/res/drawable"

CENTRE = 54.0
KEYLINE = 28.0

# The family's palette, the tones this mark uses: (light, base, deep). BERRY is the app's colour:
# the free hue farthest from the sisters' green, coral and indigo (PLANNING.md §15, «The mark»).
BERRY = ("D86BBE", "AE3790", "7E1D68")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")

# The drawing, on the 108 grid: four tiles in two rows, centred by their weight.
TILE = 18.4
GAP = 3.4
CORNER = TILE * 0.22  # the game's tile: its corner 22% of its side, as LetterTile's
LETTER = TILE * 0.5  # a capital's height on its tile
TURN = 10.0  # the last tile's turn, in degrees: just set down
TILES = (("W", -1, -1), ("O", 1, -1), ("R", -1, 1))  # the white tiles: letter, column, row
LAST = ("D", 1, 1)  # the amber one
CAP = 716  # Google Sans' capital height, in its units

# Google Sans Bold's outlines, overlaps merged, in font units, centred on their bounds, y down.
GLYPHS = {
    "W": (
        "M-284.5 358 -474.5 -358H-324.5L-226.5 44L-214.5 119H-206.5L-190.5 44L-64.5 -358H68.5L191.5 "
        "44L207.5 117H215.5L227.5 44L324.5 -358H474.5L289.5 358H144.5L22.5 -74L6.5 -151H-1.5L-17.5 "
        "-74L-145.5 358Z"
    ),
    "O": (
        "M0 374Q-80 374 -148.5 345.5Q-217 317 -267.5 265.5Q-318 214 -346 146Q-374 78 -374 0Q-374 -78 "
        "-346 -146Q-318 -214 -267.5 -265.5Q-217 -317 -148.5 -345.5Q-80 -374 0 -374Q80 -374 148 -345.5 "
        "Q216 -317 267 -265.5Q318 -214 346 -146Q374 -78 374 0Q374 78 346 146Q318 214 267 265.5Q216 "
        "317 148 345.5Q80 374 0 374ZM0 246Q67 246 121 215Q175 184 207 128.5Q239 73 239 0Q239 -74 207 "
        "-129Q175 -184 121 -215Q67 -246 0 -246Q-66 -246 -120 -215Q-174 -184 -206 -129Q-238 -74 -238 0 "
        "Q-238 73 -206 128.5Q-174 184 -120 215Q-66 246 0 246Z"
    ),
    "R": (
        "M-269 358V-358H-8Q64 -358 117.5 -329Q171 -300 201 -249Q231 -198 231 -132Q231 -83 210.5 -42.5 "
        "Q190 -2 155 27Q120 56 74 72L269 350V358H114L-72 89H-134V358ZM-134 -35H-9Q22 -35 45 -48Q68 "
        "-61 81.5 -83.5Q95 -106 95 -134Q95 -160 83.5 -182.5Q72 -205 49 -219Q26 -233 -7 -233H-134Z"
    ),
    "D": (
        "M-305 358V-358H-63Q51 -358 133.5 -312.5Q216 -267 260.5 -186.5Q305 -106 305 0Q305 106 260.5 "
        "187Q216 268 133.5 313Q51 358 -63 358ZM-170 230H-69Q10 230 63 201.5Q116 173 142.5 121Q169 69 "
        "169 0Q169 -70 142.5 -121.5Q116 -173 63 -201.5Q10 -230 -69 -230H-170Z"
    ),
}

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def step():
    return (TILE + GAP) / 2


def tile_centre(column, row):
    return CENTRE + column * step(), CENTRE + row * step()


def lit(box):
    """A shape's light: from the upper-left corner of its box to the lower-right one, along the
    diagonal of a square on the box's larger side, so every shape is lit at the same angle."""
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


def glyph(letter, cx, cy):
    """The letter's outline, scaled to LETTER and centred on (cx, cy). Every command is absolute:
    H takes an x, V a y, the others (x, y) pairs."""
    s = LETTER / CAP
    out, command, axis = [], None, 0
    for token in re.findall(r"[A-Za-z]|-?\d+(?:\.\d+)?", GLYPHS[letter]):
        if token.isalpha():
            command, axis = token, 0
            out.append(token)
            continue
        v = float(token)
        if command == "H":
            out.append(f(cx + v * s))
        elif command == "V":
            out.append(f(cy + v * s))
        else:
            out.append(f(cx + v * s) if axis == 0 else f(cy + v * s))
            axis = 1 - axis
    return " ".join(out)


def tile_path(letter, cx, cy):
    """A tile with its letter as a hole: drawn with the even-odd rule."""
    h = TILE / 2
    return f"{rrect(cx - h, cy - h, cx + h, cy + h, CORNER)} {glyph(letter, cx, cy)}"


def reach():
    """The symbol's farthest point from the centre: a tile's corner arcs, the last one turned."""
    far = 0.0
    inner = TILE / 2 - CORNER
    for _, column, row in TILES + (LAST,):
        cx, cy = tile_centre(column, row)
        turn = math.radians(TURN) if (column, row) == LAST[1:] else 0.0
        for sx, sy in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
            dx, dy = sx * inner, sy * inner
            x = cx + dx * math.cos(turn) - dy * math.sin(turn)
            y = cy + dx * math.sin(turn) + dy * math.cos(turn)
            far = max(far, math.hypot(x - CENTRE, y - CENTRE) + CORNER)
    return far


def vector(body, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="108dp"\n    android:height="108dp"\n'
        f'    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        f"{body}</vector>\n"
    )


def turned(body, cx, cy):
    return (
        f'<group android:rotation="{TURN}" android:pivotX="{f(cx)}" android:pivotY="{f(cy)}">\n'
        f"{body}</group>\n"
    )


def background():
    body = f'<path android:pathData="M 0,0 H 108 V 108 H 0 Z">\n{ramp("fillColor", (0, 0, 108, 108), BERRY[0], BERRY[2])}</path>\n'
    return HEADER.format(what="The badge's ground: the app's berry, lit from the upper left.") + vector(body)


def foreground():
    h = TILE / 2
    reach_out = step() + h
    box = (CENTRE - reach_out, CENTRE - reach_out, CENTRE + reach_out, CENTRE + reach_out)
    whites = " ".join(tile_path(letter, *tile_centre(column, row)) for letter, column, row in TILES)
    body = f'<path android:pathData="{whites}" android:fillType="evenOdd">\n{ramp("fillColor", box, WHITE[0], WHITE[2])}</path>\n'
    letter, column, row = LAST
    cx, cy = tile_centre(column, row)
    last_box = (cx - h, cy - h, cx + h, cy + h)
    last = f'<path android:pathData="{tile_path(letter, cx, cy)}" android:fillType="evenOdd">\n{ramp("fillColor", last_box, SUN[0], SUN[2])}</path>\n'
    body += turned(last, cx, cy)
    what = "The mark: four tiles spell WO / RD, the last one amber, just set down."
    return HEADER.format(what=what) + vector(body)


def monochrome():
    whites = " ".join(tile_path(letter, *tile_centre(column, row)) for letter, column, row in TILES)
    body = f'<path android:pathData="{whites}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF"/>\n'
    letter, column, row = LAST
    cx, cy = tile_centre(column, row)
    last = f'<path android:pathData="{tile_path(letter, cx, cy)}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF"/>\n'
    body += turned(last, cx, cy)
    return HEADER.format(what="The themed icon's layer: the same tiles in one ink, their letters cut out.") + vector(
        body, aapt=False
    )


def status_bar():
    """The daily reminder's status-bar icon (PLANNING.md §8): the themed layer's tiles, white on
    transparent as the system tints it, scaled from the mark's 56 units to 20 of the 24-unit grid,
    2 units of air all round."""
    scale = 20.0 / (KEYLINE * 2)
    whites = " ".join(tile_path(letter, *tile_centre(column, row)) for letter, column, row in TILES)
    body = f'<path android:pathData="{whites}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF"/>\n'
    letter, column, row = LAST
    cx, cy = tile_centre(column, row)
    last = f'<path android:pathData="{tile_path(letter, cx, cy)}" android:fillType="evenOdd" android:fillColor="#FFFFFFFF"/>\n'
    body += turned(last, cx, cy)
    group = (
        f'<group android:pivotX="{f(CENTRE)}" android:pivotY="{f(CENTRE)}" android:scaleX="{scale:.4f}" '
        f'android:scaleY="{scale:.4f}" android:translateX="{f(12 - CENTRE)}" android:translateY="{f(12 - CENTRE)}">\n'
        f"{body}</group>\n"
    )
    what = "The daily reminder's status-bar icon: the mark's tiles in one ink, white, tinted by the system."
    return HEADER.format(what=what) + (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp"\n    android:height="24dp"\n'
        '    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
        f"{group}</vector>\n"
    )


def main():
    measured = reach()
    assert measured <= KEYLINE, f"the symbol reaches {measured:.1f}, past the keyline {KEYLINE}"
    RES.mkdir(parents=True, exist_ok=True)
    (RES / "ic_launcher_background.xml").write_text(background(), encoding="utf-8", newline="\n")
    (RES / "ic_launcher_foreground.xml").write_text(foreground(), encoding="utf-8", newline="\n")
    (RES / "ic_launcher_monochrome.xml").write_text(monochrome(), encoding="utf-8", newline="\n")
    STATUS.mkdir(parents=True, exist_ok=True)
    (STATUS / "ic_stat_word.xml").write_text(status_bar(), encoding="utf-8", newline="\n")
    print(f"launcher mark written; the symbol reaches {measured:.1f} of {KEYLINE}")


if __name__ == "__main__":
    main()
