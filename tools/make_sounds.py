#!/usr/bin/env python3
"""Writes That's The Word's sounds: core/designsystem/src/main/res/raw/sound_*.wav.

    python3 tools/make_sounds.py            # write them
    python3 tools/make_sounds.py --check    # fail if a rebuild would write other bytes

The sounds are made here, from code, so the game carries no third-party audio and no licence to
credit (PLANNING.md §1, §7.5). Python's standard library only; the same bytes on every run, so the
files are this script's output and are never edited by hand: change the script and run it again.

What they are (PLANNING.md §7.5): soft and short, a wooden instrument rather than a buzzer.

- key: a letter set on a tile; a muted wooden tick, barely there, heard a hundred times a game.
- right, elsewhere, absent: a tile revealed, a marimba's note a step of the scale per mark, so a
  row revealed plays as a short phrase: the right place highest and brightest, elsewhere a fifth
  under it, absent a low muted note. A row of five, 70 ms apart, is a little melody of its result.
- refusal: a word refused; two low muted notes falling a tone, never an alarm.
- win: a word found; the rising arpeggio of the scale, on a bell over the marimba.
- milestone: a milestone said; a slow shimmer of bells, softer than the win.

Each is 22,050 Hz, 16-bit mono PCM: small (a few kilobytes each), decoded once by SoundPool while
the game is on screen.
"""

import argparse
import math
import struct
import sys
import wave
from io import BytesIO
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "core/designsystem/src/main/res/raw"
RATE = 22_050

# The scale: C major pentatonic around C5, so any notes the game plays together agree.
C5 = 523.25
D5 = 587.33
E5 = 659.25
G4 = 392.00
G5 = 783.99
A4 = 440.00
C6 = 1046.50
E6 = 1318.51
G6 = 1567.98


def silence(seconds):
    return [0.0] * int(seconds * RATE)


def mix(*tracks):
    """Tracks summed sample by sample; each is (start seconds, samples)."""
    length = max(int(start * RATE) + len(samples) for start, samples in tracks)
    out = [0.0] * length
    for start, samples in tracks:
        offset = int(start * RATE)
        for i, value in enumerate(samples):
            out[offset + i] += value
    return out


def envelope(n, attack, decay):
    """A struck instrument: a short rise (no click), then an exponential fall at [decay] per second."""
    rise = max(1, int(attack * RATE))
    return [
        (i / rise if i < rise else 1.0) * math.exp(-(i / RATE) * decay)
        for i in range(n)
    ]


def partials(freq, seconds, spectrum, attack=0.004):
    """A note from its partials: (ratio to [freq], level, decay per second)."""
    n = int(seconds * RATE)
    out = [0.0] * n
    for ratio, level, decay in spectrum:
        f = freq * ratio
        if f >= RATE / 2:
            continue
        env = envelope(n, attack, decay)
        step = 2 * math.pi * f / RATE
        for i in range(n):
            out[i] += level * env[i] * math.sin(step * i)
    return out


def marimba(freq, seconds=0.45, bright=1.0):
    # A bar's partials are not harmonic: the fourth (about 3.9x) and the tenth (about 9.2x) ring
    # briefly over the fundamental; [bright] scales them, the absent note's mallet is soft.
    return partials(
        freq,
        seconds,
        [(1.0, 1.0, 9.0), (3.93, 0.32 * bright, 28.0), (9.2, 0.08 * bright, 55.0)],
    )


def bell(freq, seconds=1.2, decay=4.0):
    # A small bell's partials: the hum, the prime, the minor third and the fifth above, the octave.
    return partials(
        freq,
        seconds,
        [(1.0, 1.0, decay), (2.0, 0.55, decay * 1.4), (2.4, 0.25, decay * 2), (3.0, 0.2, decay * 2.2),
         (4.2, 0.12, decay * 3)],
        attack=0.003,
    )


def tick():
    """The key: a very short noise burst through a resonance, like a fingertip on wood."""
    n = int(0.045 * RATE)
    seed = 0x2545F491
    noise = []
    for _ in range(n):
        # A fixed linear congruential generator: the same "noise" on every run.
        seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF
        noise.append(seed / 0x3FFFFFFF - 1.0)
    # A two-pole resonator at 1.8 kHz shapes the noise into a short knock.
    f, r = 1800.0, 0.94
    a1, a2 = 2 * r * math.cos(2 * math.pi * f / RATE), -r * r
    out, y1, y2 = [], 0.0, 0.0
    env = envelope(n, 0.001, 140.0)
    for i in range(n):
        y = noise[i] * env[i] + a1 * y1 + a2 * y2
        out.append(y)
        y1, y2 = y, y1
    body = partials(C6, 0.045, [(1.0, 0.6, 120.0)], attack=0.001)
    return [a * 0.12 + b for a, b in zip(out, body)]


def normalise(samples, peak):
    top = max(abs(s) for s in samples) or 1.0
    # A short fade at the very end, so no sound stops on a click.
    fade = int(0.01 * RATE)
    out = [s * peak / top for s in samples]
    for i in range(min(fade, len(out))):
        out[-1 - i] *= i / fade
    return out


def wav_bytes(samples):
    buffer = BytesIO()
    with wave.open(buffer, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", max(-32767, min(32767, round(s * 32767)))) for s in samples))
    return buffer.getvalue()


def sounds():
    """Each sound by its file name, at its level: the key quietest, the win the loudest."""
    return {
        "sound_key": normalise(tick(), 0.30),
        "sound_right": normalise(marimba(E5, 0.42, bright=1.2), 0.50),
        "sound_elsewhere": normalise(marimba(A4, 0.40), 0.46),
        "sound_absent": normalise(marimba(G4 / 2, 0.30, bright=0.3), 0.40),
        "sound_refusal": normalise(
            mix((0.0, marimba(D5 / 2, 0.22, bright=0.4)), (0.11, marimba(C5 / 2, 0.30, bright=0.4))),
            0.42,
        ),
        "sound_win": normalise(
            mix(
                (0.00, marimba(C5, 0.5)),
                (0.09, marimba(E5, 0.5)),
                (0.18, marimba(G5, 0.5)),
                (0.27, bell(C6, 1.1)),
                (0.27, marimba(C6, 0.6, bright=0.6)),
            ),
            0.60,
        ),
        "sound_milestone": normalise(
            mix(
                (0.00, bell(G5, 1.4, decay=3.0)),
                (0.14, bell(C6, 1.4, decay=3.0)),
                (0.28, bell(E6, 1.4, decay=3.2)),
                (0.42, bell(G6, 1.3, decay=3.4)),
            ),
            0.50,
        ),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="fail if a rebuild would write other bytes")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    stale = []
    for name, samples in sounds().items():
        path = OUT / f"{name}.wav"
        data = wav_bytes(samples)
        if args.check:
            if not path.exists() or path.read_bytes() != data:
                stale.append(path.name)
        else:
            path.write_bytes(data)
            print(f"{path.relative_to(ROOT)}  {len(data):,} bytes")
    if stale:
        print("Not what the script writes: " + ", ".join(stale), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
