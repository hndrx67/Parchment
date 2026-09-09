package org.hndrx.parchment.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfBookDao {
    @Query("SELECT * FROM pdf_books ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books WHERE id = :id")
    fun observe(id: String): Flow<PdfBook?>

    @Query("SELECT * FROM pdf_books WHERE id = :id")
    suspend fun get(id: String): PdfBook?

    @Query("SELECT * FROM pdf_books")
    suspend fun getAll(): List<PdfBook>

    @Insert
    suspend fun insertAll(books: List<PdfBook>)
    @Upsert suspend fun restoreAll(books: List<PdfBook>)
    @Query("UPDATE pdf_books SET currentPage = :page, lastOpenedAt = :now WHERE id = :id")
    suspend fun setProgress(id: String, page: Int, now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: PdfBook)

    @Update suspend fun update(book: PdfBook)
    @Update suspend fun updateAll(books: List<PdfBook>)
    @Query("UPDATE pdf_books SET title = :title, author = :author, description = :description, category = :category, tags = :tags WHERE id = :id")
    suspend fun updateDetails(id: String, title: String, author: String, description: String, category: String, tags: String)
    @Query("UPDATE pdf_books SET customCoverPath = :path WHERE id = :id")
    suspend fun setCover(id: String, path: String?)
    @Query("SELECT * FROM pdf_books WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PdfBook>
    @Query("UPDATE pdf_books SET isFavorite = :favorite WHERE id IN (:ids)")
    suspend fun setFavorites(ids: List<String>, favorite: Boolean)
    @Transaction
    suspend fun addTags(ids: List<String>, tags: List<String>) {
        updateAll(getByIds(ids).map { book ->
            val combined = (book.tags.split(',') + tags).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
            book.copy(tags = combined.joinToString(", "))
        })
    }
    @Delete suspend fun delete(book: PdfBook)
}
