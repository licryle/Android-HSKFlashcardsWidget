import SwiftUI
import WidgetKit
import AppIntents
import crossPlatform

struct HskFlashcardsWidgetView: View {
    var entry: HskEntry

    @Environment(\.widgetFamily) var family

    var body: some View {
        VStack(spacing: 4) {
            // Top Row: Reload - Level - Speak
            HStack {
                Button(intent: NextWordIntent()) {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 12))
                        .frame(width: 24, height: 24)
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)

                Spacer()

                if !entry.level.isEmpty {
                    Text(entry.level)
                        .font(.system(size: 10, weight: .medium))
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.1))
                        .cornerRadius(4)
                }

                Spacer()

                Button(intent: SpeakWordIntent(word: entry.word)) {
                    Image(systemName: "speaker.wave.2")
                        .font(.system(size: 12))
                        .frame(width: 24, height: 24)
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 4)

            // Middle Content: Pinyin - Simplified - Definition
            Link(destination: URL(string: "hskwidget://search?q=\(entry.word.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")")!) {
                VStack(spacing: 2) {
                    Spacer(minLength: 0)
                    
                    Text(entry.pinyin)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                        .lineLimit(1)

                    Text(entry.word)
                        .font(.system(size: 30, weight: .bold))
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)

                    Text(entry.definition)
                        .font(.system(size: 13))
                        .lineLimit(2)
                        .multilineTextAlignment(.center)
                        .foregroundColor(.primary.opacity(0.8))
                    
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .padding(8)
    }
}

// Interactivity Support (iOS 17+)
struct NextWordIntent: AppIntent {
    static var title: LocalizedStringResource = "Next Word"
    
    init() {}

    func perform() async throws -> some IntentResult {
        // Trigger the iOS Widget reload
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct SpeakWordIntent: AppIntent {
    static var title: LocalizedStringResource = "Speak Word"
    
    @Parameter(title: "Word")
    var word: String

    init() { self.word = "" }
    init(word: String) { self.word = word }

    func perform() async throws -> some IntentResult {
        // Initialize App Group path for KMP (needed since AppIntent runs in background)
        let services = crossPlatform.HSKAppServices.shared
        services.doInit(upToLevel: crossPlatform.HSKAppServicesPriority.Widget.shared)
        
        // Wait for initialization
        let isReady = try? await services.awaitReady(timeoutMs: 5000)

        if isReady == true {
            crossPlatform.Utils.shared.playWordInBackground(word: word)
        }

        return .result()
    }
}
