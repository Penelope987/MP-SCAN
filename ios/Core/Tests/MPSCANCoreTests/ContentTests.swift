import XCTest
@testable import MPSCANCore

final class ContentTests: XCTestCase {
    func testPlainNovelKeepsText() throws { XCTAssertEqual(try ContentParser.written(["novelText": "Primeiro parágrafo.\nSegundo parágrafo."]), [ContentBlock(.text, "Primeiro parágrafo.\nSegundo parágrafo.")]) }
    func testMixedHTMLKeepsOrderAndDropsScripts() throws {
        let blocks = try ContentParser.html("<p>Antes &amp; depois</p><script>segredo()</script><img src='https://example.com/p.png'><p>Fim</p>")
        XCTAssertEqual(blocks, [ContentBlock(.text, "Antes & depois"), ContentBlock(.image, "https://example.com/p.png"), ContentBlock(.text, "Fim")])
    }
    func testHTMLRelativeImageNeedsBase() throws { XCTAssertEqual(try ContentParser.html("<img src='/p.png'>", base: "https://example.com").first?.source, "https://example.com/p.png"); XCTAssertThrowsError(try ContentParser.html("<img src='/p.png'>")) }
    func testNumericPageOrder() throws { XCTAssertEqual(try ContentParser.pages(["pagina_10": "https://example.com/10.png", "pagina_2": "https://example.com/2.png", "pagina_1": "https://example.com/1.png"]).map(\.source), ["https://example.com/1.png", "https://example.com/2.png", "https://example.com/10.png"]) }
    func testObjectPageOrderAndNovelBlock() throws {
        let pages: [Any] = [["url": "https://example.com/2.png", "ordem": 2], ["tipo": "text", "text": "Uma história", "ordem": 1]]
        XCTAssertEqual(try ContentParser.pages(pages), [ContentBlock(.text, "Uma história"), ContentBlock(.image, "https://example.com/2.png")])
    }
    func testUnsupportedPageFailsInsteadOfSavingIncomplete() { XCTAssertThrowsError(try ContentParser.pages(["pagina_1": "javascript:alert(1)"])) }
    func testBase64AndHTTPSNormalization() { XCTAssertEqual(ContentParser.image("//example.com/a.png"), "https://example.com/a.png"); XCTAssertEqual(ContentParser.image("http://example.com/a.png?x=1&amp;y=2"), "https://example.com/a.png?x=1&y=2"); XCTAssertTrue(ContentParser.image("iVBORw0KGgoAAA")!.hasPrefix("data:image/png;base64,")); XCTAssertNil(ContentParser.image("file:///etc/passwd")) }
    func testNovelChunkingPreservesAllCharacters() throws { let raw = String(repeating: "á", count: 9001); let blocks = try ContentParser.written(["texto": raw]); XCTAssertEqual(blocks.count, 3); XCTAssertEqual(blocks.map(\.source).joined(), raw) }
    func testNewBadgeExpiresAfter48HoursAndEditDoesNotReset() {
        let now = 1_800_000_000_000.0, published = now - 172_800_000
        let old = Chapter(id: "1", raw: ["numero": 1, "publicadoEm": published, "atualizadoEm": now])
        XCTAssertFalse(old.isNew(now: now)); XCTAssertTrue(Chapter(id: "2", raw: ["publicadoEm": now - 1000]).isNew(now: now)); XCTAssertFalse(Chapter(id: "3", raw: ["publicadoEm": now + 1000]).isNew(now: now))
    }
    func testScheduledAndDraftNotReadable() { let now = 1_800_000_000_000.0; XCTAssertFalse(Chapter(id: "1", raw: ["modoPublicacao": "scheduled", "agendadoPara": now + 1000]).available(now: now)); XCTAssertFalse(Chapter(id: "2", raw: ["rascunho": true]).available(now: now)) }
    func testLabelDoesNotDuplicateChapterNumber() { let chapter = Chapter(id: "1", raw: ["numero": "1", "titulo": "Capítulo 1", "temporada": 2, "fimTemporada": true]); XCTAssertEqual(chapter.label, "Capítulo 1"); XCTAssertEqual(chapter.subtitle, ""); XCTAssertEqual(chapter.season, 2); XCTAssertTrue(chapter.seasonEnd) }
    func testNonFiniteTimestampsAreBounded() { XCTAssertEqual(milliseconds(.infinity), 0); XCTAssertEqual(milliseconds(.nan), 0); XCTAssertEqual(milliseconds(-1), 0) }
    func testUnresolvedReferencesCannotBeMarkedOffline() { let value = OfflineManifest(owner: "u", work: Work(id: "w", raw: [:]), chapter: Chapter(id: "c", raw: [:]), blocks: [ContentBlock(.reference, "capitulosPaginas/w/c/pagina_1")], sizes: [:], hashes: [:]); XCTAssertThrowsError(try value.validate(file: { _ in Data() }, digest: { _ in "" })) }
    func testNativeTextEncodedBlock() throws { XCTAssertEqual(try ContentParser.pages("mpscan-text:{\"text\":\"Olá, leitora\"}"), [ContentBlock(.text, "Olá, leitora")]) }
    func testSecondsConvertToMilliseconds() { XCTAssertEqual(milliseconds(1_800_000_000), 1_800_000_000_000) }
    func testBanExpirationAndPermanent() { let now = 1_800_000_000_000.0; XCTAssertTrue(Ban(raw: ["active": true, "permanent": true]).blocks(now: now)); XCTAssertFalse(Ban(raw: ["active": true, "until": now - 1]).blocks(now: now)); XCTAssertTrue(Ban(raw: ["active": true, "until": now + 1]).blocks(now: now)) }
    func testOfflineNovelRoundTripWithoutImages() throws {
        let value = OfflineManifest(owner: "u", work: Work(id: "w", raw: ["nome": "Novel"]), chapter: Chapter(id: "c", raw: ["numero": 1]), blocks: [ContentBlock(.text, "Olá")], sizes: [:], hashes: [:])
        let copy = try JSONDecoder().decode(OfflineManifest.self, from: JSONEncoder().encode(value)); XCTAssertEqual(copy.blocks, value.blocks); try copy.validate(file: { _ in XCTFail("Novel não precisa de imagem"); return Data() }, digest: { _ in "" })
    }
    func testOfflineMissingOrChangedMediaRejected() {
        let value = OfflineManifest(owner: "u", work: Work(id: "w", raw: ["nome": "Obra"]), chapter: Chapter(id: "c", raw: ["numero": 1]), blocks: [ContentBlock(.image, "page-0.img")], sizes: ["page-0.img": 2], hashes: ["page-0.img": "expected"])
        XCTAssertThrowsError(try value.validate(file: { _ in Data([1]) }, digest: { _ in "expected" }))
        XCTAssertThrowsError(try value.validate(file: { _ in Data([1, 2]) }, digest: { _ in "wrong" }))
    }
    func testOfflinePathTraversalRejected() { let value = OfflineManifest(owner: "u", work: Work(id: "w", raw: [:]), chapter: Chapter(id: "c", raw: [:]), blocks: [ContentBlock(.image, "../private.img")], sizes: [:], hashes: [:]); XCTAssertThrowsError(try value.validate(file: { _ in Data() }, digest: { _ in "" })) }
}
