#!/usr/bin/env python3
"""Generate every Telo app icon asset from one vector definition.

Design (approved): white silhouette of Mont Faron on a black square —
the broad limestone massif overlooking Toulon, flat undulating summit
plateau, steep flanks, three dark strata bands suggesting the cliff
faces, sitting on a gentle ground swell.

Outputs:
- telo-icon.png (1024, repo root, README)
- app/src/commonMain/composeResources/drawable/ic_launcher_foreground.png (1536)
- app/src/androidMain/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_launcher_foreground.png
- app/src/androidMain/res/mipmap-*/ic_launcher.png (rounded square) and
  ic_launcher_round.png (circle), alpha-masked like the originals
- app/src/androidMain/res/drawable/ic_launcher_monochrome.xml (themed icon)
- iosApp/Assets.xcassets/AppIcon.appiconset/appicon-1024.png

Usage: python tools/generate_app_icon.py   (run from the repo root)
"""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
CANVAS = 1024.0  # design space
SUPERSAMPLE = 4

WHITE = 255
BLACK = 0


# ─── Geometry helpers (design space → pixels) ────────────────────────────────


def quad_points(p0, c, p1, steps=48):
    """Flatten a quadratic bezier into points."""
    return [
        (
            (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t**2 * p1[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t**2 * p1[1],
        )
        for t in (i / steps for i in range(steps + 1))
    ]


class Painter:
    """Draws the design-space shapes onto a PIL canvas of size `px`."""

    def __init__(self, draw: ImageDraw.ImageDraw, px: float):
        self.d = draw
        self.k = px / CANVAS

    def _pt(self, p):
        return (p[0] * self.k, p[1] * self.k)

    def polygon(self, points, fill):
        self.d.polygon([self._pt(p) for p in points], fill=fill)

    def rect(self, x, y, w, h, fill, radius=0.0):
        box = [x * self.k, y * self.k, (x + w) * self.k, (y + h) * self.k]
        if radius > 0:
            self.d.rounded_rectangle(box, radius=radius * self.k, fill=fill)
        else:
            self.d.rectangle(box, fill=fill)

    def ellipse(self, cx, cy, rx, ry, fill):
        self.d.ellipse(
            [(cx - rx) * self.k, (cy - ry) * self.k, (cx + rx) * self.k, (cy + ry) * self.k],
            fill=fill,
        )


MASSIF = [
    (48, 856),
    (168, 646),
    (246, 506),
    (296, 408),
    (330, 352),
    (420, 330),
    (560, 324),
    (668, 336),
    (716, 366),
    (762, 442),
    (826, 566),
    (976, 856),
]


def draw_silhouette(p: Painter):
    """The Mont Faron silhouette (white), then the dark strata bands."""
    # The massif: faceted profile with the wide summit plateau
    p.polygon(MASSIF, WHITE)

    # Limestone strata: bands slicing in from the left ridge (the parts
    # overflowing the silhouette are black on black, hence invisible)
    p.rect(140, 394, 550, 20, BLACK, radius=10)
    p.rect(140, 446, 400, 17, BLACK, radius=8)
    p.rect(140, 496, 470, 15, BLACK, radius=7)


def render_square(px: int) -> Image.Image:
    """Full-bleed black square with the white silhouette, grayscale."""
    big = px * SUPERSAMPLE
    im = Image.new("L", (big, big), BLACK)
    draw_silhouette(Painter(ImageDraw.Draw(im), big))
    return im.resize((px, px), Image.LANCZOS)


def render_masked(px: int, round_mask: bool) -> Image.Image:
    """Legacy launcher icon: 6.25% inset, rounded-square or circular alpha mask."""
    big = px * SUPERSAMPLE
    art = Image.new("L", (big, big), BLACK)
    draw_silhouette(Painter(ImageDraw.Draw(art), big))

    inset = round(big * 0.0625)
    mask = Image.new("L", (big, big), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([inset, inset, big - inset, big - inset], fill=255)
    else:
        md.rounded_rectangle([inset, inset, big - inset, big - inset], radius=big * 0.1, fill=255)

    out = Image.merge("LA", (art, mask))
    return out.resize((px, px), Image.LANCZOS)


MONOCHROME_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="1024"
    android:viewportHeight="1024">
    <group
        android:pivotX="512"
        android:pivotY="512"
        android:scaleX="0.66"
        android:scaleY="0.66"
        android:translateY="-55">
        <path
            android:fillColor="#FFFFFFFF"
            android:fillType="evenOdd"
            android:pathData="M48,856 L168,646 L246,506 L296,408 L330,352 L420,330 L560,324 L668,336 L716,366 L762,442 L826,566 L976,856 Z
                M312,394 h378 v20 h-378 Z
                M285,446 h255 v17 h-255 Z
                M259,496 h351 v15 h-351 Z" />
    </group>
</vector>
"""


def main() -> None:
    res = ROOT / "app/src/androidMain/res"

    # README / repo icon
    render_square(1024).convert("RGB").save(ROOT / "telo-icon.png")

    # Compose resources foreground (used in-app: settings & consent screens)
    render_square(1536).save(
        ROOT / "app/src/commonMain/composeResources/drawable/ic_launcher_foreground.png"
    )

    # Adaptive-icon foreground per density (full-bleed, background is black too)
    for density, px in [("mdpi", 512), ("hdpi", 768), ("xhdpi", 1024), ("xxhdpi", 1536)]:
        render_square(px).save(res / f"drawable-{density}/ic_launcher_foreground.png")

    # Legacy mipmaps (baked masks, LA like the originals)
    for density, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        render_masked(px, round_mask=False).save(res / f"mipmap-{density}/ic_launcher.png")
        render_masked(px, round_mask=True).save(res / f"mipmap-{density}/ic_launcher_round.png")

    # Themed (monochrome) icon
    (res / "drawable/ic_launcher_monochrome.xml").write_text(MONOCHROME_XML, encoding="utf-8")

    # iOS app icon (opaque, no alpha recommended by Apple — RGB)
    render_square(1024).convert("RGB").save(
        ROOT / "iosApp/Assets.xcassets/AppIcon.appiconset/appicon-1024.png"
    )

    print("App icon assets generated.")


if __name__ == "__main__":
    main()
