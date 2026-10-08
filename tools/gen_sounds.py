#!/usr/bin/env python3
"""Synthesizes the queen's babble syllables: one short, buzzy vowel per file.

Run from the repo root:  python3 tools/gen_sounds.py   (needs numpy and ffmpeg)

Each syllable is a sawtooth "voice" with a little vibrato and a bee-ish buzz, passed through two
resonant filters at that vowel's formant frequencies. The client strings them together a couple
of letters at a time, with the pitch nudged per letter, which gives the Animal Crossing effect.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hivemind", "sounds", "queen")
RATE = 44100

# (F1, F2, F3) formants in Hz, roughly an adult voice, shifted up a little to sound small.
VOWELS = {
    "a": (850, 1350, 2700),
    "e": (560, 2050, 2800),
    "i": (360, 2450, 3100),
    "o": (560, 1000, 2600),
    "u": (400, 900, 2400),
}


def resonator(signal, freq, bandwidth):
    """Two-pole resonant filter."""
    r = np.exp(-np.pi * bandwidth / RATE)
    theta = 2 * np.pi * freq / RATE
    a1 = -2 * r * np.cos(theta)
    a2 = r * r
    gain = 1 - r
    out = np.zeros_like(signal)
    y1 = y2 = 0.0
    for n, x in enumerate(signal):
        y = gain * x - a1 * y1 - a2 * y2
        out[n] = y
        y2, y1 = y1, y
    return out


def syllable(formants, duration=0.085, f0=420.0):
    n = int(RATE * duration)
    t = np.arange(n) / RATE
    # Slight downward glide and vibrato, like a quick chirp of a word.
    freq = f0 * (1.08 - 0.12 * t / duration) * (1 + 0.02 * np.sin(2 * np.pi * 28 * t))
    phase = np.cumsum(freq) / RATE
    saw = 2 * (phase - np.floor(phase + 0.5))
    # Wing buzz: a fast amplitude wobble.
    buzz = 0.82 + 0.18 * np.sin(2 * np.pi * 190 * t)
    source = saw * buzz
    voice = sum(resonator(source, f, 90 + f * 0.06) * w for f, w in zip(formants, (1.0, 0.7, 0.25)))
    attack = int(RATE * 0.008)
    release = int(RATE * 0.035)
    env = np.ones(n)
    env[:attack] = np.linspace(0, 1, attack)
    env[-release:] = np.linspace(1, 0, release) ** 1.5
    voice *= env
    return voice / np.max(np.abs(voice)) * 0.8


def write_ogg(path, samples):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes((samples * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav_path)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, formants in VOWELS.items():
        write_ogg(os.path.join(OUT, "babble_%s.ogg" % name), syllable(formants))
    print("sounds written")
