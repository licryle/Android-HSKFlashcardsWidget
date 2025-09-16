package fr.berliat.hsktextviews

interface HSKTextSegmenter {
    var listener: HSKTextSegmenterListener?

    fun segment(text: String): Array<String>?
    fun isReady(): Boolean
}

interface HSKTextSegmenterListener {
    fun onIsSegmenterReady()
}