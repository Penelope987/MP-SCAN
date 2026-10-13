import Foundation

public struct ProfileWrites {
    public var own: [String: Any]
    public var visible: [String: Any]
    public var identity: [String: Any]
    public var shared: [String: Any]
    public var isPublic: Bool
    public init(previous: [String: Any], uid: String, name: String, handle: String, providerPhoto: String, now: Double) {
        var raw = previous
        raw["nome"] = name.trimmingCharacters(in: .whitespacesAndNewlines)
        raw["nomeUsuario"] = handle
        if raw["foto"] == nil { raw["foto"] = previous["photo"] as? String ?? providerPhoto }
        let publicProfile = previous["publico"] as? Bool ?? true
        raw["publico"] = publicProfile
        if raw["criadoEm"] == nil { raw["criadoEm"] = now }
        own = raw; isPublic = publicProfile
        let identityKeys = ["nome", "nomeUsuario", "foto", "molduraComentarioId", "molduraPerfilId"]
        var publicKeys = identityKeys + ["publico", "criadoEm", "corPerfil"]
        if publicProfile { publicKeys += ["bio", "capaPerfil"] }
        shared = raw.filter { (identityKeys + ["publico", "criadoEm", "corPerfil", "bio", "capaPerfil"]).contains($0.key) }; shared["uid"] = uid
        visible = raw.filter { publicKeys.contains($0.key) }; visible["uid"] = uid
        identity = raw.filter { identityKeys.contains($0.key) }; identity["uid"] = uid
    }
}
