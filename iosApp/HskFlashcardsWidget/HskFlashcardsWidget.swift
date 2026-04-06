import WidgetKit
import SwiftUI
import crossPlatform

struct HskEntry: TimelineEntry {
    let date: Date
    let word: String
    let pinyin: String
    let definition: String
    let level: String
}

struct Provider: AppIntentTimelineProvider {
    private var resources: CachedResources {
        crossPlatform.CachedResources.shared
    }

    private func fetchEntry(for configuration: HskWidgetConfigurationIntent) async -> HskEntry {
        let services = crossPlatform.HSKAppServices.shared
        services.doInit(upToLevel: crossPlatform.HSKAppServicesPriority.Widget.shared)

        // Wait for initialization
        let isReady = try? await services.awaitReady(timeoutMs: 5000)
        
        if isReady == true {
            let selectedListIds = configuration.selectedLists?.map { KotlinLong(value: Int64($0.id)) } ?? []

            // Use the DAO directly to pick a random word from the selected lists
            let word = try? await services.database.annotatedChineseWordDAO().getRandomWordFromLists(
                listIds: selectedListIds, 
                bannedWords: KotlinArray(size: 0, init: { _ in "" as NSString })
            )

            if let word = word {
                let pinyinList = word.pinyins as! [crossPlatform.Pinyin]
                let pinyinStr = pinyinList.map { $0.syllable }.joined(separator: " ")
                let definitionMap = word.word?.definition ?? [:]
                let definition = (definitionMap[crossPlatform.Locale.english] as? String) ?? (word.annotation?.notes as? String) ?? ""
                let levelStr = word.hskLevel.name
                
                return HskEntry(
                    date: Date(),
                    word: word.simplified,
                    pinyin: pinyinStr,
                    definition: definition,
                    level: levelStr
                )
            }
        }

        return HskEntry(
            date: Date(),
            word: resources.placeholderWord,
            pinyin: resources.placeholderPinyin,
            definition: resources.placeholderDefinition,
            level: resources.placeholderLevel
        )
    }

    func placeholder(in context: Context) -> HskEntry {
        HskEntry(
            date: Date(),
            word: resources.placeholderWord,
            pinyin: resources.placeholderPinyin,
            definition: resources.placeholderDefinition,
            level: resources.placeholderLevel
        )
    }

    func snapshot(for configuration: HskWidgetConfigurationIntent, in context: Context) async -> HskEntry {
        await fetchEntry(for: configuration)
    }

    func timeline(for configuration: HskWidgetConfigurationIntent, in context: Context) async -> Timeline<HskEntry> {
        let entry = await fetchEntry(for: configuration)
        return Timeline(entries: [entry], policy: .atEnd)
    }
}

struct HskFlashcardsWidget: Widget {
    let kind: String = "HskFlashcardsWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: HskWidgetConfigurationIntent.self, provider: Provider()) { entry in
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
