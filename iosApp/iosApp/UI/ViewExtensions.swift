import SwiftUI

/// SwiftUI View extensions for custom modifiers and utilities

// MARK: - TextEditor Background Customization

/// Extension to introspect and customize UITextView from SwiftUI's TextEditor.
///
/// This is a workaround for SwiftUI's TextEditor not exposing a native API to customize
/// the background color. We use UIViewRepresentable to introspect the view hierarchy
/// and access the underlying UITextView.
///
/// Usage:
/// ```swift
/// TextEditor(text: $text)
///     .introspectTextView { textView in
///         textView.backgroundColor = .clear
///     }
/// ```
extension View {
    func introspectTextView(customize: @escaping (UITextView) -> Void) -> some View {
        self.background(TextViewIntrospector(customize: customize))
    }
}

struct TextViewIntrospector: UIViewRepresentable {
    let customize: (UITextView) -> Void

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        view.backgroundColor = .clear

        DispatchQueue.main.async {
            if let textView = view.superview?.superview?.subviews.first(where: { $0 is UITextView }) as? UITextView {
                customize(textView)
            }
        }

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            if let textView = uiView.superview?.superview?.subviews.first(where: { $0 is UITextView }) as? UITextView {
                customize(textView)
            }
        }
    }
}
