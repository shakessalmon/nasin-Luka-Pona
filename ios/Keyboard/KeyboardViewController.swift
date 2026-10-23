import UIKit

/// The keyboard: a 3×4 flick pad of word keys with a side column on each edge, like the iOS Japanese 12-key pad.
final class KeyboardViewController: UIInputViewController {
    /// sronhatie (Thumb-Key's English layout) with 4-way flicks: the 9 most common letters are taps,
    /// the others are flicks toward the middle key, and h keeps the letters around it.
    private static let abc: [[String]] = [
        ["s", "", "", "w", "j"], ["r", "", "", "", "g"], ["o", "u", "", "", "b"],
        ["n", "", "", "m", ""], ["h", "k", "q", "p", "x"], ["a", "l", "", "", ""],
        ["t", "", "v", "c", ""], ["i", "", "f", "z", ""], ["e", "d", "y", "", ""],
        [KeyboardViewController.shift, "-", "~", "*", "%"], ["'", "\"", "(", ")", ""], ["@", "#", "/", "&", "_"],
    ]
    /// The ABC slot that switches the case of the last letter
    private static let shift = "a/A"
    /// [tap, ←, ↑, →, ↓]. → is left empty: this key is at the screen edge. The colon is on the space key.
    private static let punctuation = [".", ",", "?", "", "!"]
    private static let layerIcons = ["toki", "soweli", "poki"]
    private static let glyphsDefaultsKey = "showGlyphs"

    private enum Kind { case word, letter }

    /// 0, 1, 2 = word layers, 3 = ABC
    private var mode = 0
    private var showGlyphs = UserDefaults.standard.object(forKey: KeyboardViewController.glyphsDefaultsKey) as? Bool ?? true {
        didSet { UserDefaults.standard.set(showGlyphs, forKey: Self.glyphsDefaultsKey) }
    }
    private var keys: [FlickKey] = []
    private weak var heldKey: FlickKey?
    private let bubble = PreviewBubble()
    private var heightConstraint: NSLayoutConstraint?
    /// iPhone landscape: a lower keyboard with smaller labels
    private var compact = false

