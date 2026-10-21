# toki pona Flick

A flick keyboard for [toki pona](https://tokipona.org/), modeled on the Japanese 12-key flick pad.
One tap or one flick types one whole word.

- **Tap** a key for the word in the middle; **flick** left, up, right or down for the words around it.
- Related words share a key (pronouns, grammar particles, prepositions, colors, body parts…).
- Where a sitelen pona glyph points somewhere, the word sits in that direction:
  `mi` ↓, `sina` ↑, `ona` ←, `pona` ↑ / `ike` ↓, `lili` ← / `suli` →, `monsi` ← / `sinpin` →, `anpa` ↓.
- Keys show sitelen pona glyphs; the keyboard types Latin text (`toki `, with the space added for you).

**Try the web prototype:** open `web/index.html` (or the GitHub Pages site, once enabled) on a phone.

## Layers

| Layer | Contents | Words |
|---|---|---|
| 1 | The most frequent words — about 87% of real toki pona text | 57 |
| 2 | Animals, plants, sky, matter, colors, body, people, feelings | 47 |
| 3 | Position, boxes & opening/closing, conflict, objects, shapes, money | 27 |

All 131 *core* and *common* words from lipu Linku are included. Empty slots are left for nimisin or personal picks.
The full layout is in [`data/layout.json`](data/layout.json): each key lists `[tap, left, up, right, down]`.

Side columns (like the iOS Japanese keyboard):

- Left: ABC (letters for names) · layer 2 · layer 3 · next keyboard
- Right: ⌫ (tap = one letter, flick = one word) · space · punctuation (tap `.`, ← `,`, ↑ `?`, → `!`, ↓ `:`) · return

## Roadmap

- [x] Word list and frequency ranking
- [x] Web prototype
- [ ] iOS keyboard extension (Swift)
- [ ] Optional output of sitelen pona (UCSUR) instead of Latin text

## Repository layout

```
web/index.html            web prototype (single file, font embedded)
data/words.json           131 words: frequency rank, Linku category, UCSUR codepoint, English/Japanese glosses
data/layout.json          the three layers
fonts/                    subset of the nasin nanpa font + its license
scripts/                  scripts used to build words.json and layout.json
```

## Credits and licenses

- Code: MIT License (see `LICENSE`).
- Word data, categories, glosses and glyph codepoints: [lipu Linku / sona](https://github.com/lipu-linku/sona), CC BY-SA 4.0. `data/words.json` is a derivative and is shared under CC BY-SA 4.0.
- Word frequency: [sona pona wiki – Word frequency](https://sona.pona.la/wiki/Word_frequency) (ilo Muni counts, September 2024).
- Font: [nasin nanpa](https://github.com/ETBCOR/nasin-nanpa) by jan Itan, MIT License (`fonts/LICENSE-nasin-nanpa.txt`).
