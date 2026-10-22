import CoreText
import UIKit

/// One key of a layer: a group name and its words as [tap, left, up, right, down].
struct KeyGroup: Decodable {
    let g: String
    let w: [String]
}

/// data/layout.json and data/words.json, bundled with the extension.
enum KeyboardData {
    static let layers: [[KeyGroup]] = load("layout") ?? []

    /// word → its sitelen pona glyph (a UCSUR character)
    static let glyphs: [String: String] = {
        struct Word: Decodable { let word: String; let ucsur: String }
        let words: [Word] = load("words") ?? []
        var map = [String: String]()
        for w in words {
            guard w.ucsur.hasPrefix("U+"),
                  let value = UInt32(w.ucsur.dropFirst(2), radix: 16),
                  let scalar = Unicode.Scalar(value) else { continue }
            map[w.word] = String(Character(scalar))
        }
        return map
    }()

    private static func load<T: Decodable>(_ name: String) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}

enum Fonts {
    /// PostScript name of the bundled nasin nanpa subset, registered for this process on first use.
    private static let glyphFontName: String? = {
        guard let url = Bundle.main.url(forResource: "nasin-nanpa-subset", withExtension: "otf") else { return nil }
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        guard let provider = CGDataProvider(url: url as CFURL),
              let font = CGFont(provider) else { return nil }
        return font.postScriptName as String?
    }()

    static func glyph(_ size: CGFloat) -> UIFont {
        glyphFontName.flatMap { UIFont(name: $0, size: size) } ?? .systemFont(ofSize: size)
    }

    static func latin(_ size: CGFloat, bold: Bool = false) -> UIFont {
        UIFont(name: bold ? "HelveticaNeue-Bold" : "HelveticaNeue", size: size)
            ?? .systemFont(ofSize: size, weight: bold ? .bold : .regular)
    }
}

enum Palette {
    private static func dynamic(_ light: UIColor, _ dark: UIColor) -> UIColor {
        UIColor { $0.userInterfaceStyle == .dark ? dark : light }
    }
    private static func rgb(_ hex: UInt32) -> UIColor {
        UIColor(red: CGFloat(hex >> 16 & 0xff) / 255, green: CGFloat(hex >> 8 & 0xff) / 255,
                blue: CGFloat(hex & 0xff) / 255, alpha: 1)
    }

    static let board = dynamic(rgb(0xd1d5db), rgb(0x1e2225))
    static let key = dynamic(.white, rgb(0x5a6066))
    static let side = dynamic(rgb(0xacb3bc), rgb(0x34393d))
    static let pressed = dynamic(rgb(0xe2f0eb), rgb(0x1d3530))
    static let accent = dynamic(rgb(0x1f6f5c), rgb(0x5fc4a6))
    static let onAccent = dynamic(.white, rgb(0x0f1a17))
    static let text = UIColor.label
    static let muted = UIColor.secondaryLabel
}
