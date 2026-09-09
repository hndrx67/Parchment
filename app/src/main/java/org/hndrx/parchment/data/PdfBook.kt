package org.hndrx.parchment.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_books")
data class PdfBook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String = "",
    val description: String = "",
    val category: String = "Unsorted",
    val tags: String = "",
    val filePath: String,
    val coverPath: String,
    val customCoverPath: String? = null,
    val pageCount: Int,
    val currentPage: Int = 0,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
) {
    val progress: Float get() = if (pageCount <= 1) 0f else currentPage.toFloat() / (pageCount - 1)
    val displayedCover: String get() = customCoverPath ?: coverPath
}
