package fr.berliat.hskwidget.data.store

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase.Companion.DATABASE_FILE
import java.io.File

open class ReplaceTablesFromAssetMigration(
    fromVersion: Int,
    toVersion: Int,
    private val context: Context
) : Migration(fromVersion, toVersion) {
    fun replaceTables(db: SupportSQLiteDatabase, tableNames: List<String>) {
        // 1. Copy asset DB to temporary file
        val tempDbFile = File(context.cacheDir, "temp_chinese_words.db")
        context.assets.open("databases/$DATABASE_FILE").use { input ->
            tempDbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 2. Open asset DB directly (read-only)
        val assetDb = SQLiteDatabase.openDatabase(
            tempDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        for (tableName in tableNames) {
            replaceTable(assetDb, db, tableName)
        }

        assetDb.close()
        tempDbFile.delete()
    }

    private fun replaceTable(sourceDb: SQLiteDatabase, targetDb: SupportSQLiteDatabase, tableName: String) {
        val cursor = sourceDb.query(tableName, null, null, null, null, null, null)
        val columnNames = cursor.columnNames
        val placeholders = columnNames.joinToString(", ") { "?" }
        val insertSql = "INSERT INTO $tableName (${columnNames.joinToString(", ")}) VALUES ($placeholders)"

        targetDb.execSQL("DELETE FROM $tableName")
        val stmt = targetDb.compileStatement(insertSql)

        while (cursor.moveToNext()) {
            stmt.clearBindings()
            for ((i, name) in columnNames.withIndex()) {
                val colIndex = cursor.getColumnIndexOrThrow(name)
                val bindIndex = i + 1 // SQLiteStatement uses 1-based indexing

                if (cursor.isNull(colIndex)) {
                    stmt.bindNull(bindIndex)
                } else {
                    when (cursor.getType(colIndex)) {
                        Cursor.FIELD_TYPE_INTEGER -> stmt.bindLong(bindIndex, cursor.getLong(colIndex))
                        Cursor.FIELD_TYPE_FLOAT -> stmt.bindDouble(bindIndex, cursor.getDouble(colIndex))
                        Cursor.FIELD_TYPE_STRING -> stmt.bindString(bindIndex, cursor.getString(colIndex))
                        Cursor.FIELD_TYPE_BLOB -> stmt.bindBlob(bindIndex, cursor.getBlob(colIndex))
                        else -> stmt.bindNull(bindIndex) // Fallback
                    }
                }
            }
            stmt.executeInsert()
        }

        cursor.close()
    }

    companion object {
        fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
            val cursor = db.query("PRAGMA table_info($tableName)")
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                columns.add(columnName)
            }
            cursor.close()
            return columns
        }
    }
}
