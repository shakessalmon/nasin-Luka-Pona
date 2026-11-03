# Draw the app icon: the sitelen pona glyph for "nasin" in white on the accent green.
# usage: python3 scripts/make_icon.py OUT.svg [--square]
#   default: rounded square (favicon); --square: full-bleed square (iOS masks the corners itself)
import json, os, sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.transformPen import TransformPen

root = os.path.join(os.path.dirname(__file__), "..")
font = TTFont(os.path.join(root, "fonts/nasin-nanpa-subset.otf"))
cp = int(next(w for w in json.load(open(os.path.join(root, "data/words.json"))) if w["word"] == "nasin")["ucsur"][2:], 16)
glyph = font.getGlyphSet()[font.getBestCmap()[cp]]

SIZE, FILL = 512, 0.62          # the glyph's longer side takes 62% of the icon
bounds = BoundsPen(None); glyph.draw(bounds)
x0, y0, x1, y1 = bounds.bounds
scale = SIZE * FILL / max(x1 - x0, y1 - y0)
dx = (SIZE - (x1 - x0) * scale) / 2 - x0 * scale
dy = (SIZE - (y1 - y0) * scale) / 2 + y1 * scale   # font y points up, SVG y points down
pen = SVGPathPen(None)
glyph.draw(TransformPen(pen, (scale, 0, 0, -scale, dx, dy)))
radius = 0 if "--square" in sys.argv else 112
svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {SIZE} {SIZE}">'
       f'<rect width="{SIZE}" height="{SIZE}" rx="{radius}" fill="#1f6f5c"/>'
       # a white outline thickens the font's thin strokes, so the glyph still reads at 16 px
       f'<path d="{pen.getCommands()}" fill="#ffffff" stroke="#ffffff" stroke-width="18" stroke-linejoin="round"/></svg>')
open(sys.argv[1], "w").write(svg)
print(sys.argv[1], len(svg), "bytes")
