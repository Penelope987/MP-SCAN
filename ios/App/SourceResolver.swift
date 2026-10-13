import Foundation
import MPSCANCore

enum SourceResolver {
    static func resolve(_ block: ContentBlock) async throws -> [ContentBlock] {
        guard block.kind == .reference else { return [block] }
        guard block.source.range(of: "^(capitulosPaginas/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+|chapterRevisions/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+/rev_[A-Za-z0-9]+)/(pagina_)?[0-9]+$", options: .regularExpression) != nil else { throw ModelError.unsafePath }
        let blocks = try ContentParser.pages(try await Backend.shared.read(block.source))
        guard !blocks.isEmpty else { throw ModelError.invalidContent }
        return blocks
    }
}
