import Foundation
import MPSCANCore

// Only local fictional data; production accounts and server content are never used.
enum TestFixtures {
    static var enabled: Bool { ProcessInfo.processInfo.arguments.contains("--ui-testing") }
    static let chapter = Chapter(id: "sample-chapter", raw: ["numero": 1, "temporada": 1, "publicadoEm": 1800000000000])
    static let blocks = [ContentBlock(.text, "Era uma vez uma história para levar com você, mesmo sem internet."), ContentBlock(.image, "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAIAAADYYG7QAAAATElEQVR4nO3OMQHAIADAMMC/m+lACxb27+nJjkRB5n7O+JN1O/AlVISKUBEqQkWoCBWhIlSEilARKkJFqAgVoSJUhIpQESpCRagIlRddKQLPS06QLQAAAABJRU5ErkJggg==")]
}
