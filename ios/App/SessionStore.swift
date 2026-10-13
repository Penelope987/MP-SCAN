import SwiftUI
import FirebaseAuth
import FirebaseCore
import FirebaseDatabase
import GoogleSignIn
import MPSCANCore

@MainActor final class SessionStore: ObservableObject {
    enum Access { case checking, signedOut, ready, completeProfile, suspended, failed }
    @Published var access: Access = .checking
    @Published var uid = ""
    @Published var name = ""
    @Published var handle = ""
    @Published var photo = ""
    @Published var banReason = ""
    @Published var message = ""
    @Published var busy = false
    private var authListener: AuthStateDidChangeListenerHandle?
    private var banRef: DatabaseReference?
    private var banHandle: DatabaseHandle?
    private var workTask: Task<Void, Never>?
    private var mock: Bool { ProcessInfo.processInfo.arguments.contains("--ui-testing") }
    init() {
        if ProcessInfo.processInfo.arguments.contains("--ui-testing") { uid = "test-user"; name = "Leitora MP"; handle = "leitora_mp"; access = .ready; return }
        authListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in self?.changed(user) }
        }
    }
    private func changed(_ user: User?) {
        workTask?.cancel(); if let banHandle { banRef?.removeObserver(withHandle: banHandle) }
        banRef = nil; banHandle = nil
        uid = user?.uid ?? ""; name = ""; handle = ""; photo = ""; message = ""
        guard let user else { access = .signedOut; return }; access = .checking
        restoreProfile(user.uid)
        let ref = Database.database().reference().child("userBans/\(user.uid)")
        banRef = ref
        banHandle = ref.observe(.value) { [weak self] snapshot in
            Task { @MainActor in
                guard let self, self.uid == user.uid else { return }
                let ban = Ban(raw: snapshot.value as? [String: Any] ?? [:])
                if ban.blocks() { self.storeBan(ban, user.uid) }
                if ban.blocks() { self.banReason = ban.reason; self.access = .suspended }
            }
        }
        workTask = Task { await refresh() }
    }
    func refresh() async {
        if mock { return }
        guard let user = Auth.auth().currentUser, user.uid == uid else { return }
        let owner = uid
        do {
            let ban = Ban(raw: try await Backend.shared.read("userBans/\(owner)") as? [String: Any] ?? [:])
            guard !Task.isCancelled, uid == owner else { return }
            storeBan(ban, owner)
            if ban.blocks() { banReason = ban.reason; access = .suspended; return }
            let raw = try await Backend.shared.read("usuarios/\(owner)") as? [String: Any] ?? [:]
            guard !Task.isCancelled, uid == owner else { return }
            name = text(raw, "nome", "name"); handle = text(raw, "nomeUsuario", "username"); photo = text(raw, "foto", "photo")
            saveProfile(owner); access = validProfile ? .ready : .completeProfile
        } catch {
            guard !Task.isCancelled, uid == owner else { return }
            if Backend.transient(error), let ban = cachedBan(owner) {
                if ban.blocks() { banReason = ban.reason; access = .suspended }
                else { access = validProfile ? .ready : .failed; message = "Sem conexão. Você pode ler os capítulos já baixados." }
            } else { access = .failed; message = friendly(error) }
        }
    }
    var validProfile: Bool { !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && handle.range(of: "^[a-zA-Z0-9_]{3,24}$", options: .regularExpression) != nil }
    func requireAllowed() async throws {
        guard access == .ready, !uid.isEmpty else { throw ServiceError.signedOut }
        let owner = uid
        do {
            let ban = Ban(raw: try await Backend.shared.read("userBans/\(owner)") as? [String: Any] ?? [:]); storeBan(ban, owner)
            guard uid == owner else { throw ServiceError.signedOut }
            if ban.blocks() { banReason = ban.reason; access = .suspended; throw ServiceError.suspended }
        } catch {
            if Backend.transient(error), let ban = cachedBan(owner), !ban.blocks() { return }
            throw error
        }
    }
    func login(email: String, password: String) async { await action { _ = try await Auth.auth().signIn(withEmail: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password) } }
    func google() async {
        await action {
            guard let client = FirebaseApp.app()?.options.clientID,
                  let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
                  let window = scene.windows.first(where: { $0.isKeyWindow }), let root = window.rootViewController else { throw ServiceError.configuration }
            var controller = root
            while let next = controller.presentedViewController { controller = next }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: client)
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: controller)
            guard let token = result.user.idToken?.tokenString else { throw ServiceError.configuration }
            let credential = GoogleAuthProvider.credential(withIDToken: token, accessToken: result.user.accessToken.tokenString)
            _ = try await Auth.auth().signIn(with: credential)
        }
    }
    func register(email: String, password: String, name: String, handle: String) async {
        await action {
            try validate(name: name, handle: handle)
            let user = try await Auth.auth().createUser(withEmail: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password).user
            do { try await writeProfile(user: user, name: name, handle: handle) }
            catch { try? await user.delete(); throw error }
            await refresh()
        }
    }
    func complete(name: String, handle: String) async {
        await action { guard let user = Auth.auth().currentUser else { throw ServiceError.signedOut }; try validate(name: name, handle: handle); try await writeProfile(user: user, name: name, handle: handle); await refresh() }
    }
    func reset(email: String) async { await action { try await Auth.auth().sendPasswordReset(withEmail: email); message = "Enviamos as instruções de recuperação para o seu e-mail." } }
    private func validate(name: String, handle: String) throws {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, normalized(handle).range(of: "^[a-z0-9_]{3,24}$", options: .regularExpression) != nil else { throw ProfileError.invalid }
    }
    private func normalized(_ s: String) -> String { s.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "@", with: "").lowercased() }
    private func writeProfile(user: User, name: String, handle: String) async throws {
        let h = normalized(handle), owner = user.uid
        let raw: [String: Any] = ["nome": name.trimmingCharacters(in: .whitespacesAndNewlines), "nomeUsuario": h, "foto": user.photoURL?.absoluteString ?? "", "publico": true, "criadoEm": Date().timeIntervalSince1970 * 1000]
        var pub = raw; pub["uid"] = owner
        try await Backend.shared.update(["nomesUsuario/\(h)": owner, "usuarios/\(owner)": raw, "perfisPublicos/\(owner)": pub])
    }
    private func action(_ operation: () async throws -> Void) async {
        guard !busy else { return }; busy = true; message = ""; defer { busy = false }
        do { try await operation() } catch { message = friendly(error) }
    }
    func logout() { workTask?.cancel(); do { try Auth.auth().signOut(); GIDSignIn.sharedInstance.signOut() } catch { message = friendly(error) } }
    private func cachedBan(_ uid: String) -> Ban? { guard let data = UserDefaults.standard.data(forKey: "ban.\(uid)") else { return nil }; return try? JSONDecoder().decode(Ban.self, from: data) }
    private func storeBan(_ ban: Ban, _ uid: String) { if let data = try? JSONEncoder().encode(ban) { UserDefaults.standard.set(data, forKey: "ban.\(uid)") } }
    private func saveProfile(_ uid: String) { UserDefaults.standard.set(["name": name, "handle": handle, "photo": photo], forKey: "profile.\(uid)") }
    private func restoreProfile(_ uid: String) { let raw = UserDefaults.standard.dictionary(forKey: "profile.\(uid)") ?? [:]; name = raw["name"] as? String ?? ""; handle = raw["handle"] as? String ?? ""; photo = raw["photo"] as? String ?? "" }
}
enum ProfileError: Error, LocalizedError { case invalid; var errorDescription: String? { "Informe nome e @ com 3 a 24 letras, números ou sublinhado." } }
