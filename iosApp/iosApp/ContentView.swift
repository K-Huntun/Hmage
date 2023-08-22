import SwiftUI
import hmage
import commonui

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
struct ContentView: View {

	var body: some View {
           ComposeView()
                    .ignoresSafeArea(.all, edges: .bottom) // Compose has own keyboard handler
                    	}
}
