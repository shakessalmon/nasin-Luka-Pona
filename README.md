# nasin Luka Pona

*nasin luka pona* — “the good way of the hand”: a flick keyboard for [toki pona](https://tokipona.org/), modeled on the Japanese 12-key flick pad.
One tap or one flick types one whole word.

- **Tap** a key for the word in the middle; **flick** left, up, right or down for the words around it.
- While you hold a key, its words appear as a cross around it, and the one under your finger is highlighted.
- Related words share a key (pronouns, grammar particles, prepositions, colors, body parts…).
- Where a sitelen pona glyph points somewhere, the word sits in that direction:
  `mi` ↓, `sina` ↑, `ona` ←, `ike` ↓ (`pona` is the tap on that key), `lili` ← / `suli` →, `monsi` ← / `sinpin` →, `anpa` ↓.
- Keys show sitelen pona glyphs; the keyboard types Latin text (`toki `, with the space added for you).

**Try the web prototype:** open `web/index.html` on a phone, or the GitHub Pages site: https://shakessalmon.github.io/nasin-Luka-Pona/web/.

## Layers

| Layer | Contents | Words |
|---|---|---|
| 1 | The most frequent words — about 87% of real toki pona text | 57 |
| 2 | Animals, plants, sky, matter, colors, body, people, feelings | 47 |
| 3 | Position, boxes & opening/closing, conflict, objects, shapes, money | 27 |

All 131 *core* and *common* words from lipu Linku are included. Empty slots are left for nimisin or personal picks.
The full layout is in [`data/layout.json`](data/layout.json): each key lists `[tap, left, up, right, down]`.

Side columns (like the iOS Japanese keyboard):

- Left: ABC (letters for names, in the sronhatie layout from [Thumb-Key](https://github.com/dessalines/thumb-key); the first letter of a word is capitalized for you) · layer 2 · layer 3 (tap = one word, flick = keep the layer open) · next keyboard
- Right: ⌫ (tap = one letter, hold = keep deleting, flick = one word) · space (flick = `:`) · punctuation (tap `.`, ← `,`, ↑ `?`, ↓ `!`) · return

## iOS keyboard

`ios/NasinLukaPona.xcodeproj` builds a small app and the keyboard extension inside it.
The keyboard reads `data/layout.json` and `data/words.json` and uses `fonts/nasin-nanpa-subset.otf` for the glyphs, so it always matches the data files.
It does not need Full Access.

On the keyboard, tap the globe key for the next keyboard, or flick it to switch the keys between glyphs and Latin letters.
Hold ⌫ to keep deleting letters. Keys click when Keyboard Clicks is on in Settings › Sounds & Haptics, and VoiceOver reads each key, with its flicks as actions.

### Install on your iPhone with a free Apple ID

1. Open `ios/NasinLukaPona.xcodeproj` in Xcode.
2. Xcode › Settings › Accounts: add your Apple ID.
3. Select the project, then for **both** targets (NasinLukaPona and NasinLukaPonaKeyboard) open Signing & Capabilities and set Team to your Personal Team.
   If Xcode says the bundle identifier is not available, change `com.shakessalmon.nasinlukapona` to your own in both targets (the keyboard's must start with the app's, followed by `.keyboard`).
4. Connect the iPhone with a cable. On the iPhone, turn on Settings › Privacy & Security › Developer Mode (it appears after the iPhone has been connected to Xcode once) and restart.
5. Choose the iPhone as the run destination and press Run.
6. The first time, trust your certificate on the iPhone: Settings › General › VPN & Device Management.
7. Settings › General › Keyboard › Keyboards › Add New Keyboard… › nasin Luka Pona.

With a free Apple ID the app stops working after 7 days. Press Run in Xcode again to reinstall it.

## Roadmap

- [x] Word list and frequency ranking
- [x] Web prototype
- [x] iOS keyboard extension (Swift) — first version, needs testing on a device
- [ ] Optional output of sitelen pona (UCSUR) instead of Latin text

## Repository layout

```
web/index.html            web prototype (single file, font embedded)
data/words.json           131 words: frequency rank, Linku category, UCSUR codepoint, English/Japanese glosses
data/layout.json          the three layers
fonts/                    subset of the nasin nanpa font (.woff2 for the web, .otf for iOS) + its license
ios/                      Xcode project: container app (App/) and keyboard extension (Keyboard/)
scripts/                  scripts used to build words.json, layout.json and the font subsets
```

## Credits and licenses

- Code: MIT License (see `LICENSE`).
- Word data, categories, glosses and glyph codepoints: [lipu Linku / sona](https://github.com/lipu-linku/sona), CC BY-SA 4.0. `data/words.json` is a derivative and is shared under CC BY-SA 4.0.
- Word frequency: [sona pona wiki – Word frequency](https://sona.pona.la/wiki/Word_frequency) (ilo Muni counts, September 2024).
- Font: [nasin nanpa](https://github.com/ETBCOR/nasin-nanpa) by jan Itan, MIT License (`fonts/LICENSE-nasin-nanpa.txt`).
