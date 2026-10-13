import SwiftUI
import ImageIO
import MPSCANCore

struct RemotePicture: View {
    var source: String
    var local: URL? = nil
    var onLoaded: (() -> Void)? = nil
    @State private var image: UIImage?
    @State private var message = ""
    @State private var attempt = 0
    var body: some View {
        Group {
            if let image { Image(uiImage: image).resizable().scaledToFit() }
            else if !message.isEmpty { VStack(spacing: 10) { Image(systemName: "photo").font(.title2); Text(message).font(.caption).multilineTextAlignment(.center); Button("Tentar novamente") { attempt += 1 } }.padding(16).frame(maxWidth: .infinity).background(.quaternary) }
            else { Rectangle().fill(.quaternary).overlay { if source.isEmpty && local == nil { Image(systemName: "book.closed").font(.largeTitle).foregroundStyle(.secondary) } else { ProgressView() } } }
        }
        .task(id: source + (local?.path ?? "") + String(attempt)) {
            image = nil; message = ""; guard !source.isEmpty || local != nil else { return }
            do {
                let decoded = try await PictureDecoder.load(source: source, local: local)
                try Task.checkCancellation(); image = decoded; onLoaded?()
            } catch { if !Task.isCancelled { message = local != nil ? "Arquivo incompleto. Baixe novamente quando estiver online." : "Não foi possível carregar a imagem." } }
        }
    }
}
enum PictureDecoder {
    static func load(source: String, local: URL?) async throws -> UIImage {
        if let local { return try await decode(local) }
        if source.hasPrefix("data:") {
            guard let comma = source.firstIndex(of: ","), source[..<comma].contains(";base64"), let data = Data(base64Encoded: String(source[source.index(after: comma)...])), data.count <= 40_000_000 else { throw ModelError.unsupportedImage }
            return try await Task.detached(priority: .userInitiated) { try decodeData(data) }.value
        }
        guard let url = URL(string: source), url.scheme == "https" else { throw ModelError.unsupportedImage }
        var request = URLRequest(url: url); request.timeoutInterval = 60
        request.setValue("https://www.mpscan.online/", forHTTPHeaderField: "Referer")
        request.setValue("image/jpeg,image/png,image/webp,image/gif,image/*;q=0.8", forHTTPHeaderField: "Accept")
        let (file, response) = try await URLSession.shared.download(for: request)
        defer { try? FileManager.default.removeItem(at: file) }
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw ServiceError.retry }
        return try await decode(file)
    }
    private static func decode(_ file: URL) async throws -> UIImage {
        try await Task.detached(priority: .userInitiated) {
            let size = try file.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
            guard size <= 40_000_000, let source = CGImageSourceCreateWithURL(file as CFURL, nil) else { throw ModelError.unsupportedImage }
            return try thumbnail(source)
        }.value
    }
    private static func decodeData(_ data: Data) throws -> UIImage { guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { throw ModelError.unsupportedImage }; return try thumbnail(source) }
    private static func thumbnail(_ source: CGImageSource) throws -> UIImage {
        let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any] ?? [:]
        let width = (props[kCGImagePropertyPixelWidth] as? NSNumber)?.doubleValue ?? 1, height = (props[kCGImagePropertyPixelHeight] as? NSNumber)?.doubleValue ?? 1
        guard width > 0, height > 0, width.isFinite, height.isFinite else { throw ModelError.unsupportedImage }
        let maxSide = max(width, height), factor = min(1, min(1600 / width, sqrt(16_000_000 / (width * height))))
        let pixels = min(16384, max(1, maxSide * factor))
        guard let cg = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceCreateThumbnailWithTransform: true, kCGImageSourceThumbnailMaxPixelSize: Int(pixels), kCGImageSourceShouldCacheImmediately: true] as CFDictionary) else { throw ModelError.unsupportedImage }
        return UIImage(cgImage: cg)
    }
}
struct Avatar: View { var source: String; var size: CGFloat = 44; var body: some View { RemotePicture(source: source).frame(width: size, height: size).background(.quaternary).clipShape(Circle()).overlay { Circle().strokeBorder(.primary.opacity(0.08)) } } }
