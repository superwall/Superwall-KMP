import SwiftUI
import SampleShared

/// SwiftUI host for the shared Compose Multiplatform UI.
///
/// `SampleShared` is the static Kotlin framework built by the Gradle pre-build
/// phase; the Kotlin top-level function `MainViewController()` (in
/// sample/shared/src/iosMain/.../MainViewController.kt) surfaces in ObjC/Swift
/// as `MainViewControllerKt.MainViewController()`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct SampleApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Compose manages its own safe-area insets (the shared App()
                // uses Modifier.safeContentPadding()).
                .ignoresSafeArea(.all)
        }
    }
}
