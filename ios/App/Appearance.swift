import SwiftUI

@MainActor final class AppearanceStore: ObservableObject {
    @Published var mode: String { didSet { UserDefaults.standard.set(mode, forKey: "appearance.mode") } }
    @Published var color: String { didSet { UserDefaults.standard.set(color, forKey: "appearance.color") } }
    @Published var textSize: Double { didSet { UserDefaults.standard.set(textSize, forKey: "reader.textSize") } }
    init() { mode = UserDefaults.standard.string(forKey: "appearance.mode") ?? "system"; color = UserDefaults.standard.string(forKey: "appearance.color") ?? "violet"; let size = UserDefaults.standard.double(forKey: "reader.textSize"); textSize = size == 0 ? 20 : min(32, max(14, size)) }
    var scheme: ColorScheme? { mode == "dark" ? .dark : mode == "light" ? .light : nil }
    var accent: Color { switch color { case "pink": return Color(red: 0.76, green: 0.15, blue: 0.44); case "blue": return Color(red: 0.12, green: 0.38, blue: 0.76); case "green": return Color(red: 0.05, green: 0.45, blue: 0.35); default: return Color(red: 0.49, green: 0.22, blue: 0.80) } }
}
struct MPBackdrop: View {
    @EnvironmentObject var appearance: AppearanceStore
    var body: some View { Color(uiColor: .systemGroupedBackground).overlay(alignment: .topLeading) { RadialGradient(colors: [appearance.accent.opacity(0.12), .clear], center: .topLeading, startRadius: 0, endRadius: 420) }.ignoresSafeArea() }
}
struct MPCard<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) { self.content = content() }
    var body: some View { VStack(alignment: .leading, spacing: 12) { content }.padding(18).frame(maxWidth: .infinity, alignment: .leading).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24)).overlay { RoundedRectangle(cornerRadius: 24).strokeBorder(.primary.opacity(0.06)) } }
}
struct PrimaryButton: View {
    var title: String; var icon: String; var busy = false; var action: () -> Void
    var body: some View { Button(action: action) { HStack { if busy { ProgressView().tint(.white) } else { Image(systemName: icon) }; Text(title).font(.headline) }.frame(maxWidth: .infinity).padding(.vertical, 15) }.buttonStyle(.borderedProminent).buttonBorderShape(.roundedRectangle(radius: 17)).disabled(busy) }
}
struct Notice: View {
    var message: String
    var body: some View { if !message.isEmpty { Label(message, systemImage: "info.circle").font(.subheadline).foregroundStyle(.secondary).padding(14).frame(maxWidth: .infinity, alignment: .leading).background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16)).accessibilityIdentifier("notice") } }
}
