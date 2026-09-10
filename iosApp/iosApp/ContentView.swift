import SwiftUI
import ComposeApp

/// Kotlin 側の MainViewController() を SwiftUI に埋め込むためのラッパー。
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // キーボード表示時のレイアウト調整は Compose 側の imePadding に任せる
            .ignoresSafeArea(.keyboard)
    }
}
