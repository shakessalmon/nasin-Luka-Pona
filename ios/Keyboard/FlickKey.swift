import UIKit

/// A key with five slots: tap = 0, flick left = 1, up = 2, right = 3, down = 4.
final class FlickKey: UIView {
    /// A move shorter than this is a tap (same as the web prototype).
    static let tapRadius: CGFloat = 18

    static func direction(dx: CGFloat, dy: CGFloat) -> Int {
        if hypot(dx, dy) < tapRadius { return 0 }
        return abs(dx) > abs(dy) ? (dx < 0 ? 1 : 3) : (dy < 0 ? 2 : 4)
    }

    var onTouchDown: ((FlickKey) -> Void)?
    /// The finger moved onto another slot (also called with 0 on touch down).
    var onHover: ((FlickKey, Int) -> Void)?
    var onRelease: ((FlickKey) -> Void)?
    var onPick: ((Int) -> Void)?
    /// When set, holding the key without flicking repeats this (used by delete).
    var onRepeat: (() -> Void)?

    private let mainLabel = UILabel()
    private let iconView = UIImageView()
    private let edgeLabels = (0..<4).map { _ in UILabel() }
    private let baseColor: UIColor
    private let pressedColor: UIColor
    private var touch: UITouch?
    private var start = CGPoint.zero
    private(set) var slot = 0
    private var glyphNudge: CGFloat = 0
    private var repeatTimer: Timer?
    private var repeating = false

    init(color: UIColor, pressedColor: UIColor, textColor: UIColor = Palette.text, edgeColor: UIColor = Palette.muted) {
        baseColor = color
        self.pressedColor = pressedColor
        super.init(frame: .zero)
        backgroundColor = color
        layer.cornerRadius = 6
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOffset = CGSize(width: 0, height: 1)
        layer.shadowRadius = 0
        layer.shadowOpacity = 0.25
        for label in [mainLabel] + edgeLabels {
            label.textAlignment = .center
            label.textColor = textColor
            label.adjustsFontSizeToFitWidth = true
            label.minimumScaleFactor = 0.5
            label.isUserInteractionEnabled = false
            addSubview(label)
        }
        for label in edgeLabels { label.textColor = edgeColor }
        iconView.tintColor = textColor
        iconView.contentMode = .center
        iconView.isUserInteractionEnabled = false
        addSubview(iconView)
    }

    /// Shows a monochrome SF Symbol in place of the main label.
    func setIcon(_ systemName: String, pointSize: CGFloat) {
        mainLabel.text = nil
        iconView.image = UIImage(systemName: systemName, withConfiguration: UIImage.SymbolConfiguration(pointSize: pointSize))
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    /// edges: [left, up, right, down]; empty strings leave that side blank.
    /// glyph: the labels use the sitelen pona font, whose glyphs sit high in the line and are nudged down.
    func setLabels(main: String, edges: [String] = [], font: UIFont, edgeFont: UIFont? = nil, glyph: Bool = false) {
        glyphNudge = glyph ? 0.12 : 0
        mainLabel.text = main
        mainLabel.font = font
        for (i, label) in edgeLabels.enumerated() {
            label.text = i < edges.count ? edges[i] : ""
            label.font = edgeFont ?? font
        }
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let w = bounds.width, h = bounds.height
        let edgeH = edgeLabels[0].font.lineHeight
        let hasEdges = edgeLabels.contains { !($0.text ?? "").isEmpty }
        let sideW = hasEdges ? w * 0.24 : 2
        let mainDy = mainLabel.font.pointSize * glyphNudge, edgeDy = edgeLabels[0].font.pointSize * glyphNudge
        mainLabel.frame = CGRect(x: sideW, y: mainDy, width: w - sideW * 2, height: h)
        iconView.frame = bounds
        edgeLabels[0].frame = CGRect(x: 2, y: edgeDy, width: sideW - 2, height: h)
        edgeLabels[1].frame = CGRect(x: 4, y: 1 + edgeDy, width: w - 8, height: edgeH)
        edgeLabels[2].frame = CGRect(x: w - sideW, y: edgeDy, width: sideW - 2, height: h)
        edgeLabels[3].frame = CGRect(x: 4, y: h - edgeH - 1 + edgeDy, width: w - 8, height: edgeH)
        layer.shadowPath = UIBezierPath(roundedRect: bounds, cornerRadius: layer.cornerRadius).cgPath
    }

    /// Let touches in the gaps between keys reach the nearest key.
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        bounds.insetBy(dx: -2.5, dy: -2.5).contains(point)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard touch == nil, let t = touches.first else { return }
        onTouchDown?(self)
        touch = t
        start = t.location(in: self)
        slot = 0
        backgroundColor = pressedColor
        onHover?(self, 0)
        if onRepeat != nil { scheduleRepeat() }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = touch, touches.contains(t), !repeating else { return }
        let p = t.location(in: self)
        let d = Self.direction(dx: p.x - start.x, dy: p.y - start.y)
        guard d != slot else { return }
        slot = d
        if d != 0 { cancelRepeat() }
        onHover?(self, d)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = touch, touches.contains(t) else { return }
        finish(commit: true)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let t = touch, touches.contains(t) else { return }
        finish(commit: false)
    }

    /// Ends the current press now, e.g. because another key was touched while this one is held.
    func finish(commit: Bool) {
        guard touch != nil else { return }
        touch = nil
        let wasRepeating = repeating
        cancelRepeat()
        backgroundColor = baseColor
        onRelease?(self)
        if commit && !wasRepeating { onPick?(slot) }
    }

    private func scheduleRepeat() {
        repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.45, repeats: false) { [weak self] _ in
            guard let self else { return }
            self.repeating = true
            self.onRepeat?()
            self.repeatTimer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] _ in
                self?.onRepeat?()
            }
        }
    }

    private func cancelRepeat() {
        repeatTimer?.invalidate()
        repeatTimer = nil
        repeating = false
    }
}

/// The bubble that shows which word a press or flick will type.
final class PreviewBubble: UIView {
    private let glyphLabel = UILabel()
    private let textLabel = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = Palette.accent
        layer.cornerRadius = 8
        isUserInteractionEnabled = false
        isHidden = true
        for label in [glyphLabel, textLabel] {
            label.textAlignment = .center
            label.textColor = Palette.onAccent
            label.adjustsFontSizeToFitWidth = true
            label.minimumScaleFactor = 0.4
            addSubview(label)
        }
        glyphLabel.font = Fonts.glyph(28)
        textLabel.font = Fonts.latin(15, bold: true)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    func show(glyph: String?, text: String) {
        glyphLabel.text = glyph
        glyphLabel.isHidden = glyph == nil
        textLabel.text = text
        isHidden = false
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let inner = bounds.insetBy(dx: 4, dy: 2)
        if glyphLabel.isHidden {
            textLabel.frame = inner
        } else {
            let textH = textLabel.font.lineHeight
            glyphLabel.frame = CGRect(x: inner.minX, y: inner.minY + glyphLabel.font.pointSize * 0.12,
                                      width: inner.width, height: inner.height - textH)
            textLabel.frame = CGRect(x: inner.minX, y: inner.maxY - textH, width: inner.width, height: textH)
        }
    }
}
