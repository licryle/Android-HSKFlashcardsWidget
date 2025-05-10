package fr.berliat.hskwidget.domain

class SearchQueryProcessor {
    fun processSearchQuery(query: String): Pair<String?, String?> {
        val listPrefix = "list:"
        if (query.startsWith(listPrefix, ignoreCase = true)) {
            var listName = query.substring(listPrefix.length).trim()

            // Remove quotes if present
            if (listName.startsWith('"') && listName.endsWith('"') && listName.length > 1) {
                listName = listName.substring(1, listName.length - 1)
            }

            return Pair(listName, null) // listName, otherFilters
        }

        return Pair(null, query) // listName, otherFilters
    }
}