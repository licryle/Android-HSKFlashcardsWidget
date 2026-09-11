package fr.berliat.hskwidget.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import fr.berliat.hskwidget.core.Locale
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord

private const val select_left_join =
    "SELECT a.a_simplified, COALESCE(w.simplified, a.a_simplified) simplified, a.a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, w.collocations, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word_annotation AS a LEFT JOIN chinese_word AS w" +
            " ON a.a_simplified = w.simplified" +
            " "

private const val select_right_join =
    "SELECT COALESCE(a.a_simplified, w.simplified) a_simplified, w.simplified, " +
            " COALESCE(a.a_searchable_text, '') a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, w.collocations, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word AS w LEFT JOIN chinese_word_annotation AS a" +
            " ON a.a_simplified = w.simplified" +
            " "

private const val order_by_logic =
    "ORDER BY ( " +
            "  (CASE WHEN simplified || ' ' || COALESCE(traditional, '') || ' ' || COALESCE(pinyins, '') || ' ' || a_searchable_text LIKE '%' || :str THEN 5 ELSE 0 END) + " +
            "  (CASE WHEN simplified || ' ' || COALESCE(traditional, '') || ' ' || COALESCE(pinyins, '') || ' ' || a_searchable_text LIKE :str || '%' THEN 10 ELSE 0 END)" +
    ") DESC, popularity DESC, is_first_seen_null, first_seen DESC "

@Dao
interface AnnotatedChineseWordDAO {
    @Query("SELECT * FROM (" +
            "$select_left_join WHERE (a.a_searchable_text LIKE '%' || :str || '%'" +
            " OR a.a_simplified LIKE '%' || :str || '%')" +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL))" +
            " AND (a.is_exam=:atExam OR :atExam IS NULL)" +
            " UNION " +
            "$select_right_join WHERE (w.simplified LIKE '%' || :str || '%' OR COALESCE(w.traditional, '') LIKE '%' || :str || '%' OR COALESCE(w.pinyins, '') LIKE '%' || :str || '%'" +
            " OR a.a_searchable_text LIKE '%' || :str || '%'" +
            " OR EXISTS (SELECT 1 FROM word_definition d WHERE d.simplified = w.simplified AND d.language = :language AND d.definition LIKE '%' || :str || '%'))" +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL))" +
            " AND (a.is_exam=:atExam OR :atExam IS NULL)" +
            ") " +
            order_by_logic +
            " LIMIT :pageSize OFFSET (:page * :pageSize)")
    @RewriteQueriesToDropUnusedColumns
    suspend fun searchFromStrLikeRows(str: String?, language: String, hasAnnotation: Boolean, atExam: Boolean? = null, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord>

    @Transaction
    suspend fun searchFromStrLike(str: String?, language: Locale, hasAnnotation: Boolean, atExam: Boolean? = null, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord> =
        hydrate(searchFromStrLikeRows(str, language.code, hasAnnotation, atExam, page, pageSize))

    @Query("SELECT * FROM (" +
           "       $select_left_join WHERE a.a_simplified IN (SELECT simplified FROM word_list_entry WHERE list_id IN (:listIds) AND simplified NOT IN (:bannedWords))" +
           " UNION " +
           "$select_right_join WHERE w.simplified IN (SELECT simplified FROM word_list_entry WHERE list_id IN (:listIds) AND simplified NOT IN (:bannedWords))" +
           ") ORDER BY RANDOM() LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getRandomWordFromListsRow(listIds: List<Long>, bannedWords: Array<String>): AnnotatedChineseWord?

    suspend fun getRandomWordFromLists(listIds: List<Long>, bannedWords: Array<String>): AnnotatedChineseWord? =
        getRandomWordFromListsRow(listIds, bannedWords)?.let { hydrate(listOf(it)).first() }

    @Query("SELECT a.a_simplified, COALESCE(w.simplified, a.a_simplified) simplified, a.a_searchable_text, " +
            " a.a_pinyins, a.notes, a.class_type, a.class_level, a.themes, a.first_seen, a.is_exam," +
            " w.traditional, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, w.collocations, " +
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
            " w.traditional, w.hsk_level, w.pinyins, w.popularity, " +
            " w.modality, w.examples, w.type, w.synonyms, w.antonym, w.collocations, " +
            " (a.first_seen IS NULL) AS is_first_seen_null " +
            " FROM chinese_word AS w  INNER JOIN word_list_entry AS wle ON w.simplified = wle.simplified " +
            " INNER JOIN word_list AS wl ON wl.id = wle.list_id " +
            " LEFT JOIN chinese_word_annotation AS a ON a.a_simplified = w.simplified " +
            " WHERE wl.name = :listName " +
            " AND (0=:hasAnnotation OR (1=:hasAnnotation AND a.first_seen IS NOT NULL)) " +
            " ORDER BY is_first_seen_null, a.first_seen DESC, w.popularity DESC " +
            " LIMIT :pageSize OFFSET (:page * :pageSize)")
    @RewriteQueriesToDropUnusedColumns
    suspend fun searchFromWordListRows(listName: String, hasAnnotation: Boolean, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord>

    suspend fun searchFromWordList(listName: String, hasAnnotation: Boolean, page: Int = 0, pageSize: Int = 30): List<AnnotatedChineseWord> =
        hydrate(searchFromWordListRows(listName, hasAnnotation, page, pageSize))

    suspend fun getAllAnnotated(): List<AnnotatedChineseWord> {
        return searchFromStrLike("", Locale.ENGLISH, hasAnnotation = true, atExam = null, 0, Int.MAX_VALUE)
    }

    suspend fun getAllAtExam(): List<AnnotatedChineseWord> {
        return searchFromStrLike("", Locale.ENGLISH, hasAnnotation = true, atExam = true, page = 0, pageSize = Int.MAX_VALUE)
    }

    @Query("$select_left_join WHERE a_simplified = :simplifiedWord" +
            " UNION " +
            "$select_right_join WHERE simplified = :simplifiedWord" +
            " LIMIT 1")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getFromSimplifiedRow(simplifiedWord: String?): AnnotatedChineseWord?

    suspend fun getFromSimplified(simplifiedWord: String?): AnnotatedChineseWord? =
        getFromSimplifiedRow(simplifiedWord)?.let { hydrate(listOf(it)).first() }

    @Query("$select_left_join WHERE a_simplified IN (:simplifiedWords)" +
            " UNION " +
            "$select_right_join WHERE simplified IN (:simplifiedWords)")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getFromSimplifiedRows(simplifiedWords: List<String>): List<AnnotatedChineseWord>

    suspend fun getFromSimplified(simplifiedWords: List<String>): List<AnnotatedChineseWord> =
        hydrate(getFromSimplifiedRows(simplifiedWords))

    @Query("SELECT * FROM word_definition WHERE simplified IN (:simplifiedWords)")
    suspend fun definitionsForWords(simplifiedWords: List<String>): List<fr.berliat.hskwidget.data.model.WordDefinition>

    private suspend fun hydrate(words: List<AnnotatedChineseWord>): List<AnnotatedChineseWord> {
        if (words.isEmpty()) return words
        val definitions = definitionsForWords(words.map { it.simplified }).groupBy { it.simplified }
        return words.map { item ->
            item.copy(word = item.word?.also { word ->
                word.definition = definitions[item.simplified].orEmpty()
                    .mapNotNull { definition -> Locale.fromCode(definition.language)?.let { it to definition.definition } }
                    .toMap()
            })
        }
    }
}
