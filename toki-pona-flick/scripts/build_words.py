import tomllib, sys, os, json, csv
root = sys.argv[1]
md = os.path.join(root, "words/metadata")
def load(p):
    with open(p, "rb") as f: return tomllib.load(f)
ja = load(os.path.join(root, "words/translations/ja/definition.toml"))
en = load(os.path.join(root, "words/translations/en/definition.toml"))
glyphs = {}
for fn in os.listdir(os.path.join(root, "glyphs/metadata")):
    g = load(os.path.join(root, "glyphs/metadata", fn))
    glyphs[g["id"]] = g
rows = []
for fn in sorted(os.listdir(md)):
    w = load(os.path.join(md, fn))
    if w["usage_category"] not in ("core", "common"): continue
    u = w.get("usage", {})
    latest = u[max(u)] if u else None
    g = glyphs.get(w.get("primary_glyph_id", ""), {})
    rows.append(dict(word=w["word"], category=w["usage_category"], usage=latest,
        book=w.get("book",""), ucsur=g.get("ucsur",""), ja=ja.get(w["id"],""), en=en.get(w["id"],"")))
rows.sort(key=lambda r: (-(r["usage"] or 0), r["word"]))
json.dump(rows, open(sys.argv[2], "w"), ensure_ascii=False, indent=1)
print(len(rows))
for r in rows: print(r["usage"], r["category"], r["book"], r["word"], r["ucsur"], "|", r["ja"][:30])
