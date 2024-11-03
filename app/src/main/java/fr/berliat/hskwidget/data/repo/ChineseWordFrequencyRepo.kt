package fr.berliat.hskwidget.data.repo

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO.Companion.CHINESE_REGEX
import fr.berliat.hskwidget.data.model.ChineseWordFrequency

class ChineseWordFrequencyRepo(private val frequencyDAO: ChineseWordFrequencyDAO,
                               private val annotationDAO: AnnotatedChineseWordDAO
) {
    suspend fun increment(freq: ChineseWordFrequency) {
        increment(listOf(freq))
    }

    suspend fun increment(freq: List<ChineseWordFrequency>) {
        val words = freq.map { it.simplified }

        val currentFreq = frequencyDAO.getFrequencyMapped(words)
        val dictionary = annotationDAO.getFromSimplified(words)
        val indexedDict = dictionary.associate { it.simplified to it.word }

        val regex = Regex(CHINESE_REGEX)
        val newFreq: MutableList<ChineseWordFrequency> = mutableListOf()

        freq.forEach {
            if (regex.matches(it.simplified) && indexedDict[it.simplified] != null) {
                newFreq.add(
                    ChineseWordFrequency(
                        it.simplified,
                        currentFreq[it.simplified]?.appearanceCnt?.plus(it.appearanceCnt)
                            ?: it.appearanceCnt,
                        currentFreq[it.simplified]?.consultedCnt?.plus(it.consultedCnt)
                            ?: it.consultedCnt
                    )
                )
            }
        }

        if (newFreq.size > 0)
            frequencyDAO.insertOrUpdate(newFreq)
    }

    suspend fun incrementConsulted(words: Map<String, Int>) {
        val newFreq: MutableList<ChineseWordFrequency> = mutableListOf()

        words.forEach { (simplified, consultedCnt) ->
            newFreq.add(
                ChineseWordFrequency(
                    simplified,
                    0,
                    consultedCnt
                )
            )
        }

        increment(newFreq)
    }

    suspend fun incrementConsulted(simplified: String) {
        incrementConsulted(mapOf(Pair(simplified, 1)))
    }

    suspend fun incrementAppeared(words: Map<String, Int>) {
        val newFreq: MutableList<ChineseWordFrequency> = mutableListOf()

        words.forEach { (simplified, appearedCnt) ->
            newFreq.add(
                ChineseWordFrequency(
                    simplified,
                    appearedCnt,
                    0
                )
            )
        }

        increment(newFreq)
    }
}