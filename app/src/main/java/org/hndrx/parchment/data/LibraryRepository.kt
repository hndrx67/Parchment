package org.hndrx.parchment.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hndrx.parchment.pdf.PdfStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepository(private val dao: PdfBookDao, private val storage: PdfStorage) {
    private val importMutex = Mutex()
    private val coverMutex = Mutex()
    private val refreshVersion = MutableStateFlow(0)
    val books = refreshVersion.flatMapLatest { dao.observeAll() }
    suspend fun refresh() {
        dao.getAll()
        refreshVersion.update { it + 1 }
    }
    fun observeBook(id: String) = dao.observe(id)
    // Stage new PDFs; duplicate copies are discarded without rejecting the other files.
    suspend fun importAll(uris: List<Uri>, directory: String = "", onCommitted: (ImportResult) -> Unit = {}, progress: (Int, Int) -> Unit): ImportResult = importMutex.withLock {
        withContext(Dispatchers.IO) {
            val staged = mutableListOf<PdfBook>()
            var committed = false
            try {
                val known = mutableMapOf<String, String>()
                dao.getAll().forEach { book ->
                    try { known[storage.fingerprint(book.filePath)] = book.title }
                    catch (_: java.io.FileNotFoundException) { /* Configuration-only recovery may reference PDFs not yet restored. */ }
                }
                val duplicates = mutableListOf<String>()
                uris.forEachIndexed { index, uri ->
                    currentCoroutineContext().ensureActive()
                    progress(index, uris.size)
                    val book = storage.importPdf(uri)
                    staged.add(book)
                    val hash = storage.fingerprint(book.filePath)
                    val original = known.putIfAbsent(hash, book.title)
                    if (original != null) {
                        duplicates.add("${book.title} — matches $original")
                        storage.deleteFiles(book)
                        staged.remove(book)
                    }
                    progress(index + 1, uris.size)
                }
                if (directory.isNotBlank()) {
                    staged.indices.forEach { index ->
                        currentCoroutineContext().ensureActive()
                        staged[index] = storage.moveToDirectory(staged[index], Uri.parse(directory))
                    }
                }
                currentCoroutineContext().ensureActive()
                val result = ImportResult(staged.size, duplicates)
                // Finish the atomic insert even if Abort is tapped at the commit boundary.
                withContext(NonCancellable) {
                    dao.insertAll(staged)
                    committed = true
                    onCommitted(result)
                }
                result
            } finally {
                if (!committed) staged.forEach(storage::deleteFiles)
            }
        }
    }
    suspend fun update(book: PdfBook) = dao.update(book)
    suspend fun updateDetails(book: PdfBook) = dao.updateDetails(book.id, book.title, book.author, book.description, book.category, book.tags)
    suspend fun addTags(ids: Set<String>, tags: List<String>) = dao.addTags(ids.toList(), tags)
    suspend fun setFavorites(ids: Set<String>, favorite: Boolean) = dao.setFavorites(ids.toList(), favorite)
    suspend fun deleteAll(ids: Set<String>) { dao.getByIds(ids.toList()).forEach { delete(it) } }
    suspend fun setProgress(id: String, page: Int) {
        dao.setProgress(id, page, System.currentTimeMillis())
    }
    suspend fun setCustomCover(id: String, uri: Uri) = coverMutex.withLock {
        dao.get(id)?.let {
            withContext(Dispatchers.IO + NonCancellable) {
                val cover = storage.copyCover(it.id, uri)
                try { dao.setCover(id, cover) }
                catch (error: Throwable) { storage.deleteCover(cover); throw error }
                storage.deleteCover(it.customCoverPath)
            }
        }
    }
    suspend fun revertCover(id: String) = coverMutex.withLock {
        dao.get(id)?.let { book ->
            withContext(Dispatchers.IO + NonCancellable) {
                dao.setCover(id, null)
                storage.deleteCover(book.customCoverPath)
            }
        }
    }
    suspend fun delete(book: PdfBook) {
        withContext(Dispatchers.IO) { storage.deleteFiles(book) }
        dao.delete(book)
    }
}

data class ImportResult(val imported: Int, val duplicates: List<String>)
