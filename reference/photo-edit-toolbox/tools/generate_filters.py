"""Generates the toolbox's own 17 filters as colour-cube look-up images.

Each output is a 512 x 512 PNG holding a 64 x 64 x 64 colour cube in 8 x 8 tiles of 64 x 64 pixels:
inside a tile x is the red level and y the green level, the tile number is the blue level. This is
the layout `FilterLut` reads. Every look is a plain formula below, so the images are ours, can be
reproduced exactly, and can be changed by editing a formula and running this script again.

Usage:  python tools/generate_filters.py          (writes into core/src/main/assets/filters/)
Needs:  Pillow and numpy.
"""
from pathlib import Path

import numpy as np
from PIL import Image

LEVELS = 64
TILES = 8
OUT = Path(__file__).resolve().parent.parent / "core" / "src" / "main" / "assets" / "filters"

LUMA = np.array([0.2126, 0.7152, 0.0722])


def luma(rgb):
    return (rgb * LUMA).sum(axis=-1, keepdims=True)


def saturate(rgb, amount):
    grey = luma(rgb)
    return grey + (rgb - grey) * amount


def contrast(rgb, amount):
    return (rgb - 0.5) * amount + 0.5


def s_curve(rgb, strength):
    """Smooth contrast: darkens shadows and lifts highlights, strength 0 leaves colours alone."""
    smooth = rgb * rgb * (3 - 2 * rgb)
    return rgb + (smooth - rgb) * strength


def tint(rgb, shadows, highlights):
    """Mixes a colour into the shadows and another into the highlights."""
    grey = luma(rgb)
    shadow_weight = (1 - grey) ** 2
    highlight_weight = grey ** 2
    return rgb + np.array(shadows) * shadow_weight + np.array(highlights) * highlight_weight


def warm(rgb):
    rgb = rgb * np.array([1.08, 1.0, 0.88]) + np.array([0.02, 0.0, -0.02])
    return saturate(s_curve(rgb, 0.2), 1.08)


def cool(rgb):
    rgb = rgb * np.array([0.9, 1.0, 1.1]) + np.array([-0.02, 0.0, 0.03])
    return saturate(s_curve(rgb, 0.15), 0.95)


def fade(rgb):
    rgb = 0.10 + rgb * 0.82
    rgb = tint(rgb, (0.02, 0.0, 0.03), (0.02, 0.0, -0.02))
    return saturate(rgb, 0.8)


def mono(rgb):
    return np.repeat(s_curve(luma(rgb), 0.35), 3, axis=-1)


def vivid(rgb):
    return saturate(s_curve(contrast(rgb, 1.08), 0.3), 1.4)


def sepia(rgb):
    grey = s_curve(luma(rgb), 0.2)
    return np.concatenate([grey * 1.08 + 0.03, grey * 0.95 + 0.01, grey * 0.78], axis=-1)


def noir(rgb):
    grey = contrast(luma(rgb), 1.2)
    grey = np.clip((grey - 0.04) / 0.96, 0.0, 1.0)
    return np.repeat(s_curve(grey, 0.6), 3, axis=-1)


def cinema(rgb):
    rgb = tint(s_curve(rgb, 0.25), (-0.05, 0.02, 0.06), (0.08, 0.03, -0.06))
    return saturate(rgb, 1.1)


def vintage(rgb):
    rgb = 0.06 + rgb * 0.88
    rgb = rgb * np.array([1.04, 1.02, 0.86])
    rgb = tint(rgb, (0.0, 0.03, 0.02), (0.04, 0.03, -0.04))
    return saturate(rgb, 0.85)


def pastel(rgb):
    rgb = 0.15 + rgb * 0.85
    rgb = tint(contrast(rgb, 0.9), (0.0, 0.0, 0.0), (0.03, 0.0, 0.02))
    return saturate(rgb, 0.7)


def rose(rgb):
    rgb = rgb * np.array([1.06, 0.96, 1.02])
    rgb = tint(rgb, (0.04, 0.0, 0.05), (0.05, 0.0, 0.02))
    return s_curve(rgb, 0.15)


def forest(rgb):
    rgb = rgb * np.array([0.95, 1.05, 0.95])
    rgb = tint(rgb, (-0.02, 0.03, 0.0), (0.0, 0.0, 0.0))
    return saturate(s_curve(rgb, 0.2), 1.1)


def cross(rgb):
    red = s_curve(rgb[..., 0:1], 0.6)
    green = s_curve(rgb[..., 1:2], 0.4)
    blue = 0.12 + rgb[..., 2:3] * 0.7
    return saturate(np.concatenate([red, green, blue], axis=-1), 1.15)


def bleach(rgb):
    rgb = saturate(rgb, 0.45)
    return contrast(s_curve(rgb, 0.55), 1.1)


def cyano(rgb):
    grey = s_curve(luma(rgb), 0.25)
    dark = np.array([0.03, 0.12, 0.25])
    light = np.array([0.85, 0.93, 1.0])
    return dark + (light - dark) * grey


def dusk(rgb):
    rgb = rgb * np.array([0.95, 0.9, 1.05]) * 0.96
    rgb = tint(rgb, (0.03, 0.0, 0.08), (0.06, 0.02, -0.02))
    return saturate(s_curve(rgb, 0.2), 0.95)


def autumn(rgb):
    """Warm and deep: every channel darkened by its own power, red least and blue most, so greys
    and pale blues turn brown and warm colours get richer; then a little darker and more contrast."""
    rgb = np.clip(rgb, 0.0, 1.0) ** np.array([1.05, 1.3, 1.6]) * 0.95
    return s_curve(rgb, 0.45)


FILTERS = {
    "warm": warm,
    "cool": cool,
    "fade": fade,
    "mono": mono,
    "vivid": vivid,
    "sepia": sepia,
    "noir": noir,
    "cinema": cinema,
    "vintage": vintage,
    "pastel": pastel,
    "rose": rose,
    "forest": forest,
    "cross": cross,
    "bleach": bleach,
    "cyano": cyano,
    "dusk": dusk,
    "autumn": autumn,
}


def identity_cube():
    """An array [blue][green][red] -> (r, g, b) in 0..1 of the colour each level stands for."""
    axis = np.linspace(0.0, 1.0, LEVELS)
    blue, green, red = np.meshgrid(axis, axis, axis, indexing="ij")
    return np.stack([red, green, blue], axis=-1)


def to_image(cube):
    """Lays the [blue][green][red] cube out as 8 x 8 tiles, tile = blue, x = red, y = green."""
    size = LEVELS * TILES
    pixels = np.zeros((size, size, 3), dtype=np.uint8)
    data = np.clip(np.rint(cube * 255.0), 0, 255).astype(np.uint8)
    for blue in range(LEVELS):
        left = (blue % TILES) * LEVELS
        top = (blue // TILES) * LEVELS
        pixels[top:top + LEVELS, left:left + LEVELS] = data[blue]
    return Image.fromarray(pixels)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    base = identity_cube()
    for name, look in FILTERS.items():
        to_image(look(base)).save(OUT / f"{name}.png", optimize=True)
        print("wrote", OUT / f"{name}.png")


if __name__ == "__main__":
    main()
