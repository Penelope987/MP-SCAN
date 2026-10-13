import SwiftUI
import MPSCANCore

struct ReaderView: View {
    var work: Work; var chapter: Chapter
    @EnvironmentObject var session: SessionStore
    @EnvironmentObject var catalog: CatalogStore
    @EnvironmentObject var offline: OfflineStore
    @EnvironmentObject var appearance: AppearanceStore
    @Environment(\.dismiss) private var dismiss
    @State private var blocks: [ContentBlock] = []
    @State private var local: URL?
    @State private var loading = true
    @State private var message = ""
    @State private var controls = true
    @State private var settings = false
    @State private var loaded: Set<Int> = []
    @State private var footerVisible = false
    @State private var completion = 0
    var body: some View {
        ZStack(alignment: .top) {
            Color(uiColor: .systemBackground).ignoresSafeArea()
            if loading { ProgressView("Abrindo sua leitura…").frame(maxHeight: .infinity) }
            else if blocks.isEmpty { VStack(spacing: 20) { Notice(message: message); PrimaryButton(title: "Tentar novamente", icon: "arrow.clockwise") { Task { await load() } } }.padding(24).frame(maxHeight: .infinity) }
            else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        Color.clear.frame(height: 78)
                        ForEach(Array(blocks.enumerated()), id: \.offset) { index, block in
                            if block.kind == .text { Text(block.source).font(.system(size: appearance.textSize)).lineSpacing(9).textSelection(.enabled).frame(maxWidth: 720, alignment: .leading).padding(.horizontal, 24).padding(.vertical, 12).onAppear { didLoad(index) } }
                            else if block.kind == .reference { ResolvedReaderBlock(block: block, onLoaded: { didLoad(index) }) }
                            else { RemotePicture(source: local == nil ? block.source : "", local: local?.appendingPathComponent(block.source), onLoaded: { didLoad(index) }).frame(maxWidth: .infinity) }
                        }
                        VStack(spacing: 12) { Image(systemName: completion == 100 ? "checkmark.circle.fill" : "book.closed").font(.title).foregroundStyle(.tint); Text(completion == 100 ? "Capítulo concluído" : "Fim do capítulo").font(.title3.bold()); Text("Sua leitura, seu ritmo.").foregroundStyle(.secondary); Button("Voltar à obra") { dismiss() } }.padding(34).frame(maxWidth: .infinity)
                            .onAppear { footerVisible = true; finishIfReady() }.onDisappear { footerVisible = false }
                    }
                }
                .simultaneousGesture(DragGesture(minimumDistance: 18).onChanged { value in withAnimation(.easeInOut(duration: 0.22)) { controls = value.translation.height > 0 } })
                .simultaneousGesture(TapGesture(count: 2).onEnded { withAnimation { controls = true } })
            }
            if controls {
                HStack(spacing: 18) { Button { dismiss() } label: { Image(systemName: "chevron.left").frame(width: 40, height: 40) }.accessibilityLabel("Voltar"); VStack(alignment: .leading, spacing: 3) { Text(chapter.label).font(.headline); Text(work.title).font(.caption).lineLimit(1) }; Spacer(); Text("\(completion)%").font(.caption.monospacedDigit()); Button { settings = true } label: { Image(systemName: "textformat.size").frame(width: 40, height: 40) }.accessibilityLabel("Ajustes de leitura") }.padding(.horizontal, 12).padding(.vertical, 8).background(.ultraThinMaterial).transition(.move(edge: .top).combined(with: .opacity))
            }
        }.navigationBarBackButtonHidden().toolbar(.hidden, for: .navigationBar).toolbar(.hidden, for: .tabBar)
            .task { completion = UserDefaults.standard.integer(forKey: historyKey); await load() }
            .sheet(isPresented: $settings) { NavigationStack { Form { Section("Conforto de leitura") { Text("Tamanho do texto"); Slider(value: $appearance.textSize, in: 14...32, step: 1); Text("\(Int(appearance.textSize)) pontos").foregroundStyle(.secondary); Picker("Aparência", selection: $appearance.mode) { Text("Sistema").tag("system"); Text("Clara").tag("light"); Text("Escura").tag("dark") } }; Section { Text("As barras somem ao descer e voltam ao subir ou tocar duas vezes.").font(.subheadline).foregroundStyle(.secondary) } }.navigationTitle("Sua leitura, seu ritmo").toolbar { Button("Pronto") { settings = false } } }.presentationDetents([.medium, .large]) }
    }
    private var historyKey: String { "read.\(session.uid).\(work.id).\(chapter.id)" }
    private func didLoad(_ index: Int) { loaded.insert(index); if completion < 100 { completion = min(99, max(completion, Int(Double(index + 1) / Double(max(1, blocks.count)) * 99))); UserDefaults.standard.set(completion, forKey: historyKey) }; finishIfReady() }
    private func finishIfReady() { if footerVisible && !blocks.isEmpty && loaded.count == blocks.count { completion = 100; UserDefaults.standard.set(100, forKey: historyKey) } }
    private func load() async {
        loading = true; message = ""; loaded = []; defer { loading = false }
        do {
            guard !work.adult && !work.sensitive else { throw ServiceError.denied }
            try await session.requireAllowed()
            if let item = offline.item(work: work.id, chapter: chapter.id) { let manifest = try await offline.verified(item); blocks = manifest.blocks; local = item.directory }
            else { blocks = try await catalog.content(work: work, chapter: chapter); local = nil }
        } catch { blocks = []; message = friendly(error) }
    }
}

struct ResolvedReaderBlock: View {
    var block: ContentBlock
    var onLoaded: () -> Void
    @EnvironmentObject var appearance: AppearanceStore
    @State private var contents: [ContentBlock] = []
    @State private var loaded: Set<Int> = []
    @State private var message = ""
    @State private var attempt = 0
    var body: some View {
        VStack(spacing: 0) {
            if !message.isEmpty { VStack { Notice(message: message); Button("Tentar novamente") { attempt += 1 } }.padding(20) }
            else if contents.isEmpty { ProgressView().frame(height: 160) }
            else { ForEach(Array(contents.enumerated()), id: \.offset) { index, item in
                if item.kind == .text { Text(item.source).font(.system(size: appearance.textSize)).lineSpacing(9).textSelection(.enabled).frame(maxWidth: 720, alignment: .leading).padding(.horizontal, 24).padding(.vertical, 12).onAppear { didLoad(index) } }
                else { RemotePicture(source: item.source, onLoaded: { didLoad(index) }) }
            } }
        }.task(id: block.source + String(attempt)) {
            contents = []; loaded = []; message = ""
            do { let items = try await SourceResolver.resolve(block); try Task.checkCancellation(); contents = items }
            catch { if !Task.isCancelled { message = friendly(error) } }
        }
    }
    private func didLoad(_ index: Int) { loaded.insert(index); if !contents.isEmpty && loaded.count == contents.count { onLoaded() } }
}
