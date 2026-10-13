import Foundation
import SwiftSoup

public enum ContentParser {
    private static let textFields = ["texto", "text", "conteudo", "content", "html", "novelText", "textoNovel"]
    public static func image(_ value: String) -> String? {
        let s = value.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "&amp;", with: "&").replacingOccurrences(of: " ", with: "%20")
        if s.lowercased().hasPrefix("data:image/") { return s }
        if s.hasPrefix("//") { return "https:" + s }
        if s.hasPrefix("http://") { return "https://" + s.dropFirst(7) }
        if s.hasPrefix("https://"), let url = URL(string: s), url.host != nil, url.user == nil, url.password == nil { return s }
        for (prefix, type) in [("/9j/", "jpeg"), ("iVBORw0KGgo", "png"), ("R0lGOD", "gif"), ("UklGR", "webp")] {
            if s.hasPrefix(prefix) { return "data:image/\(type);base64,\(s)" }
        }
        return nil
    }
    public static func written(_ raw: [String: Any]) throws -> [ContentBlock] {
        guard let s = textFields.compactMap({ raw[$0] as? String }).first(where: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else { return [] }
        guard s.count <= 2_000_000 else { throw ModelError.invalidContent }
        if s.range(of: "<[^>]+>", options: .regularExpression) != nil { return try html(s) }
        return chunks(s).map { ContentBlock(.text, $0) }
    }
    public static func html(_ html: String, base: String = "") throws -> [ContentBlock] {
        let doc = try SwiftSoup.parseBodyFragment(html, base)
        try doc.select("script,style,iframe,form,button,nav,.ads,.advertisement,.ad-container").remove()
        guard let body = doc.body() else { return [] }
        var result: [ContentBlock] = []; var buffer = ""
        func flush() { result += chunks(buffer).map { ContentBlock(.text, $0) }; buffer = "" }
        func walk(_ node: Node) throws {
            if let t = node as? TextNode { buffer += t.getWholeText(); return }
            guard let element = node as? Element else { return }
            let tag = element.tagName()
            if tag == "img" {
                flush()
                let keys = ["data-src", "data-lazy-src", "data-original", "src"]
                var source: String?
                for key in keys {
                    let raw = try element.attr(key)
                    if !raw.isEmpty { source = raw.hasPrefix("data:") || raw.hasPrefix("https://") || raw.hasPrefix("http://") || raw.hasPrefix("//") ? raw : try element.absUrl(key); break }
                }
                guard let value = source, let normalized = image(value) else { throw ModelError.unsupportedImage }
                result.append(ContentBlock(.image, normalized))
            } else {
                if tag == "br" { buffer += "\n" }
                for child in element.getChildNodes() { try walk(child) }
                if ["p", "div", "li", "blockquote", "h1", "h2", "h3", "h4", "section", "article"].contains(tag) { buffer += "\n"; flush() }
            }
        }
        try walk(body); flush(); return result
    }
    public static func pages(_ raw: Any) throws -> [ContentBlock] {
        if let s = raw as? String {
            if s.hasPrefix("mpscan-text:"), let data = String(s.dropFirst(12)).data(using: .utf8), let value = try JSONSerialization.jsonObject(with: data) as? [String: Any] { return try written(value) }
            guard let normalized = image(s) else { throw ModelError.invalidContent }
            return [ContentBlock(.image, normalized)]
        }
        if raw is NSNull { return [] }
        if let object = raw as? [String: Any] {
            if ["text", "texto", "novel"].contains(text(object, "tipo", "type")) { return try written(object) }
            for key in ["dataUrl", "url", "imagemUrl", "imagem", "src", "page", "base64", "imageUrl", "imageURL"] {
                if let value = object[key] as? String { return try pages(value) }
            }
            for key in ["paginas", "pages", "imagens", "images", "data"] {
                if let value = object[key], value is [Any] || value is [String: Any] || value is String { return try pages(value) }
            }
            let entries = object.filter { key, _ in key.range(of: "^(pagina_)?[0-9]+$", options: .regularExpression) != nil || key.hasPrefix("-") }
            return try entries.sorted {
                let a = order($0.key, $0.value), b = order($1.key, $1.value)
                return a == b ? $0.key < $1.key : a < b
            }.flatMap { try pages($0.value) }
        }
        if let array = raw as? [Any] { return try array.enumerated().sorted { order(String($0.offset), $0.element) < order(String($1.offset), $1.element) }.flatMap { try pages($0.element) } }
        throw ModelError.invalidContent
    }
    private static func order(_ key: String, _ value: Any) -> Double {
        if let obj = value as? [String: Any] {
            for k in ["ordem", "order", "index", "posicao", "position", "pagina", "numero"] { if let n = Double(text(obj, k)), n.isFinite { return n } }
        }
        return Double(key.replacingOccurrences(of: "pagina_", with: "")) ?? .greatestFiniteMagnitude
    }
    private static func chunks(_ s: String) -> [String] {
        let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return [] }; let chars = Array(trimmed)
        return stride(from: 0, to: chars.count, by: 4000).map { String(chars[$0..<min($0 + 4000, chars.count)]) }
    }
}
