import SwiftUI
import FirebaseCore
import GoogleSignIn
import WidgetKit
import crossPlatform

class iOSWidgetPlatformDelegate: NSObject, WidgetPlatformDelegate {
    func reloadAll() {
        WidgetCenter.shared.reloadAllTimelines()
    }

    func getWidgetIds(callback: @escaping ([KotlinInt]) -> Void) {
        WidgetCenter.shared.getCurrentConfigurations { result in
            switch result {
            case .success(let widgetInfo):
                let ids = (0..<widgetInfo.count).map { KotlinInt(value: Int32($0)) }
                callback(ids)
            case .failure:
                callback([])
            }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let _ = crossPlatform.HSKAppServices.shared
        return crossPlatform.AppViewControllerKt.AppViewController() // Call the Kotlin ViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

class AppDelegate: NSObject, UIApplicationDelegate {
  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
    FirebaseApp.configure()

    // Attempt to get the Client ID from Firebase options first, then fallback to Info.plist
    if let clientID = FirebaseApp.app()?.options.clientID ?? Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String {
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    }

    // Register Widget Delegate
    WidgetProvider.shared.delegate = iOSWidgetPlatformDelegate()

    return true
  }
}

@main
struct hskwidgetApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    handleOpenURL(url)
                }
        }
    }
    
    private func handleOpenURL(_ url: URL) {
        var appIntent: crossPlatform.AppIntent? = nil
        
        if url.scheme == "hskwidget" {
            if url.host == "search" {
                if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
                   let word = components.queryItems?.first(where: { $0.name == "q" })?.value {
                    // Convert iOS URL intent to KMP AppIntent
                    appIntent = crossPlatform.AppIntent.Search(query: word)
                }
            } else if url.host == "configure" {
                appIntent = crossPlatform.AppIntent.WidgetConfiguration(widgetId: 0)
            }
        }

        if let intent = appIntent {
            crossPlatform.AppIntentBus.shared.emit(intent: intent)
        }
    }
}
