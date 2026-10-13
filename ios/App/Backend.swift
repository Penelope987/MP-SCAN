import Foundation
import FirebaseAuth
import FirebaseCore
import FirebaseDatabase
import MPSCANCore

enum ServiceError: Error, LocalizedError {
    case denied, signedOut, suspended, retry, http(Int), configuration
    var errorDescription: String? {
        switch self {
        case .denied: return "Sua conta não tem permissão para acessar este conteúdo."
        case .signedOut: return "Entre na sua conta para continuar."
        case .suspended: return "Sua conta está suspensa. Consulte o motivo na tela de acesso."
        case .retry, .http: return "Não foi possível conectar. Confira sua internet e tente novamente."
        case .configuration: return "Não foi possível iniciar o aplicativo. Entre em contato com a equipe."
        }
    }
}
func friendly(_ error: Error) -> String {
    if let e = error as? ProfileError { return e.localizedDescription }
    if let e = error as? ModelError { return e.localizedDescription }
    if let e = error as? ServiceError { return e.localizedDescription }
    let e = error as NSError
    if e.domain == NSURLErrorDomain { return "Não foi possível conectar. Confira sua internet e tente novamente." }
    switch e.code {
    case 17004, 17008, 17009, 17011: return "Confira seu e-mail e sua senha."
    case 17007: return "Este e-mail já está cadastrado. Entre ou redefina a senha."
    case 17026: return "Use uma senha com pelo menos seis caracteres."
    case 17010: return "Muitas tentativas. Aguarde um pouco e tente novamente."
    case -5: return "Você cancelou a escolha da conta."
    default: return "Não foi possível concluir. Tente novamente ou avise a equipe."
    }
}
struct Backend {
    static let shared = Backend()
    func read(_ path: String, shallow: Bool = false) async throws -> Any {
        guard let user = Auth.auth().currentUser else { throw ServiceError.signedOut }
        guard let database = FirebaseApp.app()?.options.databaseURL, let base = URL(string: database) else { throw ServiceError.configuration }
        var url = base
        for part in path.split(separator: "/") { url.appendPathComponent(String(part)) }
        url.appendPathExtension("json")
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "auth", value: try await user.getIDToken())]
        if shallow { components.queryItems?.append(URLQueryItem(name: "shallow", value: "true")) }
        var request = URLRequest(url: components.url!); request.timeoutInterval = 60
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ServiceError.retry }
        if http.statusCode == 401 || http.statusCode == 403 { throw ServiceError.denied }
        if http.statusCode >= 500 || http.statusCode == 429 { throw ServiceError.retry }
        guard (200..<300).contains(http.statusCode) else { throw ServiceError.http(http.statusCode) }
        return try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
    }
    func set(_ path: String, value: Any) async throws { try await Database.database().reference().child(path).setValue(value) }
    func update(_ paths: [String: Any]) async throws { try await Database.database().reference().updateChildValues(paths) }
    static func transient(_ error: Error) -> Bool {
        if let e = error as? ServiceError, case .retry = e { return true }
        return (error as NSError).domain == NSURLErrorDomain
    }
}
