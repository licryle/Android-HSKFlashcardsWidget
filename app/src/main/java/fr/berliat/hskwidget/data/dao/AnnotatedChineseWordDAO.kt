package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation

data class AnnotatedChineseWord(
    @Embedded val word: ChineseWord?,
    @Embedded val annotation: ChineseWordAnnotation?) {

    val simplified: String
        get() {
            return word?.simplified ?: annotation?.simplified!!
        }

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
            " COALESCE(w.searchable_text, '') searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinesewordannotation AS a LEFT JOIN chineseword AS w" +
            " ON a.a_simplified = w.simplified" +
            " "

private const val select_right_join =
    "SELECT COALESCE(a.a_simplified, w.simplified) a_simplified, w.simplified, " +
            " COALESCE(a.a_searchable_text, '') a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, w.searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chineseword AS w LEFT JOIN chinesewordannotation AS a" +
            " ON a.a_simplified = w.simplified" +
            " "

@Dao
interface AnnotatedChineseWordDAO {
     @Query("$select_left_join WHERE a.a_searchable_text LIKE '%' || :str || '%'" +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL))" +
            " UNION " +
            "$select_right_join WHERE w.searchable_text LIKE '%' || :str || '%'" +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL))" +
            " ORDER BY is_first_seen_null, a.first_seen DESC, w.popularity DESC " +
            " LIMIT :pageSize OFFSET (:page * :pageSize)")
    suspend fun searchFromStrLike(str: String?, hasAnnotation: Boolean, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord>

    @Query("$select_left_join WHERE a_simplified = :simplifiedWord" +
            " UNION " +
            "$select_right_join WHERE simplified = :simplifiedWord" +
            " LIMIT 1")
    suspend fun getFromSimplified(simplifiedWord: String?): AnnotatedChineseWord?

    @Query("$select_left_join WHERE a_simplified IN (:simplifiedWords)" +
            " UNION " +
            "$select_right_join WHERE simplified IN (:simplifiedWords)")
    suspend fun getFromSimplified(simplifiedWords: List<String>): List<AnnotatedChineseWord>
}