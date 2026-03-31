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
        }
    }
}
