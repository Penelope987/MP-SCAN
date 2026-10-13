import SwiftUI
import CryptoKit
import ImageIO
import MPSCANCore

struct DownloadItem: Identifiable, Sendable { let manifest: OfflineManifest; let directory: URL; var id: String { manifest.work.id + "/" + manifest.chapter.id } }
@MainActor final class OfflineStore: ObservableObject {
    @Published var items: [DownloadItem] = []
    @Published var progress: [String: Double] = [:]
    @Published var errors: [String: String] = [:]
    private var owner = ""
    private var tasks: [String: Task<Void, Never>] = [:]
    private let storage = OfflineDisk()
    nonisolated static func key(_ value: String) -> String { SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined() }
    func bind(_ uid: String) {
        for task in tasks.values { task.cancel() }; tasks = [:]; progress = [:]; errors = [:]; items = []; owner = uid
        guard !uid.isEmpty else { return }; Task { await reload() }
    }
    func reload() async { let uid = owner; let result = await storage.list(owner: uid); if uid == owner { items = result } }
    func cover(_ work: String) -> URL? {
        guard let directory = items.first(where: { $0.manifest.work.id == work && $0.manifest.owner == owner })?.directory else { return nil }
        let file = directory.appendingPathComponent("cover.img")
        return FileManager.default.fileExists(atPath: file.path) ? file : nil
    }
    func item(work: String, chapter: String) -> DownloadItem? { items.first { $0.manifest.work.id == work && $0.manifest.chapter.id == chapter && $0.manifest.owner == owner } }
    func download(work: Work, chapter: Chapter, catalog: CatalogStore, session: SessionStore) {
        let id = work.id + "/" + chapter.id, uid = owner
        guard tasks[id] == nil, !uid.isEmpty else { return }; progress[id] = 0; errors[id] = nil
        tasks[id] = Task {
            defer { if owner == uid { tasks[id] = nil; progress[id] = nil } }
            do {
                try await session.requireAllowed(); let blocks = try await catalog.content(work: work, chapter: chapter)
                try await storage.download(owner: uid, work: work, chapter: chapter, blocks: blocks) { value in
                    Task { @MainActor in if self.owner == uid { self.progress[id] = value } }
                }
                try Task.checkCancellation(); guard owner == uid else { return }; await reload()
            } catch { if owner == uid && !(error is CancellationError) { errors[id] = friendly(error) } }
        }
    }
    func cancel(_ id: String) { tasks[id]?.cancel() }
    func remove(_ item: DownloadItem) async { do { try await storage.remove(item.directory); await reload() } catch { errors[item.id] = friendly(error) } }
    func verified(_ item: DownloadItem) async throws -> OfflineManifest { try await storage.verify(item) }
}
actor OfflineDisk {
    private let fm = FileManager.default
    private var root: URL {
        let url = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("Offline", isDirectory: true)
        try? fm.createDirectory(at: url, withIntermediateDirectories: true)
        var mutable = url; var values = URLResourceValues(); values.isExcludedFromBackup = true; try? mutable.setResourceValues(values)
        return url
    }
    private func directory(owner: String, work: String, chapter: String) -> URL { root.appendingPathComponent(OfflineStore.key(owner), isDirectory: true).appendingPathComponent(OfflineStore.key(work + "/" + chapter), isDirectory: true) }
    func list(owner: String) -> [DownloadItem] {
        guard !owner.isEmpty else { return [] }
        let directory = root.appendingPathComponent(OfflineStore.key(owner))
        let dirs = (try? fm.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)) ?? []
        return dirs.compactMap { url in
            guard !url.lastPathComponent.hasPrefix("."), let data = try? Data(contentsOf: url.appendingPathComponent("manifest.json")), let manifest = try? JSONDecoder().decode(OfflineManifest.self, from: data), manifest.owner == owner, manifest.version == 1 else { return nil }
            return DownloadItem(manifest: manifest, directory: url)
        }.sorted { $0.manifest.savedAt > $1.manifest.savedAt }
    }
    func verify(_ item: DownloadItem) throws -> OfflineManifest {
        try item.manifest.validate(file: { try Data(contentsOf: item.directory.appendingPathComponent($0), options: .mappedIfSafe) }, digest: Self.digest)
        return item.manifest
    }
    func remove(_ directory: URL) throws { guard directory.path.hasPrefix(root.path + "/") else { throw ModelError.unsafePath }; try fm.removeItem(at: directory) }
    func download(owner: String, work: Work, chapter: Chapter, blocks: [ContentBlock], progress: @escaping @Sendable (Double) -> Void) async throws {
        let final = directory(owner: owner, work: work.id, chapter: chapter.id)
        let parent = final.deletingLastPathComponent(); try fm.createDirectory(at: parent, withIntermediateDirectories: true)
        let stage = parent.appendingPathComponent(".stage-" + UUID().uuidString, isDirectory: true)
        try fm.createDirectory(at: stage, withIntermediateDirectories: true); defer { try? fm.removeItem(at: stage) }
        var result: [ContentBlock] = [], sizes: [String: Int] = [:], hashes: [String: String] = [:]
        var fileIndex = 0
        for (index, original) in blocks.enumerated() {
            try Task.checkCancellation()
            let resolved = try await SourceResolver.resolve(original)
            for block in resolved {
            try Task.checkCancellation()
            if block.kind == .text { result.append(block) }
            else {
                let name = String(format: "page-%06d.img", fileIndex), target = stage.appendingPathComponent(name)
                if block.source.hasPrefix("data:") {
                    guard let comma = block.source.firstIndex(of: ","), block.source[..<comma].contains(";base64"), let data = Data(base64Encoded: String(block.source[block.source.index(after: comma)...])), data.count <= 40_000_000 else { throw ModelError.unsupportedImage }
                    try data.write(to: target, options: .completeFileProtectionUntilFirstUserAuthentication)
                } else {
                    guard let source = URL(string: block.source), source.scheme == "https" else { throw ModelError.unsupportedImage }
                    var request = URLRequest(url: source); request.timeoutInterval = 90
                    request.setValue("image/jpeg,image/png,image/webp,image/gif,image/*;q=0.8", forHTTPHeaderField: "Accept")
                    request.setValue("https://www.mpscan.online/", forHTTPHeaderField: "Referer")
                    let (temp, response) = try await URLSession.shared.download(for: request)
                    defer { try? fm.removeItem(at: temp) }
                    guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw ServiceError.retry }
                    try fm.moveItem(at: temp, to: target)
                }
                let attributes = try fm.attributesOfItem(atPath: target.path), count = (attributes[.size] as? NSNumber)?.intValue ?? 0
                guard count > 0, count <= 40_000_000, let source = CGImageSourceCreateWithURL(target as CFURL, nil), CGImageSourceGetCount(source) > 0,
                      CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceThumbnailMaxPixelSize: 128] as CFDictionary) != nil else { throw ModelError.unsupportedImage }
                try fm.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: target.path)
                sizes[name] = count; hashes[name] = try Self.fileDigest(target); result.append(ContentBlock(.image, name))
            }
            fileIndex += 1
            }
            progress(Double(index + 1) / Double(blocks.count))
        }
        if let source = URL(string: work.cover), source.scheme == "https" { try? await saveCover(source, stage: stage) }
        try Task.checkCancellation()
        let manifest = OfflineManifest(owner: owner, work: work, chapter: chapter, blocks: result, sizes: sizes, hashes: hashes)
        try manifest.validate(file: { try Data(contentsOf: stage.appendingPathComponent($0), options: .mappedIfSafe) }, digest: Self.digest)
        try JSONEncoder().encode(manifest).write(to: stage.appendingPathComponent("manifest.json"), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
        // Keep the existing complete copy unless the replacement has passed every check.
        if fm.fileExists(atPath: final.path) { _ = try fm.replaceItemAt(final, withItemAt: stage, backupItemName: nil, options: []) }
        else { try fm.moveItem(at: stage, to: final) }
    }
    private func saveCover(_ url: URL, stage: URL) async throws {
        var request = URLRequest(url: url); request.timeoutInterval = 20
        let (temp, response) = try await URLSession.shared.download(for: request)
        defer { try? fm.removeItem(at: temp) }
        let size = try temp.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode), size > 0, size <= 10_000_000,
              let image = CGImageSourceCreateWithURL(temp as CFURL, nil), CGImageSourceGetCount(image) > 0 else { return }
        let target = stage.appendingPathComponent("cover.img")
        try fm.moveItem(at: temp, to: target)
        try fm.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: target.path)
    }
    static func digest(_ data: Data) -> String { SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined() }
    private static func fileDigest(_ url: URL) throws -> String {
        let file = try FileHandle(forReadingFrom: url); defer { try? file.close() }; var hash = SHA256()
        while let data = try file.read(upToCount: 1_048_576), !data.isEmpty { hash.update(data: data) }
        return hash.finalize().map { String(format: "%02x", $0) }.joined()
    }
}
