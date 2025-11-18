import SwiftUI
import crossPlatformKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let _ = crossPlatformKit.HSKAppServices.shared
        return crossPlatformKit.AppViewControllerKt.AppViewController() // Call the Kotlin ViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct hskwidgetApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
        }
    }
}
