package org.hndrx.parchment.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import org.hndrx.parchment.data.BookCollection
import org.hndrx.parchment.data.CollectionBook
import java.util.UUID
import org.hndrx.parchment.ParchmentApplication
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.settings.ReaderPreferences
import org.hndrx.parchment.data.Profile
import org.hndrx.parchment.data.ReadingHistory
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BookSort { RECENTLY_ADDED, TITLE, LAST_OPENED }

data class LibraryUiState(
    val books: List<PdfBook> = emptyList(),
    val query: String = "",
    val sort: BookSort = BookSort.RECENTLY_ADDED,
    val favoritesOnly: Boolean = false,
    val gridMode: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
    val availableTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val importCompleted: Int = 0,
    val importTotal: Int = 0,
    val duplicates: List<String> = emptyList(),
    val importedCount: Int = 0,
    val refreshing: Boolean = false,
    val refreshVersion: Int = 0,
    val managing: Boolean = false,
    val initialized: Boolean = false
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ParchmentApplication).container
    private val repository = container.repository
    private val settingsRepository = container.settings
    private val settings = MutableStateFlow(LibraryUiState())
    private var importJob: Job? = null
    val collections = container.collections.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val collectionMembers = container.collections.observeMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val profiles = container.profiles.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeProfile = combine(profiles, settingsRepository.activeProfileId) { list, id -> list.find { it.id == id } ?: list.firstOrNull() ?: Profile() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
    val history = container.profiles.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recoveryState = MutableStateFlow(RecoveryState())
    private var recoveryJob: Job? = null

    val state = combine(repository.books, settings) { books, ui ->
        val filtered = books.filter {
            (!ui.favoritesOnly || it.isFavorite) &&
                (ui.selectedTags.isEmpty() || it.tagList().any { tag -> tag in ui.selectedTags }) &&
                (ui.query.isBlank() || listOf(it.title, it.author, it.category, it.tags)
                    .any { value -> value.contains(ui.query, ignoreCase = true) })
        }
        val sorted = when (ui.sort) {
            BookSort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
            BookSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            BookSort.LAST_OPENED -> filtered.sortedByDescending { it.lastOpenedAt ?: 0L }
        }
        ui.copy(books = sorted, availableTags = books.flatMap { it.tagList() }.distinct().sorted())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
    val readerPreferences = settingsRepository.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderPreferences(publicMode = true)
    )

    init {
        viewModelScope.launch { container.profiles.insert(Profile()) }
        viewModelScope.launch {
            settingsRepository.libraryPreferences.collect { saved ->
                settings.update { it.copy(query = saved.query, sort = runCatching { BookSort.valueOf(saved.sort) }.getOrDefault(BookSort.RECENTLY_ADDED),
                    favoritesOnly = saved.favoritesOnly, selectedTags = saved.tags, gridMode = saved.grid, initialized = true) }
            }
        }
    }

    fun setQuery(value: String) = viewModelScope.launch { settingsRepository.setQuery(value) }
    fun setSort(value: BookSort) = viewModelScope.launch { settingsRepository.setSort(value.name) }
    fun toggleFavorites() = viewModelScope.launch { settingsRepository.toggleFavorites() }
    fun toggleLayout() = viewModelScope.launch { settingsRepository.toggleGrid() }
    fun clearMessage() = settings.update { it.copy(message = null) }
    fun clearDuplicates() = settings.update { it.copy(duplicates = emptyList()) }
    fun toggleTag(tag: String) = viewModelScope.launch { settingsRepository.toggleTag(tag) }
    fun clearTags() = viewModelScope.launch { settingsRepository.clearTags() }
    fun refresh() {
        if (settings.value.refreshing || settings.value.busy) return
        settings.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            try {
                repository.refresh()
                settings.update { it.copy(refreshVersion = it.refreshVersion + 1) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { settings.update { it.copy(message = "Refresh failed: ${error.localizedMessage}") } }
            finally { settings.update { it.copy(refreshing = false) } }
        }
    }

    fun importAll(uris: List<Uri>) {
        if (uris.isEmpty() || settings.value.busy) return
        settings.update { it.copy(busy = true, importCompleted = 0, importTotal = uris.size, duplicates = emptyList(), message = null) }
        importJob = viewModelScope.launch {
            var committed = false
            try {
                val destination = settingsRepository.preferences.first().storageDirectory
                repository.importAll(uris, directory = destination, onCommitted = { result ->
                    committed = true
                    settings.update { it.copy(importedCount = result.imported, duplicates = result.duplicates,
                        message = "Imported ${result.imported} PDF${if (result.imported == 1) "" else "s"}; ${result.duplicates.size} duplicates skipped") }
                }) { completed, total ->
                    settings.update { it.copy(importCompleted = completed, importTotal = total) }
                }
            } catch (error: CancellationException) {
                if (!committed) settings.update { it.copy(message = "Import aborted. No files were added.") }
                throw error
            } catch (error: Exception) {
                settings.update { it.copy(message = "Import failed. No files were added. ${error.localizedMessage.orEmpty()}") }
            } finally {
                settings.update { it.copy(busy = false) }
            }
        }
    }
    fun abortImport() { importJob?.cancel() }

    fun createCollection(name: String, bookId: String? = null) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        container.collections.insert(BookCollection(UUID.randomUUID().toString(), trimmed))
        val collection = container.collections.find(trimmed) ?: return@launch
        if (bookId != null) container.collections.add(CollectionBook(collection.id, bookId))
    }
    fun addToCollection(collectionId: String, bookId: String) = viewModelScope.launch {
        container.collections.add(CollectionBook(collectionId, bookId))
    }
    fun removeFromCollection(collectionId: String, bookId: String) = viewModelScope.launch {
        container.collections.remove(collectionId, bookId)
    }
    fun deleteCollection(collection: BookCollection) = viewModelScope.launch { container.collections.delete(collection) }

    private fun manage(action: suspend () -> Unit) {
        if (settings.value.managing) return
        settings.update { it.copy(managing = true) }
        viewModelScope.launch {
            try { action(); settings.update { it.copy(message = "Documents updated") } }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { settings.update { it.copy(message = "Could not complete the changes: ${error.localizedMessage}") } }
            finally { settings.update { it.copy(managing = false) } }
        }
    }
    fun addTags(ids: Set<String>, tags: List<String>) = manage { repository.addTags(ids, tags) }
    fun setFavorites(ids: Set<String>, favorite: Boolean) = manage { repository.setFavorites(ids, favorite) }
    fun deleteBooks(ids: Set<String>) = manage { repository.deleteAll(ids) }
    fun addBooksToCollection(ids: Set<String>, collectionId: String) = manage {
        container.collections.addAll(ids.map { CollectionBook(collectionId, it) })
    }
    fun createCollectionForBooks(name: String, ids: Set<String>) = manage {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        container.collections.insert(BookCollection(UUID.randomUUID().toString(), trimmed))
        val collection = requireNotNull(container.collections.find(trimmed))
        container.collections.addAll(ids.map { CollectionBook(collection.id, it) })
    }

    fun update(book: PdfBook) = viewModelScope.launch { repository.update(book) }
    fun delete(book: PdfBook) = viewModelScope.launch { repository.delete(book) }
    fun setProgress(id: String, page: Int) = viewModelScope.launch { repository.setProgress(id, page) }
    fun setCover(id: String, uri: Uri) = manage { repository.setCustomCover(id, uri) }
    fun revertCover(id: String) = manage { repository.revertCover(id) }
    fun saveDetails(book: PdfBook) = manage { repository.updateDetails(book) }
    fun observeBook(id: String) = repository.observeBook(id)
    fun updatePreferences(value: ReaderPreferences) = viewModelScope.launch { settingsRepository.update(value) }
    fun customizeCollection(value: BookCollection) = manage { container.collections.update(value) }
    fun selectProfile(id: String) = viewModelScope.launch { settingsRepository.setProfile(id) }
    fun saveProfile(profile: Profile) = manage { container.profiles.save(profile); settingsRepository.setProfile(profile.id) }
    fun setProfilePicture(profile: Profile, uri: Uri) = manage {
        val path = withContext(Dispatchers.IO) { container.storage.copyCover("profile_${profile.id}", uri) }
        container.profiles.save(profile.copy(avatarPath = path))
    }
    fun recordReading(profileId: String, book: PdfBook, elapsed: Long) = viewModelScope.launch(NonCancellable) {
        container.profiles.record(profileId, book.id, book.title, elapsed, System.currentTimeMillis())
    }
    fun backup(uri: Uri, pdfs: Boolean) = recoverTask { container.recovery.backup(uri, pdfs) { status -> recoveryState.value = RecoveryState(true, status) }; "Backup complete" }
    fun restore(uri: Uri, archive: Boolean) = recoverTask { container.recovery.restore(uri, archive) { status -> recoveryState.value = RecoveryState(true, status) } }
    private fun recoverTask(action: suspend () -> String) {
        if (recoveryState.value.busy) return
        recoveryState.value = RecoveryState(true, "Preparing…")
        recoveryJob = viewModelScope.launch {
            try { recoveryState.value = RecoveryState(false, action()); repository.refresh() }
            catch (error: CancellationException) { recoveryState.value = RecoveryState(false, "Operation cancelled"); throw error }
            catch (error: Exception) { recoveryState.value = RecoveryState(false, "Could not complete recovery operation: ${error.localizedMessage}") }
        }
    }
}

data class RecoveryState(val busy: Boolean = false, val message: String = "")

fun PdfBook.tagList(): List<String> = tags.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
fun PdfBook.hideCover(publicMode: Boolean): Boolean = publicMode && "adult content" in tagList()
