package fr.berliat.hskwidget.data

import com.opencsv.CSVReader
import java.util.Locale

class ChineseWords: ArrayList<ChineseWord>() {
    fun addFromCSVResource(hsk: ChineseWord.HSK_Level, reader: CSVReader) {
        var nextLine: Array<String>?
        nextLine = reader.readNext()
        while (nextLine != null) {
            this.add(ChineseWord(
                nextLine[0],
                "",
                mapOf(Locale.ENGLISH to nextLine[2]),
                hsk,
                ChineseWord.Pinyins(nextLine[1])
            ))

            nextLine = reader.readNext()
        }
    }
}