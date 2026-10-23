# Subset nasin nanpa to the glyphs listed in data/words.json.
# usage: python3 scripts/subset_font.py SOURCE OUT [OUT ...]
#   SOURCE: nasin-nanpa-4.0.2-UCSUR.otf (or an existing subset, e.g. fonts/nasin-nanpa-subset.woff2)
#   OUT:    .otf for iOS, .woff2 for the web prototype
import json, os, sys
from fontTools import subset
from fontTools.ttLib import TTFont

root = os.path.join(os.path.dirname(__file__), "..")
words = json.load(open(os.path.join(root, "data/words.json")))
cps = [int(w["ucsur"][2:], 16) for w in words if w["ucsur"]]

for out in sys.argv[2:]:
    font = TTFont(sys.argv[1])
    opts = subset.Options()
    opts.flavor = "woff2" if out.endswith(".woff2") else None
    opts.layout_features = []   # one glyph per codepoint: no alternates or ligatures pulled in
    sub = subset.Subsetter(opts)
    sub.populate(unicodes=cps)
    sub.subset(font)
    font.flavor = opts.flavor
    font.save(out)
    print(out, len(font.getBestCmap()), "glyphs")
