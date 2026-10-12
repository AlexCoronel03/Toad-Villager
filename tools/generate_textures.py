"""Builds the six Toad cap-colour textures from the hand-painted base atlas.

tools/source/toad_base_red.png is the painted 128x128 atlas for the red variant (it matches the UV layout of
toad.geo.json). The other five variants are produced by recolouring ONLY the red cap-patch pixels (pixels whose
hue is red and saturated); skin, white cap, vest, trim and boots stay byte-for-byte identical.

    python tools/generate_textures.py
"""
import colorsys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "tools/source/toad_base_red.png"
ASSETS = ROOT / "src/main/resources/assets/toadmod"
OUT = ASSETS / "textures/entity/toad"

# Target hue (0..1) plus saturation / value multipliers applied on top of the red ramp.
VARIANTS = {
    "red": None,
    "green": (0.355, 0.95, 0.82),
    "magenta": (0.865, 0.95, 0.93),
    "blue": (0.625, 0.98, 0.95),
    "lime": (0.215, 0.92, 1.00),
    "yellow": (0.135, 0.95, 1.12),
}

RED_HUE_TOLERANCE = 0.045
RED_MIN_SATURATION = 0.55


def is_patch_pixel(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
    near_red = h <= RED_HUE_TOLERANCE or h >= 1.0 - RED_HUE_TOLERANCE
    return near_red and s >= RED_MIN_SATURATION and v > 0.15


def recolor(base, hue, sat_mul, val_mul):
    out = base.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and is_patch_pixel(r, g, b):
                _, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
                nr, ng, nb = colorsys.hsv_to_rgb(hue, min(1.0, s * sat_mul), min(1.0, v * val_mul))
                px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def spawn_egg():
    """16x16 inventory icon: the cap over the face, patch in classic red."""
    rows = [
        "................",
        "....OOOOOOOO....",
        "...OwwwwwwwwO...",
        "..OwwrrrrrrwwO..",
        "..OwrRRRRRRrwO..",
        ".OwwrRRRRRRrwwO.",
        ".OwwrRRRRRRrwwO.",
        ".OwwwrrrrrrwwwO.",
        ".OwwwwwwwwwwwwO.",
        ".OsssssssssssO..",
        "..OsEsssssEsO...",
        "..OsEsssssEsO...",
        "...OssSSSSsO....",
        "....OOOOOOO.....",
        "................",
        "................",
    ]
    key = {
        "O": (60, 40, 40),
        "w": (250, 250, 252),
        "r": (255, 100, 92),
        "R": (222, 32, 36),
        "s": (253, 205, 165),
        "S": (216, 150, 112),
        "E": (16, 12, 18),
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in key:
                img.putpixel((x, y), key[ch] + (255,))
    return img


def main():
    base = Image.open(BASE).convert("RGBA")
    OUT.mkdir(parents=True, exist_ok=True)
    for name, params in VARIANTS.items():
        tex = base.copy() if params is None else recolor(base, *params)
        tex.save(OUT / f"toad_{name}.png")
        print("wrote", OUT / f"toad_{name}.png")
    item_dir = ASSETS / "textures/item"
    item_dir.mkdir(parents=True, exist_ok=True)
    spawn_egg().save(item_dir / "toad_spawn_egg.png")
    print("wrote", item_dir / "toad_spawn_egg.png")


if __name__ == "__main__":
    main()
