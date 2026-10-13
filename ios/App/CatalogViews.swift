import SwiftUI
import MPSCANCore

struct HomeView: View {
    @EnvironmentObject var session: SessionStore
    @EnvironmentObject var catalog: CatalogStore
    @EnvironmentObject var offline: OfflineStore
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                HStack { VStack(alignment: .leading, spacing: 6) { Text("Olá, \(session.name)").font(.subheadline).foregroundStyle(.secondary); Text("Sua próxima história").font(.largeTitle.bold()) }; Spacer(); Avatar(source: session.photo, size: 48) }
                MPCard { HStack { VStack(alignment: .leading, spacing: 8) { Text("Sua leitura, seu ritmo").font(.title3.bold()); Text("Baixe seus capítulos favoritos e leve as histórias com você.").font(.subheadline).foregroundStyle(.secondary) }; Image(systemName: "book.pages.fill").font(.system(size: 36)).foregroundStyle(.tint) } }
                if !offline.items.isEmpty { Text("Disponíveis offline").font(.title2.bold()); WorkGrid(works: Array(Dictionary(offline.items.map { ($0.manifest.work.id, $0.manifest.work) }, uniquingKeysWith: { a, _ in a }).values).sorted { $0.title < $1.title }) }
                Text("Explore o catálogo").font(.title2.bold())
                if catalog.loading { ProgressView("Carregando obras…").frame(maxWidth: .infinity) }
                Notice(message: catalog.message)
                if catalog.works.isEmpty && !catalog.loading { ContentUnavailableView("Histórias a caminho", systemImage: "books.vertical", description: Text("As obras publicadas pela equipe aparecem aqui.")) }
                WorkGrid(works: catalog.works)
            }.padding(20).frame(maxWidth: 1100).frame(maxWidth: .infinity)
        }.background { MPBackdrop() }.navigationTitle("MP SCAN").navigationBarTitleDisplayMode(.inline).refreshable { await session.refresh(); catalog.reload() }
    }
}
struct WorkGrid: View {
    var works: [Work]
    var body: some View { LazyVGrid(columns: [GridItem(.adaptive(minimum: 140, maximum: 210), spacing: 16)], spacing: 22) { ForEach(works) { work in NavigationLink { WorkView(work: work) } label: { WorkTile(work: work) }.buttonStyle(.plain).accessibilityIdentifier("work-\(work.id)") } } }
}
struct WorkTile: View {
    @EnvironmentObject var offline: OfflineStore
    var work: Work
    var body: some View { VStack(alignment: .leading, spacing: 10) { RemotePicture(source: work.cover, local: offline.cover(work.id)).aspectRatio(0.68, contentMode: .fit).clipShape(RoundedRectangle(cornerRadius: 18)).overlay(alignment: .bottomLeading) { if !work.kind.isEmpty { Text(work.kind.uppercased()).font(.caption2.bold()).padding(7).background(.ultraThinMaterial, in: Capsule()).padding(8) } }; Text(work.title).font(.subheadline.bold()).lineLimit(2); Text(work.status.isEmpty ? work.author : work.status).font(.caption).foregroundStyle(.secondary).lineLimit(1) }.accessibilityElement(children: .combine) }
}
struct SearchView: View {
    @EnvironmentObject var catalog: CatalogStore
    @State private var query = ""
    @State private var people: [PublicPerson] = []
    @State private var message = ""
    @State private var busy = false
    var filtered: [Work] { let q = query.trimmingCharacters(in: .whitespacesAndNewlines); return q.isEmpty ? catalog.works : catalog.works.filter { [$0.title, $0.alternateTitle, $0.author, $0.synopsis].joined(separator: " ").localizedCaseInsensitiveContains(q) } }
    var body: some View {
        ScrollView { VStack(alignment: .leading, spacing: 22) { Text("Encontre sua próxima leitura").font(.title.bold()); Text("Pesquise títulos, autores ou pessoas pelo @.").foregroundStyle(.secondary); Text("Obras").font(.title2.bold()); WorkGrid(works: filtered); if !query.isEmpty { Text("Usuários").font(.title2.bold()); if busy { ProgressView() }; Notice(message: message); ForEach(people) { person in NavigationLink { PublicProfileView(person: person) } label: { PersonRow(person: person) }.buttonStyle(.plain) }; if !busy && people.isEmpty && message.isEmpty { Text("Nenhum perfil encontrado.").foregroundStyle(.secondary) } } }.padding(20).frame(maxWidth: 1100).frame(maxWidth: .infinity) }.background { MPBackdrop() }.navigationTitle("Busca").searchable(text: $query, prompt: "Obra, autor, nome ou @")
            .task(id: query) {
                people = []; message = ""; guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }; busy = true
                do { try await Task.sleep(for: .milliseconds(350)); let raw = try await Backend.shared.read("perfisPublicos") as? [String: Any] ?? [:]; try Task.checkCancellation(); let q = query.replacingOccurrences(of: "@", with: ""); people = raw.compactMap { id, value in (value as? [String: Any]).map { PublicPerson(id: id, raw: $0) } }.filter { $0.name.localizedCaseInsensitiveContains(q) || $0.handle.localizedCaseInsensitiveContains(q) }.sorted { $0.handle < $1.handle }.prefix(30).map { $0 }; busy = false }
                catch { if !Task.isCancelled { busy = false; message = friendly(error) } }
            }
    }
}
struct WorkView: View {
    var work: Work
    @EnvironmentObject var catalog: CatalogStore
    @EnvironmentObject var session: SessionStore
    @EnvironmentObject var offline: OfflineStore
    @State private var chapters: [Chapter] = []
    @State private var busy = true
    @State private var message = ""
    @State private var collapsed: Set<Int> = []
    var seasons: [Int] { Set(chapters.map(\.season)).sorted(by: >) }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack(alignment: .top, spacing: 20) { RemotePicture(source: work.cover, local: offline.cover(work.id)).frame(width: 120, height: 178).clipShape(RoundedRectangle(cornerRadius: 18)); VStack(alignment: .leading, spacing: 10) { Text(work.title).font(.title2.bold()); if !work.alternateTitle.isEmpty { Text(work.alternateTitle).font(.subheadline).foregroundStyle(.secondary) }; Text([work.kind, work.status].filter { !$0.isEmpty }.joined(separator: " · ")).font(.caption.bold()).foregroundStyle(.tint); Text(work.author).font(.subheadline) } }
                MPCard { VStack(alignment: .leading, spacing: 12) { Text("Sobre a obra").font(.headline); Text(work.synopsis.isEmpty ? "A sinopse será adicionada pela equipe." : work.synopsis).foregroundStyle(.secondary); Text(work.genres.joined(separator: " · ")).font(.caption).foregroundStyle(.tint) } }
                HStack { Text("Capítulos").font(.title2.bold()); Spacer(); Label("\(chapters.count)", systemImage: "book.closed").font(.caption).foregroundStyle(.secondary) }
                if busy { ProgressView("Carregando capítulos…") }; Notice(message: message)
                if work.adult || work.sensitive { Notice(message: "Esta obra possui conteúdo restrito. A verificação de idade ainda será integrada nesta versão para iPhone.") }
                ForEach(seasons, id: \.self) { season in
                    VStack(spacing: 12) {
                        Button { withAnimation(.easeInOut(duration: 0.2)) { if collapsed.contains(season) { collapsed.remove(season) } else { collapsed.insert(season) } } } label: { HStack { Text("Temporada \(season)").font(.headline); Spacer(); Image(systemName: collapsed.contains(season) ? "plus.circle" : "minus.circle") }.padding(16).background(.thinMaterial, in: RoundedRectangle(cornerRadius: 17)) }
                        if !collapsed.contains(season) { ForEach(chapters.filter { $0.season == season }) { chapter in ChapterRow(work: work, chapter: chapter).disabled(work.adult || work.sensitive) } }
                    }
                }
            }.padding(20).frame(maxWidth: 780).frame(maxWidth: .infinity)
        }.background { MPBackdrop() }.navigationTitle("Detalhes da obra").navigationBarTitleDisplayMode(.inline)
            .task { await load() }.refreshable { await load() }
    }
    private func load() async {
        busy = true; defer { busy = false }
        do { try await session.requireAllowed(); chapters = try await catalog.chapters(work); message = "" }
        catch {
            chapters = offline.items.filter { $0.manifest.work.id == work.id }.map { $0.manifest.chapter }.sorted { ($0.number ?? -1) > ($1.number ?? -1) }
            message = chapters.isEmpty ? friendly(error) : "Mostrando seus capítulos baixados."
        }
        collapsed = Set(seasons.filter { season in chapters.contains { $0.season == season && $0.seasonEnd } })
    }
}
struct ChapterRow: View {
    var work: Work; var chapter: Chapter
    @EnvironmentObject var offline: OfflineStore
    @EnvironmentObject var catalog: CatalogStore
    @EnvironmentObject var session: SessionStore
    var id: String { work.id + "/" + chapter.id }
    var saved: Bool { offline.item(work: work.id, chapter: chapter.id) != nil }
    var body: some View {
        MPCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 12) {
                    NavigationLink { ReaderView(work: work, chapter: chapter) } label: { VStack(alignment: .leading, spacing: 5) { HStack { Text(chapter.label).font(.headline); if chapter.isNew() { Text("NOVO").font(.system(size: 9, weight: .heavy)).foregroundStyle(.white).padding(.horizontal, 8).padding(.vertical, 4).background(.tint, in: Capsule()) } }; if !chapter.subtitle.isEmpty { Text(chapter.subtitle).font(.subheadline).foregroundStyle(.secondary) }; if chapter.seasonEnd { Text("Fim da temporada").font(.caption).foregroundStyle(.tint) }; if !chapter.available() { Text("Liberação agendada").font(.caption).foregroundStyle(.secondary) } }.frame(maxWidth: .infinity, alignment: .leading) }.buttonStyle(.plain).disabled(!chapter.available())
                    if let progress = offline.progress[id] { Button { offline.cancel(id) } label: { VStack { ProgressView(value: progress).frame(width: 45); Text("\(Int(progress * 100))%").font(.caption2) } }.accessibilityLabel("Cancelar download") }
                    else { Button { offline.download(work: work, chapter: chapter, catalog: catalog, session: session) } label: { Image(systemName: saved ? "checkmark.circle.fill" : "arrow.down.to.line").font(.title3).padding(12).background(Color.primary.opacity(0.06), in: RoundedRectangle(cornerRadius: 14)) }.disabled(saved || !chapter.available()).accessibilityLabel(saved ? "Disponível offline" : "Baixar capítulo") }
                }
                if saved { Label("Disponível offline", systemImage: "checkmark.circle").font(.caption).foregroundStyle(.secondary) }
                if let error = offline.errors[id] { Notice(message: error) }
            }
        }
    }
}