    private var proxy: UITextDocumentProxy { textDocumentProxy }
    private var before: String { proxy.documentContextBeforeInput ?? "" }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Palette.board
        let height = view.heightAnchor.constraint(equalToConstant: 236)
        height.priority = UILayoutPriority(999)
        height.isActive = true
        heightConstraint = height
        view.addSubview(bubble)
        rebuild()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // The system delays touches near the screen edges; keys there should react at once.
        view.window?.gestureRecognizers?.forEach { $0.delaysTouchesBegan = false }
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        let isCompact = traitCollection.verticalSizeClass == .compact
        guard isCompact != compact else { return }
        compact = isCompact
        heightConstraint?.constant = compact ? 180 : 236
        rebuild()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let pad: CGFloat = 5, gap: CGFloat = 5
        let weights: [CGFloat] = [0.7, 1, 1, 1, 0.7]
        let width = min(view.bounds.width, 520)
        let left = (view.bounds.width - width) / 2 + pad
        let unit = (width - pad * 2 - gap * 4) / weights.reduce(0, +)
        let rowH = (view.bounds.height - pad * 2 - gap * 3) / 4
        for (i, key) in keys.enumerated() {
            let row = i / 5, col = i % 5
            let x = left + weights[..<col].reduce(CGFloat(0)) { $0 + $1 * unit + gap }
            key.frame = CGRect(x: x, y: pad + CGFloat(row) * (rowH + gap), width: weights[col] * unit, height: rowH)
        }
    }

    // MARK: - Building the keys

    /// Font size for the current keyboard height.
    private func size(_ points: CGFloat) -> CGFloat { compact ? points * 0.78 : points }

    private func rebuild() {
        heldKey?.finish(commit: false)
        keys.forEach { $0.removeFromSuperview() }
        let groups: [[String]] = mode < 3
            ? (KeyboardData.layers.indices.contains(mode) ? KeyboardData.layers[mode].map(\.w) : [])
            : Self.abc
        let left = [mode == 3 ? backKey() : lettersKey(), layerKey(1), layerKey(2), globeKey()]
        let right = [deleteKey(), spaceKey(), punctuationKey(), returnKey()]
        keys = []
        for row in 0..<4 {
            keys.append(left[row])
            for col in 0..<3 {
                let i = row * 3 + col
                keys.append(mainKey(groups.indices.contains(i) ? groups[i] : [], kind: mode < 3 ? .word : .letter))
            }
            keys.append(right[row])
        }
        keys.forEach { view.insertSubview($0, belowSubview: bubble) }
        view.setNeedsLayout()
    }

    private func mainKey(_ words: [String], kind: Kind) -> FlickKey {
        let slots = (0..<5).map { $0 < words.count ? words[$0] : "" }
        let glyphFace = kind == .word && showGlyphs
        let face = { (w: String) in glyphFace ? (KeyboardData.glyphs[w] ?? w) : w }
        let key = FlickKey(color: Palette.key, pressedColor: Palette.pressed)
        if glyphFace {
            key.setLabels(main: face(slots[0]), edges: slots[1...].map(face),
                          font: Fonts.glyph(size(24)), edgeFont: Fonts.glyph(size(12)), glyph: true)
        } else {
            key.setLabels(main: face(slots[0]), edges: slots[1...].map(face),
                          font: Fonts.latin(size(kind == .word ? 15 : 18), bold: true),
                          edgeFont: Fonts.latin(size(kind == .word ? 9 : 11)))
        }
        if !slots.contains(where: { !$0.isEmpty }) { key.alpha = 0.35 }
        wire(key, preview: { i in
            guard !slots[i].isEmpty else { return nil }
            switch kind {
            case .word: return (KeyboardData.glyphs[slots[i]], slots[i])
            case .letter: return (nil, slots[i] == Self.shift ? "Aa" : slots[i])
            }
        }, pick: { [weak self] i in
            guard let self, !slots[i].isEmpty else { return }
            switch kind {
            case .word: self.insertWord(slots[i])
            case .letter:
                if slots[i] == Self.shift { self.toggleCase() } else { self.proxy.insertText(slots[i]) }
            }
        })
        return key
    }

    private func sideKey(_ face: String, glyph: Bool = false, active: Bool = false,
                         preview: @escaping (Int) -> (String?, String)?, pick: @escaping (Int) -> Void) -> FlickKey {
        let key = active
            ? FlickKey(color: Palette.accent, pressedColor: Palette.pressed, textColor: Palette.onAccent)
            : FlickKey(color: Palette.side, pressedColor: Palette.key)
        key.setLabels(main: face, font: glyph ? Fonts.glyph(size(28)) : Fonts.latin(size(15), bold: true), glyph: glyph)
        wire(key, preview: preview, pick: pick)
        return key
    }

    /// A key that only does something on tap.
    private func tapKey(_ face: String, glyph: Bool = false, active: Bool = false,
                        preview: (String?, String), action: @escaping () -> Void) -> FlickKey {
        sideKey(face, glyph: glyph, active: active, preview: { $0 == 0 ? preview : nil }, pick: { if $0 == 0 { action() } })
    }

    /// A layer key shows its layer's icon word; while that layer is open it becomes the way back to layer 1.
    private func layerKey(_ n: Int) -> FlickKey {
        let active = mode == n
        let word = Self.layerIcons[active ? 0 : n]
        return wordFaceKey(word, active: active) { [weak self] in self?.go(active ? 0 : n) }
    }

    private func backKey() -> FlickKey {
        wordFaceKey("toki", active: true) { [weak self] in self?.go(0) }
    }

    private func wordFaceKey(_ word: String, active: Bool, action: @escaping () -> Void) -> FlickKey {
        let glyph = showGlyphs ? KeyboardData.glyphs[word] : nil
        return tapKey(glyph ?? word, glyph: glyph != nil, active: active,
                      preview: (KeyboardData.glyphs[word], word), action: action)
    }

    private func lettersKey() -> FlickKey {
        tapKey("ABC", preview: (nil, "ABC")) { [weak self] in self?.go(3) }
    }

    /// Tap: next keyboard. Flick: switch the key faces between glyphs and Latin letters.
    private func globeKey() -> FlickKey {
        let key = sideKey("", preview: { [weak self] i in
            guard let self else { return nil }
            let label = i == 0 ? "Next keyboard" : (self.showGlyphs ? "Latin" : "Glyphs")
            return (nil, label)
        }, pick: { [weak self] i in
            guard let self else { return }
            if i == 0 {
                self.advanceToNextInputMode()
            } else {
                self.showGlyphs.toggle()
                self.rebuild()
            }
        })
        // the system's monochrome globe, not the colored 🌐 emoji
        key.setIcon("globe", pointSize: size(20))
        return key
    }

    private func deleteKey() -> FlickKey {
        let key = sideKey("⌫", preview: { i in (nil, i == 0 ? "1 letter" : "1 word") }, pick: { [weak self] i in
            if i == 0 { self?.proxy.deleteBackward() } else { self?.deleteWord() }
        })
        key.onRepeat = { [weak self] in self?.proxy.deleteBackward() }
        return key
    }

    /// Tap: a space. Flick in any direction: a colon (as in "e ni:").
    /// Only the left colon is drawn to keep the key quiet.
    private func spaceKey() -> FlickKey {
        let key = sideKey("␣", preview: { i in (nil, i == 0 ? "␣" : ":") }, pick: { [weak self] i in
            if i == 0 { self?.proxy.insertText(" ") } else { self?.insertPunctuation(":") }
        })
        key.setLabels(main: "␣", edges: [":"], font: Fonts.latin(size(20), bold: true), edgeFont: Fonts.latin(size(13)))
        return key
    }

    private func punctuationKey() -> FlickKey {
        let p = Self.punctuation
        let key = sideKey(p[0], preview: { i in
            guard !p[i].isEmpty else { return nil }
            return (nil, p[i])
        }, pick: { [weak self] i in
            if !p[i].isEmpty { self?.insertPunctuation(p[i]) }
        })
        key.setLabels(main: p[0], edges: Array(p[1...]), font: Fonts.latin(size(20), bold: true), edgeFont: Fonts.latin(size(13)))
        return key
    }

    private func returnKey() -> FlickKey {
        tapKey("⏎", preview: (nil, "⏎")) { [weak self] in
            self?.trimTrailingSpaces()
            self?.proxy.insertText("\n")
        }
    }

    private func wire(_ key: FlickKey, preview: @escaping (Int) -> (String?, String)?, pick: @escaping (Int) -> Void) {
        key.onTouchDown = { [weak self] key in
            // Touching a new key while another is still held types the held one first.
            if let held = self?.heldKey, held !== key { held.finish(commit: true) }
            self?.heldKey = key
        }
        key.onHover = { [weak self] key, slot in self?.showBubble(over: key, slot: slot, content: preview(slot)) }
        key.onRelease = { [weak self] key in
            guard let self, self.heldKey === key else { return }
            self.heldKey = nil
            self.bubble.isHidden = true
        }
        key.onPick = pick
    }

    /// Places the bubble one key away in the flick direction (above the key for a tap), kept inside the keyboard.
    private func showBubble(over key: FlickKey, slot: Int, content: (String?, String)?) {
        let f = key.frame
        let offsets: [CGPoint] = [CGPoint(x: 0, y: -1), CGPoint(x: -1, y: 0), CGPoint(x: 0, y: -1), CGPoint(x: 1, y: 0), CGPoint(x: 0, y: 1)]
        let bubbleSize = CGSize(width: max(f.width, 64), height: max(f.height, 62))
        var origin = CGPoint(x: f.midX + offsets[slot].x * (f.width + 4) - bubbleSize.width / 2,
                             y: f.midY + offsets[slot].y * (f.height + 4) - bubbleSize.height / 2)
        origin.x = min(max(origin.x, 0), view.bounds.width - bubbleSize.width)
        origin.y = min(max(origin.y, 0), view.bounds.height - bubbleSize.height)
        bubble.frame = CGRect(origin: origin, size: bubbleSize)
        bubble.show(glyph: content?.0, text: content?.1 ?? "—")
        view.bringSubviewToFront(bubble)
    }

    private func go(_ newMode: Int) {
        mode = newMode
        rebuild()
    }

    // MARK: - Editing

    /// Types a word followed by a space, adding a space before it if the text does not end in one.
    private func insertWord(_ word: String) {
        let needsSpace = before.last.map { !$0.isWhitespace } ?? false
        proxy.insertText((needsSpace ? " " : "") + word + " ")
    }

    /// Punctuation goes right after the last word: drop the auto-inserted space, then add one after the mark.
    private func insertPunctuation(_ mark: String) {
        trimTrailingSpaces()
        proxy.insertText(mark + " ")
    }

    private func trimTrailingSpaces() {
        var text = before
        while text.hasSuffix(" ") {
            proxy.deleteBackward()
            text.removeLast()
        }
    }

    /// Deletes the last word and the spaces after it.
    private func deleteWord() {
        if let selected = proxy.selectedText, !selected.isEmpty {
            proxy.deleteBackward()
            return
        }
        var text = Substring(before)
        var count = 0
        while let c = text.last, c.isWhitespace { text.removeLast(); count += 1 }
        while let c = text.last, !c.isWhitespace { text.removeLast(); count += 1 }
        for _ in 0..<max(count, 1) { proxy.deleteBackward() }
    }

    /// Switches the case of the last letter (the a/A key in ABC mode).
    private func toggleCase() {
        guard let last = before.last else { return }
        let s = String(last)
        proxy.deleteBackward()
        proxy.insertText(s == s.lowercased() ? s.uppercased() : s.lowercased())
    }
}
