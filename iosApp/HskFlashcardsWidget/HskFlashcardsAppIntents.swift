import Foundation
import AppIntents
import crossPlatform
import WidgetKit

struct WordListEntity: AppEntity, Equatable {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Word List"
    static var defaultQuery = WordListQuery()

    var id: Int
    var name: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)")
    }
    
    static func == (lhs: WordListEntity, rhs: WordListEntity) -> Bool {
        return lhs.id == rhs.id
    }
}

extension WordListEntity: Identifiable {}

struct WordListQuery: EntityQuery {
    func entities(for identifiers: [Int]) async throws -> [WordListEntity] {
        let all = try await fetchAllLists()
        return all.filter { identifiers.contains($0.id) }
    }

    func suggestedEntities() async throws -> [WordListEntity] {
        return try await fetchAllLists()
    }

    private func fetchAllLists() async throws -> [WordListEntity] {
        let services = crossPlatform.HSKAppServices.shared
        services.doInit(upToLevel: crossPlatform.HSKAppServicesPriority.Widget.shared)

        // Wait for initialization
        let isReady = try? await services.awaitReady(timeoutMs: 5000)

        if isReady == true {
            let lists = try await services.database.wordListDAO().getAllLists()
            return lists.map { wordListWithCount in
                WordListEntity(id: Int(wordListWithCount.id), name: wordListWithCount.name)
            }
        }
        
        return []
    }
}

struct HskWidgetConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Configure Widget"
    static var description = IntentDescription("Select word lists to display in the widget.")

    @Parameter(title: "Selected Lists")
    var selectedLists: [WordListEntity]?
}