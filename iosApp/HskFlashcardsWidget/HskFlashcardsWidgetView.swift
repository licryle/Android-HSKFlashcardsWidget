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
                Button(intent: NextWordIntent(widgetId: entry.widgetId)) {
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

                    if family != .systemSmall {
                        Text(entry.definition)
                            .font(.system(size: 13))
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.primary.opacity(0.8))
                    }
                    
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
    
    @Parameter(title: "Widget ID")
    var widgetId: Int

    init() { self.widgetId = 0 }
    init(widgetId: Int) { self.widgetId = widgetId }

    func perform() async throws -> some IntentResult {
        NSLog("NextWordIntent: Started")
        // Initialize App Group path for KMP (needed since AppIntent runs in background)
        let services = crossPlatform.HSKAppServices.shared
        services.doInit(upToLevel: crossPlatform.HSKAppServicesPriority.Widget.shared)

        // Wait for initialization to complete
        for _ in 1...50 {
            if services.status.value is crossPlatform.AppServices.StatusReady {
                break
            }
            try await Task.sleep(nanoseconds: 100_000_000) // 100ms
        }

        guard services.status.value is crossPlatform.AppServices.StatusReady else {
            NSLog("NextWordIntent: KMP Init Timeout")
            return .result()
        }

        // 1. Correct labels: 'listIds' and 'bannedWords'
        // 2. Correct types: Kotlin List<Long> maps to [KotlinLong], Array<String> maps to KotlinArray
        let listIds = [0, 1, 2, 3, 4, 5, 6, 7].map { KotlinLong(value: Int64($0)) }
        let bannedWords = KotlinArray<NSString>(size: 0) { _ in "" }

        let word = try await services.database.annotatedChineseWordDAO().getRandomWordFromLists(
            listIds: listIds,
            bannedWords: bannedWords
        )

        // Save the new word to the widget's store so it displays on refresh
        if let simplified = word?.simplified {
            let _ = try await services.widgetsPreferencesProvider.invoke(p1: Int32(widgetId))
            NSLog("New word found: \(simplified)")
        }

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
        for _ in 1...50 {
            if services.status.value is crossPlatform.AppServices.StatusReady {
                break
            }
            try await Task.sleep(nanoseconds: 100_000_000)
        }

        crossPlatform.Utils.shared.playWordInBackground(word: word)
        return .result()
    }
}
