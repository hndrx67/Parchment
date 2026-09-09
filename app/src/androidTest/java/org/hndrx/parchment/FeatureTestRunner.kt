package org.hndrx.parchment

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hndrx.parchment.data.*
import org.hndrx.parchment.pdf.PdfStorage
import org.hndrx.parchment.settings.SettingsRepository
import java.io.File
import java.util.UUID
import android.provider.DocumentsContract
import org.hndrx.parchment.pdf.PdfPageRenderer
import org.hndrx.parchment.ui.hideCover
import org.hndrx.parchment.ui.tagList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.hndrx.parchment.settings.AppPalette
import org.hndrx.parchment.settings.ReaderPreferences
import org.hndrx.parchment.ui.theme.paletteColors
import org.hndrx.parchment.ui.profile.ForegroundReadingClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Device integration tests using the platform runner, without an extra test dependency. */
class FeatureTestRunner : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val tests: List<Pair<String, suspend () -> Unit>> = listOf(
            "migrationPreservesLibrary" to { migrationPreservesLibrary() },
            "renamedDuplicateKeepsNewFiles" to { fixture { db, storage, root ->
                val repository = LibraryRepository(db.books(), storage)
                val original = pdf(root, "original.pdf", "Original")
                repository.importAll(listOf(original)) { _, _ -> }
                val renamed = File(root, "renamed.pdf").also { File(original.path!!).copyTo(it) }
                val another = pdf(root, "another.pdf", "Different content")
                val result = repository.importAll(listOf(Uri.fromFile(renamed), another)) { _, _ -> }
                check(result.imported == 1 && result.duplicates.size == 1)
                check(db.books().getAll().size == 2)
                check(File(root, "documents").listFiles()!!.size == 2)
                check(File(root, "covers").listFiles()!!.size == 2)
            } },
            "duplicatesWithinSelectionKeepOneCopy" to { fixture { db, storage, root ->
                val uri = pdf(root, "same.pdf", "Same")
                val result = LibraryRepository(db.books(), storage).importAll(listOf(uri, uri)) { _, _ -> }
                check(result.imported == 1 && result.duplicates.size == 1)
                check(db.books().getAll().size == 1)
                check(File(root, "documents").listFiles()!!.size == 1)
                check(File(root, "covers").listFiles()!!.size == 1)
            } },
            "abortRollsBackStagedFiles" to { fixture { db, storage, root ->
                val first = pdf(root, "first.pdf", "First")
                val second = pdf(root, "second.pdf", "Second")
                coroutineScope {
                    val job = launch {
                        LibraryRepository(db.books(), storage).importAll(listOf(first, second)) { done, _ ->
                            if (done == 1) cancel("Abort")
                        }
                    }
                    job.join()
                    check(job.isCancelled)
                }
                check(db.books().getAll().isEmpty())
                check(File(root, "documents").listFiles()!!.isEmpty())
                check(File(root, "covers").listFiles()!!.isEmpty())
            } },
            "invalidPdfRollsBackBatch" to { fixture { db, storage, root ->
                val good = pdf(root, "good.pdf", "Good")
                val bad = File(root, "bad.pdf").apply { writeText("not a PDF") }
                check(runCatching { LibraryRepository(db.books(), storage).importAll(listOf(good, Uri.fromFile(bad))) { _, _ -> } }.isFailure)
                check(db.books().getAll().isEmpty())
                check(File(root, "documents").listFiles()!!.isEmpty())
                check(File(root, "covers").listFiles()!!.isEmpty())
            } },
            "collectionMembershipAndCascade" to { fixture { db, storage, root ->
                LibraryRepository(db.books(), storage).importAll(listOf(pdf(root, "book.pdf", "Book"))) { _, _ -> }
                val book = db.books().getAll().single()
                val collection = BookCollection("collection", "Reading")
                db.collections().insert(collection)
                db.collections().add(CollectionBook(collection.id, book.id))
                db.collections().add(CollectionBook(collection.id, book.id))
                check(db.collections().observeMembers().first().size == 1)
                db.collections().remove(collection.id, book.id)
                check(db.collections().observeMembers().first().isEmpty())
                db.collections().add(CollectionBook(collection.id, book.id))
                db.collections().delete(collection)
                check(db.collections().observeMembers().first().isEmpty())
                check(db.books().getAll().size == 1)
                db.collections().insert(collection)
                db.collections().add(CollectionBook(collection.id, book.id))
                db.books().delete(book)
                check(db.collections().observeMembers().first().isEmpty())
            } },
            "appearancePreferencesPersist" to {
                val root = File(targetContext.cacheDir, "settings-test-${UUID.randomUUID()}").apply { mkdirs() }
                val context = isolatedContext(root)
                val settings = SettingsRepository(context)
                val initial = settings.preferences.first()
                settings.update(initial.copy(animationsEnabled = false, gridSize = 187, publicMode = true, storageDirectory = "content://test/tree/folder"))
                val saved = SettingsRepository(context).preferences.first()
                check(!saved.animationsEnabled && saved.gridSize == 187)
                check(saved.publicMode && saved.storageDirectory == "content://test/tree/folder")
                settings.update(saved.copy(gridSize = 900))
                check(settings.preferences.first().gridSize == 400)
            },
            "batchTagsPreserveMetadataAndMaskCovers" to { fixture { db, storage, root ->
                val repository = LibraryRepository(db.books(), storage)
                repository.importAll(listOf(pdf(root, "one.pdf", "One"), pdf(root, "two.pdf", "Two"))) { _, _ -> }
                val before = db.books().getAll()
                db.books().update(before.first().copy(tags = "History", currentPage = 3))
                val ids = before.map { it.id }.toSet()
                repository.addTags(ids, listOf(" Adult Content ", "history"))
                val after = db.books().getAll()
                check(after.all { it.tagList().toSet() == setOf("history", "adult content") })
                check(after.first { it.id == before.first().id }.currentPage == 3)
                check(after.all { it.hideCover(true) && !it.hideCover(false) })
                val collection = BookCollection("bulk", "Bulk")
                db.collections().insert(collection)
                db.collections().addAll(ids.map { CollectionBook(collection.id, it) })
                check(db.collections().observeMembers().first().size == 2)
            } },
            "refreshRequeriesLibrary" to { fixture { db, storage, root ->
                val repository = LibraryRepository(db.books(), storage)
                repository.importAll(listOf(pdf(root, "refresh.pdf", "Refresh"))) { _, _ -> }
                coroutineScope {
                    val snapshots = mutableListOf<List<PdfBook>>()
                    val firstRead = CompletableDeferred<Unit>()
                    val collection = launch { repository.books.take(2).collect { snapshots.add(it); firstRead.complete(Unit) } }
                    firstRead.await()
                    // Direct SQL simulates an update that bypasses Room's invalidation notification.
                    db.openHelper.writableDatabase.execSQL("UPDATE pdf_books SET title = 'Refreshed title'")
                    repository.refresh()
                    withTimeout(5_000) { collection.join() }
                    check(snapshots.last().single().title == "Refreshed title")
                }
            } },
            "customCoverSurvivesMetadataSaveAndReverts" to { fixture { db, storage, root ->
                val repository = LibraryRepository(db.books(), storage)
                repository.importAll(listOf(pdf(root, "cover.pdf", "Cover"))) { _, _ -> }
                val original = db.books().getAll().single()
                val image = File(root, "custom.png")
                val bitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.RED)
                image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                repository.setCustomCover(original.id, Uri.fromFile(image))
                val first = db.books().get(original.id)!!.customCoverPath!!
                repository.updateDetails(original.copy(title = "Edited metadata"))
                check(db.books().get(original.id)!!.customCoverPath == first)
                repository.setCustomCover(original.id, Uri.fromFile(image))
                val second = db.books().get(original.id)!!.customCoverPath!!
                check(second != first && File(second).exists() && !File(first).exists())
                repository.revertCover(original.id)
                val reverted = db.books().get(original.id)!!
                check(reverted.customCoverPath == null && reverted.displayedCover == original.coverPath)
                check(File(original.coverPath).exists() && !File(second).exists())
                check(reverted.title == "Edited metadata")
            } },
            "libraryStatePersists" to {
                val root = File(targetContext.cacheDir, "library-prefs-${UUID.randomUUID()}").apply { mkdirs() }
                val settings = SettingsRepository(isolatedContext(root))
                val initial = settings.libraryPreferences.first()
                settings.setQuery("Saved search"); settings.setSort("TITLE"); settings.clearTags(); settings.toggleTag("history")
                settings.toggleFavorites(); settings.toggleGrid()
                val restored = SettingsRepository(isolatedContext(root)).libraryPreferences.first()
                check(restored.query == "Saved search" && restored.sort == "TITLE" && restored.tags == setOf("history"))
                check(restored.grid != initial.grid && restored.favoritesOnly != initial.favoritesOnly)
            },
            "palettesHaveReadableText" to {
                fun contrast(a: Color, b: Color): Double {
                    val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
                    return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
                }
                AppPalette.entries.forEach { palette -> listOf(false, true).forEach { dark ->
                    val colors = paletteColors(palette, dark)
                    check(contrast(colors.primary, colors.onPrimary) >= 4.5) { "$palette primary contrast" }
                    check(contrast(colors.surface, colors.onSurface) >= 4.5)
                    check(contrast(colors.primaryContainer, colors.onPrimaryContainer) >= 4.5)
                } }
                check(paletteColors(AppPalette.OLED_BLACK, true).background == Color.Black)
            },
            "foregroundClockExcludesBackground" to {
                var now = 100L; val clock = ForegroundReadingClock { now }
                clock.resume(); now += 5_000; check(clock.checkpoint() == 5_000L)
                now += 2_000; check(clock.pause() == 2_000L)
                now += 90_000; check(clock.checkpoint() == 0L)
                clock.resume(); now += 3_000; check(clock.pause() == 3_000L)
            },
            "profilesKeepSeparateReadingTotals" to { fixture { db, _, _ ->
                db.profiles().insert(Profile("one", "One")); db.profiles().insert(Profile("two", "Two"))
                db.profiles().record("one", "book", "Title", 5_000, 100)
                db.profiles().record("one", "book", "Title", 7_000, 200)
                db.profiles().record("two", "book", "Title", 2_000, 300)
                check(db.profiles().getHistory("one", "book")!!.totalMillis == 12_000L)
                check(db.profiles().getHistory("two", "book")!!.totalMillis == 2_000L)
            } },
            "fullBackupRestoresPdfsSettingsAndProfiles" to { fixture { db, storage, root ->
                val sourceContext = isolatedContext(root)
                val settings = SettingsRepository(sourceContext)
                settings.update(ReaderPreferences(palette = AppPalette.MINT_GREEN, collectionCards = false))
                LibraryRepository(db.books(), storage).importAll(listOf(pdf(root, "backup.pdf", "Backup content"))) { _, _ -> }
                val original = db.books().getAll().single()
                db.collections().insert(BookCollection("collection", "Study", "MINT_GREEN", "STUDY"))
                db.collections().add(CollectionBook("collection", original.id))
                db.profiles().insert(Profile("reader", "Test Reader"))
                db.profiles().record("reader", original.id, original.title, 42_000, 1234)
                settings.setProfile("reader")
                val archive = File(root, "backup.zip")
                RecoveryRepository(sourceContext, db, settings).backup(Uri.fromFile(archive), true) {}
                val destinationRoot = File(root, "other-device").apply { mkdirs() }
                val destinationDb = Room.inMemoryDatabaseBuilder(targetContext, ParchmentDatabase::class.java).build()
                try {
                    settings.update(ReaderPreferences(palette = AppPalette.ROSE))
                    val recovery = RecoveryRepository(isolatedContext(destinationRoot), destinationDb, settings)
                    recovery.restore(Uri.fromFile(archive), true) {}
                    val restored = destinationDb.books().getAll().single()
                    check(File(restored.filePath).readBytes().contentEquals(File(original.filePath).readBytes()))
                    check(File(restored.coverPath).isFile)
                    check(destinationDb.collections().getAll().single().icon == "STUDY")
                    check(destinationDb.collections().getMembers().size == 1)
                    check(destinationDb.profiles().getHistory("reader", original.id)!!.totalMillis == 42_000L)
                    check(settings.preferences.first().palette == AppPalette.MINT_GREEN)
                    check(settings.activeProfileId.first() == "reader")
                    recovery.restore(Uri.fromFile(archive), true) {}
                    check(destinationDb.books().getAll().size == 1)
                } finally { destinationDb.close() }
            } },
            "configRecoveryDoesNotInventPdfFiles" to { fixture { db, storage, root ->
                val settings = SettingsRepository(isolatedContext(root))
                LibraryRepository(db.books(), storage).importAll(listOf(pdf(root, "config.pdf", "Config only"))) { _, _ -> }
                val config = File(root, "config.json")
                RecoveryRepository(isolatedContext(root), db, settings).backup(Uri.fromFile(config), false) {}
                val targetRoot = File(root, "config-target").apply { mkdirs() }
                val targetDb = Room.inMemoryDatabaseBuilder(targetContext, ParchmentDatabase::class.java).build()
                try {
                    val message = RecoveryRepository(isolatedContext(targetRoot), targetDb, settings).restore(Uri.fromFile(config), false) {}
                    check(targetDb.books().getAll().size == 1 && !File(targetDb.books().getAll().single().filePath).exists())
                    check(message.contains("need their PDF archive"))
                    LibraryRepository(targetDb.books(), PdfStorage(isolatedContext(targetRoot))).importAll(listOf(pdf(root, "new.pdf", "Unrelated"))) { _, _ -> }
                    check(targetDb.books().getAll().size == 2)
                } finally { targetDb.close() }
            } },
            "recoveryRejectsUnsafeArchiveBeforeMutation" to { fixture { db, _, root ->
                val archive = File(root, "invalid.zip")
                ZipOutputStream(archive.outputStream()).use { zip ->
                    zip.putNextEntry(ZipEntry("../outside.pdf")); zip.write(byteArrayOf(1, 2, 3)); zip.closeEntry()
                }
                val settings = SettingsRepository(isolatedContext(root)); val before = settings.exportConfig().toString()
                check(runCatching { RecoveryRepository(isolatedContext(root), db, settings).restore(Uri.fromFile(archive), true) {} }.isFailure)
                check(db.books().getAll().isEmpty() && settings.exportConfig().toString() == before)
                check(!File(root.parentFile, "outside.pdf").exists())
            } },
            "selectedFolderImportReadDuplicateAndDelete" to { fixture { db, storage, root ->
                val repository = LibraryRepository(db.books(), storage)
                val uri = pdf(root, "external.pdf", "External PDF")
                val tree = DocumentsContract.buildTreeDocumentUri("org.hndrx.parchment.test.documents", "root")
                val result = repository.importAll(listOf(uri), directory = tree.toString()) { _, _ -> }
                check(result.imported == 1)
                val book = db.books().getAll().single()
                check(book.filePath.startsWith("content://"))
                check(File(root, "documents").listFiles()!!.isEmpty())
                val renderer = PdfPageRenderer(book.filePath, targetContext)
                try { check(renderer.pageCount == 1); renderer.render(0, 500).recycle() } finally { renderer.close() }
                val duplicate = repository.importAll(listOf(uri), directory = tree.toString()) { _, _ -> }
                check(duplicate.imported == 0 && duplicate.duplicates.size == 1)
                repository.delete(book)
                check(db.books().getAll().isEmpty())
                check(runCatching { targetContext.contentResolver.openInputStream(Uri.parse(book.filePath))!!.close() }.isFailure)
            } }
        )
        var failures = 0
        tests.forEachIndexed { index, (name, test) ->
            val status = Bundle().apply {
                putString("class", "org.hndrx.parchment.FeatureTestRunner"); putString("test", name)
                putInt("current", index + 1); putInt("numtests", tests.size)
            }
            sendStatus(1, status)
            try {
                runBlocking(Dispatchers.IO) { test() }
                status.putString("stream", "\nPASS: $name\n")
                sendStatus(0, status)
            } catch (error: Throwable) {
                failures++
                status.putString("stack", error.stackTraceToString())
                status.putString("stream", "\nFAIL: $name\n${error.stackTraceToString()}")
                sendStatus(-2, status)
            }
        }
        finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "\n${tests.size - failures}/${tests.size} passed; $failures failures\n") })
    }

    private fun isolatedContext(root: File): Context = object : ContextWrapper(targetContext) {
        override fun getFilesDir(): File = root
        override fun getApplicationContext(): Context = this
    }

    private suspend fun fixture(test: suspend (ParchmentDatabase, PdfStorage, File) -> Unit) {
        val root = File(targetContext.cacheDir, "import-test-${UUID.randomUUID()}").apply { mkdirs() }
        val db = Room.inMemoryDatabaseBuilder(targetContext, ParchmentDatabase::class.java).build()
        try { test(db, PdfStorage(isolatedContext(root)), root) }
        finally { db.close(); root.deleteRecursively() }
    }

    private fun pdf(root: File, name: String, text: String): Uri {
        val file = File(root, name)
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(100, 150, 1).create())
            page.canvas.drawText(text, 10f, 40f, android.graphics.Paint())
            document.finishPage(page)
            file.outputStream().use { document.writeTo(it) }
        } finally { document.close() }
        return Uri.fromFile(file)
    }

    private suspend fun migrationPreservesLibrary() {
        val name = "migration-test-${UUID.randomUUID()}.db"
        val file = targetContext.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE pdf_books (id TEXT NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL, description TEXT NOT NULL, category TEXT NOT NULL, tags TEXT NOT NULL, filePath TEXT NOT NULL, coverPath TEXT NOT NULL, customCoverPath TEXT, pageCount INTEGER NOT NULL, currentPage INTEGER NOT NULL, isFavorite INTEGER NOT NULL, addedAt INTEGER NOT NULL, lastOpenedAt INTEGER, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO pdf_books VALUES ('existing', 'Keep me', '', '', 'Unsorted', 'history', '/pdf', '/cover', NULL, 10, 4, 1, 1, NULL)")
            db.version = 1
        }
        val migrated = ParchmentDatabase.create(targetContext, name)
        try {
            val book = migrated.books().get("existing")!!
            check(book.title == "Keep me" && book.currentPage == 4 && book.isFavorite && book.tags == "history")
            migrated.collections().insert(BookCollection("new", "Migrated"))
            migrated.collections().add(CollectionBook("new", book.id))
            check(migrated.collections().observeMembers().first().size == 1)
            check(migrated.collections().getAll().single().color == "PARCHMENT")
            check(migrated.profiles().getAll().any { it.id == "default" })
        } finally { migrated.close(); targetContext.deleteDatabase(name) }
    }
}
