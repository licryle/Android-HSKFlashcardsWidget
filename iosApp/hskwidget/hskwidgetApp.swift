import SwiftUI
import crossPlatform

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let _ = crossPlatform.HSKAppServices.shared
        return crossPlatform.AppViewControllerKt.AppViewController() // Call the Kotlin ViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct hskwidgetApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}
