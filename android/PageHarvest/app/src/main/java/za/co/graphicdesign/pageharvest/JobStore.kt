package za.co.graphicdesign.pageharvest

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class JobStore(context: Context) {
    private val prefs = context.getSharedPreferences("pageharvest_job", Context.MODE_PRIVATE)

    fun persist(config: HarvestConfig) {
        prefs.edit().putString("active", config.toJson()).apply()
    }

    fun lastConfig(): HarvestConfig? = prefs.getString("active", null)?.let {
        runCatching { HarvestConfig.fromJson(it) }.getOrNull()
    }

    fun savePage(page: Int) {
        prefs.edit().putInt("page", page).apply()
    }

    fun lastPage(): Int = prefs.getInt("page", 0)

    fun clearPage() {
        prefs.edit().putInt("page", 0).apply()
    }
}

data class SavedRow(
    val sourceUrl: String,
    val pageUrl: String,
    val name: String,
    val sha256: String,
    val time: Long
)

class DownloadRegistry(context: Context) :
    SQLiteOpenHelper(context, "pageharvest.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE downloads (
                collection_key TEXT NOT NULL,
                source_url TEXT NOT NULL,
                page_url TEXT NOT NULL,
                filename TEXT NOT NULL,
                sha256 TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                PRIMARY KEY (collection_key, source_url),
                UNIQUE (collection_key, sha256)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun hasUrl(key: String, url: String): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM downloads WHERE collection_key=? AND source_url=? LIMIT 1",
            arrayOf(key, url)
        ).use { it.moveToFirst() }

    fun hasHash(key: String, hash: String): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM downloads WHERE collection_key=? AND sha256=? LIMIT 1",
            arrayOf(key, hash)
        ).use { it.moveToFirst() }

    fun record(key: String, row: SavedRow): Boolean {
        val values = ContentValues().apply {
            put("collection_key", key)
            put("source_url", row.sourceUrl)
            put("page_url", row.pageUrl)
            put("filename", row.name)
            put("sha256", row.sha256)
            put("timestamp", row.time)
        }
        return writableDatabase.insertWithOnConflict(
            "downloads", null, values, SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun count(key: String): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM downloads WHERE collection_key=?",
            arrayOf(key)
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun list(key: String): List<SavedRow> {
        val rows = mutableListOf<SavedRow>()
        readableDatabase.rawQuery(
            """
            SELECT source_url, page_url, filename, sha256, timestamp
            FROM downloads WHERE collection_key=? ORDER BY timestamp, filename
            """.trimIndent(),
            arrayOf(key)
        ).use { cursor ->
            while (cursor.moveToNext()) rows += SavedRow(
                cursor.getString(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getLong(4)
            )
        }
        return rows
    }
}
