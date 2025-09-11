import SwiftUI
import crossPlatformKit

struct ContentView: View {
    @State private var text: String = Greeting().greet()

    @ObservedObject var viewModel: ContentView.ViewModel

    var body: some View {
        VStack(spacing: 15.0) {
            ValidatedTextField(titleKey: "Platform", secured: false, text:$text)
        }
        .padding(.all)
    }
}

struct ValidatedTextField: View {
    let titleKey: String
    let secured: Bool
    @Binding var text: String

    @ViewBuilder var textField: some View {
        if secured {
            SecureField(titleKey, text: $text)
        }  else {
            TextField(titleKey, text: $text)
        }
    }

    var body: some View {
        ZStack {
            textField
                .textFieldStyle(RoundedBorderTextFieldStyle())
        }
    }
}

struct FieldTextErrorHint: View {
    let error: String
    @State private var showingAlert = false

    var body: some View {
        Button(action: { self.showingAlert = true }) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
        }
        .alert(isPresented: $showingAlert) {
            Alert(title: Text("Error"), message: Text(error), dismissButton: .default(Text("Got it!")))
        }
    }
}

extension ContentView {

    struct LoginFormState {
        let usernameError: String?
        let passwordError: String?
        var isDataValid: Bool {
            get { return usernameError == nil && passwordError == nil }
        }
    }

    class ViewModel: ObservableObject {
        @Published var formState = LoginFormState(usernameError: nil, passwordError: nil)

        init() {
        }

        func login(username: String, password: String) {
        }

        func loginDataChanged(username: String, password: String) {
        }
    }
}
