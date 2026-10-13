import SwiftUI
import FirebaseDatabase
import MPSCANCore

@MainActor final class CatalogStore: ObservableObject {
    @Published var works: [Work] = []
    @Published var loading = false
    @Published var message = ""
    private var owner = ""
    private var ref: DatabaseReference?
    private var observer: DatabaseHandle?
    func bind(_ uid: String) {
        if let observer { ref?.removeObserver(withHandle: observer) }; observer = nil; ref = nil
        owner = uid; works = []; message = ""; loading = false
        guard !uid.isEmpty else { return }
        if ProcessInfo.processInfo.arguments.contains("--ui-testing") { works = [Work(id: "sample", raw: ["nome": "Nossa próxima história", "sinopse": "Uma história para ler no seu ritmo.", "tipo": "Novel"] )]; return }
        if let data = try? Data(contentsOf: cacheURL(uid)), let saved = try? JSONDecoder().decode([Work].self, from: data) { works = saved }
        loading = works.isEmpty
        let source = Database.database().reference().child("obras"); ref = source
        observer = source.observe(.value, with: { [weak self] snapshot in
            let raw = snapshot.value as? [String: Any] ?? [:]
            let items = raw.compactMap { id, v -> Work? in
                guard let x = v as? [String: Any], x["publicado"] as? Bool != false, x["published"] as? Bool != false else { return nil }
                let work = Work(id: id, raw: x); return work.title.isEmpty ? nil : work
            }.sorted { $0.updatedAt > $1.updatedAt }
            Task { @MainActor in
                guard let self, self.owner == uid else { return }
                self.works = items; self.loading = false; self.message = ""
                if let data = try? JSONEncoder().encode(items) { try? data.write(to: self.cacheURL(uid), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication]) }
            }
        }, withCancel: { [weak self] error in
            Task { @MainActor in guard self?.owner == uid else { return }; self?.loading = false; self?.message = friendly(error) }
        })
    }
    func reload() { bind(owner) }
    private func cacheURL(_ uid: String) -> URL {
        let root = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        return root.appendingPathComponent("catalog-\(OfflineStore.key(uid)).json")
    }
    func chapters(_ work: Work) async throws -> [Chapter] {
        if TestFixtures.enabled { return [TestFixtures.chapter] }
        let raw = try await Backend.shared.read("capitulos/\(work.id)") as? [String: Any] ?? [:]
        return raw.compactMap { id, v in (v as? [String: Any]).map { Chapter(id: id, raw: $0) } }.filter { $0.mode != "draft" }.sorted { ($0.number ?? -1) > ($1.number ?? -1) }
    }
    func content(work: Work, chapter: Chapter) async throws -> [ContentBlock] {
        if TestFixtures.enabled { return TestFixtures.blocks }
        let raw = try await Backend.shared.read("capitulos/\(work.id)/\(chapter.id)") as? [String: Any] ?? [:]
        guard Chapter(id: chapter.id, raw: raw).available() else { throw ModelError.unavailable }
        var content = try ContentParser.written(raw)
        var inline = raw
        for key in ["texto", "text", "conteudo", "content", "html", "novelText", "textoNovel"] { inline.removeValue(forKey: key) }
        let inlinePages = try ContentParser.pages(inline)
        content += inlinePages.filter { !content.contains($0) }
        let revision = text(raw, "pagesRevision")
        let path = revision.range(of: "^rev_[A-Za-z0-9]+$", options: .regularExpression) != nil ? "chapterRevisions/\(work.id)/\(chapter.id)/\(revision)" : "capitulosPaginas/\(work.id)/\(chapter.id)"
        let shallow = try await Backend.shared.read(path, shallow: true) as? [String: Any] ?? [:]
        if !shallow.isEmpty && shallow.allSatisfy({ key, value in key.range(of: "^(pagina_)?[0-9]+$", options: .regularExpression) != nil && value as? Bool == true }) {
            let keys = shallow.keys.sorted { (Int($0.replacingOccurrences(of: "pagina_", with: "")) ?? Int.max) < (Int($1.replacingOccurrences(of: "pagina_", with: "")) ?? Int.max) }
            guard content.count + keys.count <= 3000 else { throw ModelError.invalidContent }
            return content + keys.map { ContentBlock(.reference, path + "/" + $0) }
        }
        let pages = try ContentParser.pages(try await Backend.shared.read(path))
        content += pages.filter { !content.contains($0) }
        guard !content.isEmpty, content.count <= 3000 else { throw ModelError.invalidContent }
        return content
    }
}
