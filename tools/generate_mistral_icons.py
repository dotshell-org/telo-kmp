#!/usr/bin/env python3
"""Generate every Réseau Mistral line badge as a Compose vector drawable.

The Mistral network has no public-domain line pictograms; its official
visual identity (reseaumistral.com, printed maps) renders each line as a
rounded square filled with the line color and the line name centered in
the text color. Both colors come from the GTFS routes.txt (route_color /
route_text_color, Licence Ouverte v2.0), so the badges are regenerable at
every GTFS update and new lines get a badge automatically.

Glyph outlines are extracted from Segoe UI Bold with fontTools and baked
into the drawable as pathData (vector drawables cannot render text).

Outputs one XML per route into app/src/commonMain/composeResources/drawable/,
named by the LineIconResolver convention (lowercase, "_" prefix when the
name starts with a digit): U→u.xml, T→t.xml, 8M→_8m.xml, 11B→_11b.xml…
Also writes a contact-sheet preview PNG for visual inspection when
--preview <path> is given.

Usage: python tools/generate_mistral_icons.py [--gtfs Z:/Android/Projects/GTFS-MISTRAL]
                                              [--preview badges_preview.png]
"""

import argparse
import csv
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.misc.transform import Transform
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
DRAWABLE_DIR = ROOT / "app/src/commonMain/composeResources/drawable"
FONT_PATH = Path(r"C:\Windows\Fonts\segoeuib.ttf")

VIEWPORT = 28.0          # dp, same square canvas as the former RTM pictograms
CORNER_RADIUS = 4.8      # ≈17% — matches the .ligne__badge rounding of the official site
MAX_TEXT_WIDTH = 20.0    # text box: keep side padding
MAX_CAP_HEIGHT = 13.0    # cap on glyph height for 1-2 character names


def drawable_name(line_name: str) -> str:
    """Mirror of LineIconResolver.getDrawableNameForLineName."""
    lower = line_name.lower()
    return ("_" + lower) if lower[:1].isdigit() else lower


class BadgeFont:
    def __init__(self, path: Path):
        self.font = TTFont(str(path))
        self.cmap = self.font.getBestCmap()
        self.glyphs = self.font.getGlyphSet()
        self.upem = self.font["head"].unitsPerEm
        os2 = self.font["OS/2"]
        self.cap_height = getattr(os2, "sCapHeight", 0) or int(self.upem * 0.7)

    def advance(self, char: str) -> int:
        return self.glyphs[self.cmap[ord(char)]].width

    def text_path(self, text: str, scale: float, x: float, baseline_y: float) -> str:
        """SVG path data for `text`, scaled and placed in viewport coordinates."""
        parts = []
        cursor = x
        for char in text:
            glyph = self.glyphs[self.cmap[ord(char)]]
            pen = SVGPathPen(self.glyphs, ntos=lambda v: f"{v:.2f}")
            # Font units are y-up; the viewport is y-down: flip and translate.
            glyph.draw(TransformPen(pen, Transform(scale, 0, 0, -scale, cursor, baseline_y)))
            data = pen.getCommands()
            if data:
                parts.append(data)
            cursor += glyph.width * scale
        return " ".join(parts)


def rounded_square_path(size: float, radius: float) -> str:
    s, r = size, radius
    k = r * 0.4477  # quadratic control pull-in for a circle-ish corner
    return (
        f"M{r:.2f},0 L{s - r:.2f},0 Q{s - k:.2f},0 {s:.2f},{r:.2f} L{s:.2f},{s - r:.2f} "
        f"Q{s:.2f},{s - k:.2f} {s - r:.2f},{s:.2f} L{r:.2f},{s:.2f} Q{k:.2f},{s:.2f} 0,{s - r:.2f} "
        f"L0,{r:.2f} Q0,{k:.2f} {r:.2f},0 Z"
    )


def badge_xml(font: BadgeFont, name: str, bg_color: str, text_color: str) -> str:
    # Fit: height-capped for short names, width-capped for long ones.
    total_advance = sum(font.advance(c) for c in name)
    scale = min(MAX_CAP_HEIGHT / font.cap_height, MAX_TEXT_WIDTH / total_advance)
    text_width = total_advance * scale
    x = (VIEWPORT - text_width) / 2
    baseline_y = (VIEWPORT + font.cap_height * scale) / 2
    text_data = font.text_path(name, scale, x, baseline_y)

    return (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="28dp"\n'
        '    android:height="28dp"\n'
        f'    android:viewportWidth="{VIEWPORT:g}"\n'
        f'    android:viewportHeight="{VIEWPORT:g}">\n'
        '    <path\n'
        f'        android:pathData="{rounded_square_path(VIEWPORT, CORNER_RADIUS)}"\n'
        f'        android:fillColor="#FF{bg_color}" />\n'
        '    <path\n'
        f'        android:pathData="{text_data}"\n'
        f'        android:fillColor="#FF{text_color}" />\n'
        '</vector>\n'
    )


def write_preview(routes, path: Path):
    """Contact sheet rendered with the same font/colors, for visual inspection."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.font_manager import FontProperties
    from matplotlib.patches import FancyBboxPatch

    prop = FontProperties(fname=str(FONT_PATH))
    cols = 9
    rows = (len(routes) + cols - 1) // cols
    fig, axes = plt.subplots(rows, cols, figsize=(cols * 1.1, rows * 1.1))
    for ax in axes.flat:
        ax.set_axis_off()
    for ax, (name, bg, fg) in zip(axes.flat, routes):
        ax.set_xlim(0, 1)
        ax.set_ylim(0, 1)
        ax.add_patch(FancyBboxPatch((0.08, 0.08), 0.84, 0.84,
                                    boxstyle="round,pad=0,rounding_size=0.14",
                                    facecolor="#" + bg, edgecolor="none"))
        size = 26 if len(name) == 1 else (20 if len(name) == 2 else 14)
        ax.text(0.5, 0.5, name, ha="center", va="center", color="#" + fg,
                fontproperties=prop, fontsize=size)
    fig.savefig(path, dpi=110, bbox_inches="tight", facecolor="#e8e8e8")
    print(f"preview written to {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gtfs", default=r"Z:\Android\Projects\GTFS-MISTRAL")
    parser.add_argument("--preview", default=None)
    args = parser.parse_args()

    font = BadgeFont(FONT_PATH)

    with (Path(args.gtfs) / "routes.txt").open(encoding="utf-8-sig", newline="") as fh:
        routes = [
            (
                row["route_short_name"],
                (row["route_color"] or "003893").upper(),
                (row["route_text_color"] or "FFFFFF").upper(),
            )
            for row in sorted(csv.DictReader(fh),
                              key=lambda r: int(r.get("route_sort_order") or 0))
        ]

    for name, bg, fg in routes:
        target = DRAWABLE_DIR / (drawable_name(name) + ".xml")
        target.write_text(badge_xml(font, name, bg, fg), encoding="utf-8")
    print(f"{len(routes)} badges written to {DRAWABLE_DIR}")

    if args.preview:
        write_preview(routes, Path(args.preview))


if __name__ == "__main__":
    main()
