package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.store.TypeConverters

data class AnnotatedChineseWord(val word: ChineseWord?, val annotation: ChineseWordAnnotation?) {
    val simplified = word?.simplified ?: annotation?.simplified

    companion object {
        fun getBlank(simplified: String = ""): AnnotatedChineseWord {
            return AnnotatedChineseWord(
                ChineseWord.getBlank(simplified),
                ChineseWordAnnotation.getBlank(simplified)
            )
        }
    }

    fun hasAnnotation(): Boolean {
        return annotation?.firstSeen != null
    }
}

private const val select_left_join =
    "SELECT a.a_simplified, COALESCE(w.simplified, a.a_simplified) simplified, a.a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, " +
            " COALESCE(w.searchable_text, '') searchable_text " +
            " FROM chinesewordannotation AS a LEFT JOIN chineseword AS w" +
            " ON a.a_simplified = w.simplified" +
            " "

private const val select_right_join =
    "SELECT COALESCE(a.a_simplified, w.simplified) a_simplified, w.simplified, " +
            " COALESCE(a.a_searchable_text, '') a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, w.searchable_text " +
            " FROM chineseword AS w LEFT JOIN chinesewordannotation AS a" +
            " ON a.a_simplified = w.simplified" +
            " "

@Dao
interface AnnotatedChineseWordDAO {
 /**   @Query(select_outer_join)
    suspend fun getAll(): List<AnnotatedChineseWord>

    @Query("$select_outer_join WHERE w.searchable_text LIKE '%' || :str || '%' ORDER BY w.popularity DESC LIMIT :pageSize OFFSET (:page * :pageSize)")
    suspend fun findWordFromStrLike(str: String?, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord>
**/
    @Query("$select_left_join WHERE a.a_searchable_text LIKE '%' || :str || '%'" +
            " UNION " +
            "$select_right_join WHERE w.searchable_text LIKE '%' || :str || '%'" +
            " ORDER BY w.popularity DESC LIMIT :pageSize OFFSET (:page * :pageSize)")
    suspend fun _findWordFromStrLike(str: String?, page: Int = 0, pageSize: Int = 30): Map<ChineseWordAnnotation, List<ChineseWord>>

    suspend fun findWordFromStrLike(str: String?, page: Int = 0, pageSize: Int = 30) : List<AnnotatedChineseWord> {
        return TypeConverters.AnnotatedChineseWordsConverter.fromMap(_findWordFromStrLike(str, page, pageSize))
    }

    @Query("$select_left_join WHERE a_simplified = :simplifiedWord" +
            " UNION " +
            "$select_right_join WHERE simplified = :simplifiedWord" +
            " LIMIT 1")
    suspend fun _findWordFromSimplified(simplifiedWord: String?): Map<ChineseWordAnnotation, List<ChineseWord>>

    suspend fun findWordFromSimplified(simplifiedWord: String?) : AnnotatedChineseWord? {
        val list = TypeConverters.AnnotatedChineseWordsConverter.fromMap(_findWordFromSimplified(simplifiedWord))

        return if (list.isEmpty())
            null
        else
            list[0]
    }
}