package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord

private const val select_left_join =
    "SELECT a.a_simplified, COALESCE(w.simplified, a.a_simplified) simplified, a.a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, " +
            " COALESCE(w.searchable_text, '') searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word_annotation AS a LEFT JOIN chinese_word AS w" +
            " ON a.a_simplified = w.simplified" +
            " "

private const val select_right_join =
    "SELECT COALESCE(a.a_simplified, w.simplified) a_simplified, w.simplified, " +
            " COALESCE(a.a_searchable_text, '') a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, " +
            " w.searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word AS w LEFT JOIN chinese_word_annotation AS a" +
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

    @Query("SELECT * FROM (" +
           "       $select_left_join WHERE a.a_simplified IN (SELECT simplified FROM word_list_entry WHERE list_id IN (:listIds) AND simplified NOT IN (:bannedWords))" +
           " UNION " +
           "$select_right_join WHERE w.simplified IN (SELECT simplified FROM word_list_entry WHERE list_id IN (:listIds) AND simplified NOT IN (:bannedWords))" +
           ") ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWordFromLists(listIds: List<Long>, bannedWords: Array<String>): AnnotatedChineseWord?

    @Query("SELECT a.a_simplified, COALESCE(w.simplified, a.a_simplified) simplified, a.a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, " +
            " COALESCE(w.searchable_text, '') searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word_annotation AS a INNER JOIN word_list_entry AS wle ON a.a_simplified = wle.simplified " +
            " INNER JOIN word_list AS wl ON wl.id = wle.list_id " +
            " LEFT JOIN chinese_word AS w ON a.a_simplified = w.simplified " +
            " WHERE wl.name = :listName " +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL)) " +
            " UNION " +
            " SELECT COALESCE(a.a_simplified, w.simplified) a_simplified, w.simplified, " +
            " COALESCE(a.a_searchable_text, '') a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.definition, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, " +
            " w.searchable_text, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word AS w  INNER JOIN word_list_entry AS wle ON w.simplified = wle.simplified " +
            " INNER JOIN word_list AS wl ON wl.id = wle.list_id " +
            " LEFT JOIN chinese_word_annotation AS a ON a.a_simplified = w.simplified " +
            " WHERE wl.name = :listName " +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL)) " +
            " ORDER BY is_first_seen_null, a.first_seen DESC, w.popularity DESC " +
            " LIMIT :pageSize OFFSET (:page * :pageSize)")
    suspend fun searchFromWordList(listName: String, hasAnnotation: Boolean, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord>

    suspend fun getAllAnnotated(): List<AnnotatedChineseWord> {
        return searchFromStrLike("", true, 0, Int.MAX_VALUE)
    }

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