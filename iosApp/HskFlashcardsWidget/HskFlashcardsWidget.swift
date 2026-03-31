import WidgetKit
import SwiftUI
import crossPlatform

struct HskEntry: TimelineEntry {
    let date: Date
    let word: String
    let pinyin: String
    let definition: String
    let level: String
    let widgetId: Int
}

struct Provider: TimelineProvider {
    private var resources: CachedResources {
        crossPlatform.CachedResources.shared
    }

    func placeholder(in context: Context) -> HskEntry {
        // placeholder MUST be synchronous and return immediately.
        // It uses the default hardcoded values in the singleton.
        HskEntry(
            date: Date(),
            word: resources.placeholderWord,
            pinyin: resources.placeholderPinyin,
            definition: resources.placeholderDefinition,
            level: resources.placeholderLevel,
            widgetId: 0
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (HskEntry) -> ()) {
        Task {
            // Load resources (translations, etc.) asynchronously
            _ = try? await resources.load()

            let entry = HskEntry(
                date: Date(),
                word: resources.placeholderWord,
                pinyin: resources.placeholderPinyin,
                definition: resources.placeholderDefinition,
                level: resources.placeholderLevel,
                widgetId: 0
            )
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HskEntry>) -> ()) {
        Task {
            // Ensure resources are loaded for the real widget display
            _ = try? await resources.load()

            let entry = HskEntry(
                date: Date(),
                word: resources.placeholderWord,
                pinyin: resources.placeholderPinyin,
                definition: resources.placeholderDefinition,
                level: resources.placeholderLevel,
                widgetId: 0
            )
            
            let timeline = Timeline(entries: [entry], policy: .atEnd)
            completion(timeline)
        }
    }
}

struct HskFlashcardsWidget: Widget {
    let kind: String = "HskFlashcardsWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            HskFlashcardsWidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(UIColor.systemBackground)
                }
        }
        .configurationDisplayName(crossPlatform.CachedResources.shared.appName)
        .description(crossPlatform.CachedResources.shared.appSlogan)
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
