package org.hndrx.parchment.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class Profile(@PrimaryKey val id: String = "default", val username: String = "Reader", val avatarPath: String? = null)

@Entity(tableName = "reading_history", primaryKeys = ["profileId", "bookId"])
data class ReadingHistory(val profileId: String, val bookId: String, val title: String, val totalMillis: Long = 0, val lastReadAt: Long = 0)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY username COLLATE NOCASE") fun observeAll(): Flow<List<Profile>>
    @Query("SELECT * FROM profiles") suspend fun getAll(): List<Profile>
    @Query("SELECT * FROM reading_history ORDER BY lastReadAt DESC") fun history(): Flow<List<ReadingHistory>>
    @Query("SELECT * FROM reading_history") suspend fun getHistory(): List<ReadingHistory>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(profile: Profile)
    @Upsert suspend fun save(profile: Profile)
    @Upsert suspend fun saveHistory(history: ReadingHistory)
    @Query("SELECT * FROM reading_history WHERE profileId = :profile AND bookId = :book") suspend fun getHistory(profile: String, book: String): ReadingHistory?
    @Transaction suspend fun record(profile: String, book: String, title: String, elapsed: Long, now: Long) {
        val current = getHistory(profile, book)
        saveHistory(ReadingHistory(profile, book, title, (current?.totalMillis ?: 0) + elapsed.coerceAtLeast(0), now))
    }
}
