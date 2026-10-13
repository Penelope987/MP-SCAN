import SwiftUI
import MPSCANCore

struct RootView: View {
    @EnvironmentObject var session: SessionStore
    @EnvironmentObject var catalog: CatalogStore
    @EnvironmentObject var offline: OfflineStore
    @Environment(\.scenePhase) private var phase
    var body: some View {
        ZStack {
            MPBackdrop()
            switch session.access {
            case .checking: VStack(spacing: 18) { ProgressView(); Text("Preparando sua leitura…").foregroundStyle(.secondary) }
            case .signedOut: LoginView()
            case .completeProfile: CompleteProfileView()
            case .suspended: AccessStateView(title: "Uma pausa na sua conta", icon: "lock.shield", message: session.banReason.isEmpty ? "Sua conta está suspensa. Fale com a equipe pelo canal oficial." : session.banReason)
            case .failed: AccessStateView(title: "Vamos tentar novamente?", icon: "wifi.exclamationmark", message: session.message)
            case .ready: MainTabs()
            }
        }
        .task(id: session.access == .ready ? session.uid : "") {
            let uid = session.access == .ready ? session.uid : ""
            catalog.bind(uid); offline.bind(uid)
        }
        .onChange(of: phase) { _, value in if value == .active { Task { await session.refresh(); await offline.reload() } } }
    }
}
struct AccessStateView: View {
    @EnvironmentObject var session: SessionStore
    var title: String; var icon: String; var message: String
    var body: some View { VStack(spacing: 24) { Image(systemName: icon).font(.system(size: 52)).foregroundStyle(.tint); Text(title).font(.title2.bold()); Text(message).multilineTextAlignment(.center).foregroundStyle(.secondary); PrimaryButton(title: "Verificar novamente", icon: "arrow.clockwise") { Task { await session.refresh() } }; Link("Falar com a equipe", destination: URL(string: "https://t.me/MPSCAN0")!); Button("Sair da conta") { session.logout() } }.padding(30).frame(maxWidth: 460) }
}
struct LoginView: View {
    @EnvironmentObject var session: SessionStore
    @State private var email = ""
    @State private var password = ""
    @State private var name = ""
    @State private var handle = ""
    @State private var register = false
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                VStack(alignment: .leading, spacing: 10) { Image(systemName: "books.vertical.fill").font(.system(size: 42)).foregroundStyle(.tint); Text("MP SCAN").font(.largeTitle.bold()); Text("Sua leitura. Seu ritmo.").font(.title3).foregroundStyle(.secondary) }.padding(.top, 40)
                MPCard {
                    VStack(alignment: .leading, spacing: 18) {
                        Text(register ? "Crie seu espaço de leitura" : "Bem-vinda de volta").font(.title2.bold())
                        if register { TextField("Nome de perfil", text: $name).textContentType(.name); TextField("@arroba", text: $handle).textInputAutocapitalization(.never).autocorrectionDisabled() }
                        TextField("Seu e-mail", text: $email).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled().textContentType(.username).accessibilityIdentifier("login-email")
                        SecureField("Sua senha", text: $password).textContentType(register ? .newPassword : .password).accessibilityIdentifier("login-password")
                        Notice(message: session.message)
                        PrimaryButton(title: register ? "Criar conta" : "Entrar", icon: "arrow.right", busy: session.busy) { Task { if register { await session.register(email: email, password: password, name: name, handle: handle) } else { await session.login(email: email, password: password) } } }
                        if !register { Button("Esqueci minha senha") { Task { await session.reset(email: email) } }.font(.subheadline) }
                        Divider()
                        Button { Task { await session.google() } } label: { Label("Entrar com Google", systemImage: "person.crop.circle.badge.checkmark").font(.headline).frame(maxWidth: .infinity).padding(12) }.buttonStyle(.bordered).disabled(session.busy)
                    }.textFieldStyle(.roundedBorder)
                }
                Button(register ? "Já tenho conta" : "Ainda não tenho conta") { register.toggle(); session.message = "" }.frame(maxWidth: .infinity)
                Text("Entre para acessar suas obras e guardar capítulos para ler offline.").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }.padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity)
        }.scrollDismissesKeyboard(.interactively)
    }
}
struct CompleteProfileView: View {
    @EnvironmentObject var session: SessionStore
    @State private var name = ""
    @State private var handle = ""
    var body: some View { ScrollView { VStack(alignment: .leading, spacing: 22) { Text("Seu espaço na MP SCAN").font(.largeTitle.bold()); Text("Escolha o nome e o @ que vão identificar você na comunidade.").foregroundStyle(.secondary); MPCard { VStack(spacing: 16) { TextField("Nome de perfil", text: $name); TextField("@arroba", text: $handle).textInputAutocapitalization(.never).autocorrectionDisabled(); Notice(message: session.message); PrimaryButton(title: "Salvar e continuar", icon: "checkmark", busy: session.busy) { Task { await session.complete(name: name, handle: handle) } } }.textFieldStyle(.roundedBorder) }; Button("Sair") { session.logout() } }.padding(24).frame(maxWidth: 520).frame(maxWidth: .infinity) }.onAppear { name = session.name; handle = session.handle } }
}
struct MainTabs: View {
    var body: some View {
        TabView {
            NavigationStack { HomeView() }.tabItem { Label("Início", systemImage: "house.fill") }
            NavigationStack { SearchView() }.tabItem { Label("Busca", systemImage: "magnifyingglass") }
            NavigationStack { LibraryView() }.tabItem { Label("Biblioteca", systemImage: "books.vertical.fill") }
            NavigationStack { MyProfileView() }.tabItem { Label("Perfil", systemImage: "person.crop.circle") }
            NavigationStack { SettingsView() }.tabItem { Label("Menu", systemImage: "line.3.horizontal") }
        }
    }
}
