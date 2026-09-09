package org.hndrx.parchment.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "collections", indices = [Index(value = ["name"], unique = true)])
data class BookCollection(@PrimaryKey val id: String, val name: String,
    @ColumnInfo(defaultValue = "'PARCHMENT'") val color: String = "PARCHMENT",
    @ColumnInfo(defaultValue = "'FOLDER'") val icon: String = "FOLDER")

@Entity(
    tableName = "collection_books",
    primaryKeys = ["collectionId", "bookId"],
    foreignKeys = [
        ForeignKey(entity = BookCollection::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PdfBook::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("bookId")]
)
data class CollectionBook(val collectionId: String, val bookId: String)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<BookCollection>>
    @Query("SELECT * FROM collection_books")
    fun observeMembers(): Flow<List<CollectionBook>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collection: BookCollection)
    @Update suspend fun update(collection: BookCollection)
    @Query("SELECT * FROM collections") suspend fun getAll(): List<BookCollection>
    @Query("SELECT * FROM collection_books") suspend fun getMembers(): List<CollectionBook>
    @Query("SELECT * FROM collections WHERE name = :name")
    suspend fun find(name: String): BookCollection?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(member: CollectionBook)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(members: List<CollectionBook>)
    @Query("DELETE FROM collection_books WHERE collectionId = :collectionId AND bookId = :bookId")
    suspend fun remove(collectionId: String, bookId: String)
    @Delete suspend fun delete(collection: BookCollection)
}
