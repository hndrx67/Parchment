package org.hndrx.parchment.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PdfBook::class, BookCollection::class, CollectionBook::class, Profile::class, ReadingHistory::class], version = 3, exportSchema = false)
abstract class ParchmentDatabase : RoomDatabase() {
    abstract fun books(): PdfBookDao
    abstract fun collections(): CollectionDao
    abstract fun profiles(): ProfileDao

    companion object {
        fun create(context: Context, name: String = "parchment.db") = Room.databaseBuilder(
            context.applicationContext,
            ParchmentDatabase::class.java,
            name
        ).addMigrations(object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_collections_name ON collections (name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS collection_books (collectionId TEXT NOT NULL, bookId TEXT NOT NULL, PRIMARY KEY(collectionId, bookId), FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(bookId) REFERENCES pdf_books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_books_bookId ON collection_books (bookId)")
            }
        }, object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN color TEXT NOT NULL DEFAULT 'PARCHMENT'")
                db.execSQL("ALTER TABLE collections ADD COLUMN icon TEXT NOT NULL DEFAULT 'FOLDER'")
                db.execSQL("CREATE TABLE IF NOT EXISTS profiles (id TEXT NOT NULL, username TEXT NOT NULL, avatarPath TEXT, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS reading_history (profileId TEXT NOT NULL, bookId TEXT NOT NULL, title TEXT NOT NULL, totalMillis INTEGER NOT NULL, lastReadAt INTEGER NOT NULL, PRIMARY KEY(profileId, bookId))")
                db.execSQL("INSERT OR IGNORE INTO profiles (id, username) VALUES ('default', 'Reader')")
            }
        }).build()
    }
}
