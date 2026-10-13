import SwiftUI
import FirebaseCore
import FirebaseDatabase
import GoogleSignIn

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        // Disk cache is metadata only: chapter media is saved by OfflineStore.
        return true
    }
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool { GIDSignIn.sharedInstance.handle(url) }
}
@main
struct MPSCANApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var session: SessionStore
    @StateObject private var catalog = CatalogStore()
    @StateObject private var offline = OfflineStore()
    @StateObject private var appearance = AppearanceStore()
    init() {
        _ = AppBootstrap.configure
        _session = StateObject(wrappedValue: SessionStore())
    }
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(session).environmentObject(catalog).environmentObject(offline).environmentObject(appearance)
                .tint(appearance.accent).preferredColorScheme(appearance.scheme)
                .onOpenURL { _ = GIDSignIn.sharedInstance.handle($0) }
        }
    }
}

private enum AppBootstrap {
    static let configure: Void = {
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        Database.database().isPersistenceEnabled = true
    }()
}
