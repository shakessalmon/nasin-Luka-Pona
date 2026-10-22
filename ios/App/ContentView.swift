import SwiftUI
import UIKit

/// The container app: how to turn the keyboard on, and a place to try it.
struct ContentView: View {
    @State private var text = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Turn on the keyboard") {
                    Text("1. Open Settings › General › Keyboard › Keyboards › Add New Keyboard… and pick nasin Luka Pona. Full Access is not needed.")
                    Text("2. In any text field, hold 🌐 and choose nasin Luka Pona.")
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }
                Section("Try it") {
                    TextEditor(text: $text)
                        .frame(minHeight: 120)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }
                Section("How to type") {
                    Text("Tap a key for the word in the middle, or flick left, up, right or down for the words around it. A space is added after each word.")
                    Text("Most words sit in the direction their glyph points: mi ↓, sina ↑, ona ←, pona ↑ / ike ↓, lili ← / suli →.")
                    Text("Left column: ABC letters · layer 2 · layer 3 · 🌐. Tap 🌐 for the next keyboard; flick it to switch the keys between glyphs and Latin letters.")
                    Text("Right column: ⌫ (tap for a letter, flick for a word, hold to repeat) · space · punctuation (tap . ← , ↑ ? ↓ ! → :) · return.")
                }
            }
            .navigationTitle("nasin Luka Pona")
        }
    }
}
