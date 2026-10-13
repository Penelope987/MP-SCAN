import Foundation

public enum ModelError: Error, LocalizedError {
    case invalidContent, unsupportedImage, incompleteDownload, unavailable, unsafePath
    public var errorDescription: String? {
        switch self {
        case .invalidContent: return "O capítulo contém um conteúdo que não conseguimos reconhecer."
        case .unsupportedImage: return "Uma imagem não está em um formato compatível."
        case .incompleteDownload: return "O download está incompleto. Conecte-se e baixe novamente."
        case .unavailable: return "Este capítulo ainda não está disponível."
        case .unsafePath: return "Não foi possível abrir este arquivo."
        }
    }
}
public func text(_ object: [String: Any], _ keys: String...) -> String {
    for key in keys {
        if let v = object[key] as? String, !v.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return v }
        if let v = object[key] as? NSNumber { return v.stringValue }
    }
    return ""
}
public func milliseconds(_ raw: Double) -> Double { guard raw.isFinite, raw > 0, raw < 100_000_000_000_000 else { return 0 }; return raw < 100_000_000_000 ? raw * 1000 : raw }
public struct Work: Codable, Identifiable, Hashable, Sendable {
    public var id: String
    public var title: String
    public var alternateTitle: String
    public var synopsis: String
    public var cover: String
    public var banner: String
    public var kind: String
    public var status: String
    public var author: String
    public var genres: [String]
    public var updatedAt: Double
    public var adult: Bool
    public var sensitive: Bool
    public init(id: String, raw: [String: Any]) {
        self.id = id; title = text(raw, "nome", "name", "titulo")
        alternateTitle = text(raw, "subtitulo", "nomeAlternativo", "tituloAlternativo", "alternateTitle")
        synopsis = text(raw, "sinopse", "synopsis"); cover = text(raw, "capa", "cover", "coverURL")
        banner = text(raw, "banner", "bannerURL"); kind = text(raw, "tipo", "type")
        status = text(raw, "status"); author = text(raw, "autor", "author")
        let g = raw["generos"] ?? raw["genres"]
        genres = (g as? [String]) ?? (g as? [String: String]).map { Array($0.values).sorted() } ?? (g as? String)?.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) } ?? []
        updatedAt = milliseconds(Double(text(raw, "atualizadoEm", "updatedAt")) ?? 0)
        adult = ["adulto", "adult", "mais18", "nsfw", "conteudoAdulto"].contains { raw[$0] as? Bool == true }
        sensitive = ["sensivel", "sensitive", "conteudoSensivel"].contains { raw[$0] as? Bool == true }
    }
}
public struct Chapter: Codable, Identifiable, Hashable, Sendable {
    public var id: String
    public var number: Double?
    public var title: String
    public var mode: String
    public var scheduledAt: Double
    public var publishedAt: Double
    public var createdAt: Double
    public var updatedAt: Double
    public var season: Int
    public var seasonEnd: Bool
    public init(id: String, raw: [String: Any]) {
        self.id = id; number = Double(text(raw, "numero", "number").replacingOccurrences(of: ",", with: "."))
        if let n = number, !n.isFinite || abs(n) > 1_000_000 { number = nil }
        title = text(raw, "titulo", "title")
        scheduledAt = milliseconds(Double(text(raw, "agendadoPara", "scheduledAt")) ?? 0)
        let explicit = text(raw, "modoPublicacao", "publicationMode")
        mode = explicit.isEmpty ? (raw["rascunho"] as? Bool == true || raw["publicado"] as? Bool == false || raw["published"] as? Bool == false ? "draft" : scheduledAt > 0 ? "scheduled" : "published") : explicit
        publishedAt = milliseconds(Double(text(raw, "publicadoEm", "publishedAt")) ?? 0)
        createdAt = milliseconds(Double(text(raw, "criadoEm", "createdAt")) ?? 0)
        updatedAt = milliseconds(Double(text(raw, "atualizadoEm", "updatedAt")) ?? 0)
        season = min(999, max(1, Int(text(raw, "temporada", "season")) ?? 1))
        seasonEnd = raw["fimTemporada"] as? Bool ?? raw["seasonEnd"] as? Bool ?? false
    }
    public var label: String { "Capítulo " + number.map { $0 == $0.rounded() ? String(Int($0)) : String($0) }.safeDefault }
    public var subtitle: String {
        let v = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let stripped = v.replacingOccurrences(of: "^(?:(?:cap[ií]tulo)\\s*)+", with: "", options: [.regularExpression, .caseInsensitive])
        return v.caseInsensitiveCompare(label) == .orderedSame || (number != nil && Double(stripped) == number) ? "" : v
    }
    public func available(now: Double = Date().timeIntervalSince1970 * 1000) -> Bool { mode != "draft" && (mode != "scheduled" || (scheduledAt > 0 && scheduledAt <= now)) }
    public func isNew(now: Double = Date().timeIntervalSince1970 * 1000) -> Bool {
        let published = [publishedAt, scheduledAt, createdAt, updatedAt].first { $0 > 0 } ?? 0
        return available(now: now) && published > 0 && now >= published && now - published < 172_800_000
    }
}
private extension Optional where Wrapped == String { var safeDefault: String { self ?? "—" } }
public struct ContentBlock: Codable, Equatable, Sendable {
    public enum Kind: String, Codable, Sendable { case text, image, reference }
    public var kind: Kind
    public var source: String
    public init(_ kind: Kind, _ source: String) { self.kind = kind; self.source = source }
}
public struct Ban: Codable, Sendable {
    public var active: Bool
    public var permanent: Bool
    public var until: Double
    public var reason: String
    public init(raw: [String: Any]) { active = raw["active"] as? Bool ?? false; permanent = raw["permanent"] as? Bool ?? false; until = milliseconds(Double(text(raw, "until")) ?? 0); reason = text(raw, "reason") }
    public func blocks(now: Double = Date().timeIntervalSince1970 * 1000) -> Bool { active && (permanent || until > now) }
}
public struct OfflineManifest: Codable, Sendable {
    public var version = 1
    public var owner: String
    public var work: Work
    public var chapter: Chapter
    public var blocks: [ContentBlock]
    public var sizes: [String: Int]
    public var hashes: [String: String]
    public var savedAt: Date
    public init(owner: String, work: Work, chapter: Chapter, blocks: [ContentBlock], sizes: [String: Int], hashes: [String: String]) {
        self.owner = owner; self.work = work; self.chapter = chapter; self.blocks = blocks; self.sizes = sizes; self.hashes = hashes; savedAt = Date()
    }
    public func validate(file: (String) throws -> Data, digest: (Data) -> String) throws {
        guard version == 1, !blocks.isEmpty, !blocks.contains(where: { $0.kind == .reference }) else { throw ModelError.incompleteDownload }
        for block in blocks where block.kind == .image {
            guard !block.source.contains("/"), !block.source.contains(".."), let size = sizes[block.source], let hash = hashes[block.source] else { throw ModelError.unsafePath }
            let data = try file(block.source)
            guard data.count == size, !data.isEmpty, digest(data) == hash else { throw ModelError.incompleteDownload }
        }
    }
}
