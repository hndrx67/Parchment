package org.hndrx.parchment.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.*
import org.hndrx.parchment.settings.SettingsRepository
import org.hndrx.parchment.pdf.PdfPageRenderer
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Versioned, experimental recovery format. Archive names never become filesystem paths. */
class RecoveryRepository(private val context: Context, private val db: ParchmentDatabase, private val settings: SettingsRepository) {
    private fun json(vararg fields: Pair<String, Any?>) = JSONObject().apply { fields.forEach { put(it.first, it.second ?: JSONObject.NULL) } }
    private fun array(values: List<JSONObject>) = JSONArray(values)
    private suspend fun snapshot(): JSONObject = db.withTransaction {
        json("format" to "parchment-backup", "schema" to 1, "settings" to settings.exportConfig(),
            "books" to array(db.books().getAll().map { b -> json("id" to b.id, "title" to b.title, "author" to b.author,
                "description" to b.description, "category" to b.category, "tags" to b.tags, "filePath" to b.filePath,
                "coverPath" to b.coverPath, "customCoverPath" to b.customCoverPath, "pageCount" to b.pageCount,
                "currentPage" to b.currentPage, "isFavorite" to b.isFavorite, "addedAt" to b.addedAt, "lastOpenedAt" to b.lastOpenedAt) }),
            "collections" to array(db.collections().getAll().map { json("id" to it.id, "name" to it.name, "color" to it.color, "icon" to it.icon) }),
            "members" to array(db.collections().getMembers().map { json("collectionId" to it.collectionId, "bookId" to it.bookId) }),
            "profiles" to array(db.profiles().getAll().map { json("id" to it.id, "username" to it.username, "avatarPath" to it.avatarPath) }),
            "history" to array(db.profiles().getHistory().map { json("profileId" to it.profileId, "bookId" to it.bookId, "title" to it.title, "totalMillis" to it.totalMillis, "lastReadAt" to it.lastReadAt) }))
    }
    private fun objects(root: JSONObject, key: String): List<JSONObject> {
        val items = root.getJSONArray(key)
        require(items.length() <= 50_000) { "Backup contains too many entries" }
        return (0 until items.length()).map { items.getJSONObject(it) }
    }
    private fun open(path: String): InputStream = if (path.startsWith("content://")) {
        requireNotNull(context.contentResolver.openInputStream(Uri.parse(path))) { "A stored file is unavailable" }
    } else File(path).inputStream()
    private suspend fun copy(input: InputStream, output: OutputStream, limit: Long = Long.MAX_VALUE) {
        val buffer = ByteArray(64 * 1024); var total = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer); if (count < 0) break
            total += count; require(total <= limit) { "Backup entry exceeds the supported size" }
            output.write(buffer, 0, count)
        }
    }
    suspend fun backup(destination: Uri, includePdfs: Boolean, progress: (String) -> Unit) = withContext(Dispatchers.IO) {
        progress("Preparing backup…")
        val config = snapshot().put("includesPdfs", includePdfs)
        val configBytes = config.toString().toByteArray(Charsets.UTF_8)
        require(configBytes.size <= 10 * 1024 * 1024) { "Configuration is too large for this experimental backup format" }
        requireNotNull(context.contentResolver.openOutputStream(destination, "wt")).use { output ->
            if (!includePdfs) output.write(configBytes)
            else ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("config.json")); zip.write(configBytes); zip.closeEntry()
                suspend fun add(name: String, path: String) {
                    zip.putNextEntry(ZipEntry(name)); open(path).use { copy(it, zip) }; zip.closeEntry()
                }
                objects(config, "books").forEachIndexed { index, b ->
                    progress("Backing up PDF ${index + 1} of ${config.getJSONArray("books").length()}")
                    add("pdfs/$index.pdf", b.getString("filePath"))
                    b.optString("coverPath").takeIf { File(it).isFile }?.let { add("covers/$index.png", it) }
                    if (!b.isNull("customCoverPath")) b.getString("customCoverPath").takeIf { File(it).isFile }?.let { add("custom/$index.bin", it) }
                }
                objects(config, "profiles").forEachIndexed { index, p ->
                    if (!p.isNull("avatarPath")) p.getString("avatarPath").takeIf { File(it).isFile }?.let { add("avatars/$index.bin", it) }
                }
            }
        }
        progress("Backup complete")
    }
    private fun safeStoredPath(path: String?): String? {
        if (path.isNullOrBlank() || path.startsWith("content://")) return null
        val file = File(path).canonicalFile
        return listOf("documents", "covers", "restored").map { File(context.filesDir, it).canonicalFile }.firstOrNull {
            file.toPath().startsWith(it.toPath()) && file != it && file.isFile
        }?.let { file.path }
    }
    private fun validate(root: JSONObject) {
        require(root.getString("format") == "parchment-backup" && root.getInt("schema") == 1) { "Not a supported Parchment backup" }
        root.getJSONObject("settings")
        val books = objects(root, "books"); val collections = objects(root, "collections"); val profiles = objects(root, "profiles")
        listOf(books, collections, profiles).forEach { list ->
            val ids = list.map { it.getString("id") }
            require(ids.distinct().size == ids.size && ids.all { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }) { "Invalid or duplicate IDs in backup" }
        }
        val bookIds = books.map { it.getString("id") }.toSet(); val collectionIds = collections.map { it.getString("id") }.toSet()
        objects(root, "members").forEach { require(it.getString("bookId") in bookIds && it.getString("collectionId") in collectionIds) { "Invalid collection membership" } }
        books.forEach { require(it.getInt("pageCount") > 0) { "Invalid page count" }; it.getString("title") }
        collections.forEach { require(it.getString("name").isNotBlank() && it.getString("name").length <= 80) }
        profiles.forEach { require(it.getString("username").isNotBlank() && it.getString("username").length <= 80) }
        val profileIds = profiles.map { it.getString("id") }.toSet()
        objects(root, "history").forEach { require(it.getString("profileId") in profileIds && it.getLong("totalMillis") >= 0) { "Invalid reading history" } }
    }
    suspend fun restore(source: Uri, archive: Boolean, progress: (String) -> Unit): String = withContext(Dispatchers.IO) {
        val folder = File(context.filesDir, "restored/${UUID.randomUUID()}").apply { mkdirs() }
        val assets = mutableMapOf<String, File>()
        var committed = false
        try {
            progress("Validating backup…")
            val config = requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
                if (!archive) ByteArrayOutputStream().use { buffer -> copy(input, buffer, 10L * 1024 * 1024); JSONObject(buffer.toString("UTF-8")) }
                else {
                    var root: JSONObject? = null
                    val names = mutableSetOf<String>()
                    ZipInputStream(input).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            require(names.add(entry.name) && names.size <= 200_001) { "Duplicate or excessive archive entries" }
                            if (entry.name == "config.json") {
                                root = ByteArrayOutputStream().use { buffer -> copy(zip, buffer, 10L * 1024 * 1024); JSONObject(buffer.toString("UTF-8")) }
                            } else {
                                require(entry.name.matches(Regex("(pdfs/[0-9]+\\.pdf|covers/[0-9]+\\.png|custom/[0-9]+\\.bin|avatars/[0-9]+\\.bin)"))) { "Unexpected archive entry" }
                                val target = File(folder, UUID.randomUUID().toString())
                                target.outputStream().use { copy(zip, it, 2L * 1024 * 1024 * 1024) }
                                assets[entry.name] = target
                            }
                            zip.closeEntry()
                        }
                    }
                    requireNotNull(root) { "Archive has no configuration" }
                }
            }
            validate(config)
            if (archive) require(config.optBoolean("includesPdfs")) { "This archive has no PDF backup" }
            val allowedAssets = objects(config, "books").indices.flatMap { listOf("pdfs/$it.pdf", "covers/$it.png", "custom/$it.bin") }.toSet() +
                objects(config, "profiles").indices.map { "avatars/$it.bin" }
            require(assets.keys.all { it in allowedAssets }) { "Archive contains unrecognized assets" }
            val existing = db.books().getAll().associateBy { it.id }
            var unavailable = 0
            val books = objects(config, "books").mapIndexed { index, b ->
                val id = b.getString("id"); val old = existing[id]
                val file = if (archive) {
                    val asset = requireNotNull(assets["pdfs/$index.pdf"]) { "Archive is missing PDF ${index + 1}" }
                    progress("Checking PDF ${index + 1}")
                    PdfPageRenderer(asset.path).close()
                    asset.path
                } else old?.filePath ?: safeStoredPath(b.optString("filePath")) ?: File(folder, "missing-$index.pdf").path.also { unavailable++ }
                PdfBook(id, b.getString("title"), b.optString("author"), b.optString("description"), b.optString("category", "Unsorted"), b.optString("tags"),
                    file, assets["covers/$index.png"]?.path ?: old?.coverPath ?: safeStoredPath(b.optString("coverPath")) ?: "",
                    assets["custom/$index.bin"]?.path ?: old?.customCoverPath ?: safeStoredPath(b.optString("customCoverPath")),
                    b.getInt("pageCount"), b.optInt("currentPage").coerceIn(0, b.getInt("pageCount") - 1), b.optBoolean("isFavorite"),
                    b.optLong("addedAt", System.currentTimeMillis()), if (b.isNull("lastOpenedAt")) null else b.getLong("lastOpenedAt"))
            }
            val profiles = objects(config, "profiles").mapIndexed { index, p ->
                Profile(p.getString("id"), p.getString("username"), assets["avatars/$index.bin"]?.path ?: safeStoredPath(p.optString("avatarPath")))
            }
            val restoredSettings = config.getJSONObject("settings")
            val storage = restoredSettings.optString("storage_directory")
            if (context.contentResolver.persistedUriPermissions.none { it.uri.toString() == storage && it.isWritePermission }) restoredSettings.put("storage_directory", "")
            val previousSettings = settings.exportConfig()
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                try {
                    db.withTransaction {
                        db.books().restoreAll(books)
                        val collectionIds = mutableMapOf<String, String>()
                        objects(config, "collections").forEach { c ->
                            val name = c.getString("name")
                            val id = db.collections().find(name)?.id ?: c.getString("id")
                            collectionIds[c.getString("id")] = id
                            val value = BookCollection(id, name, c.optString("color", "PARCHMENT"), c.optString("icon", "FOLDER"))
                            db.collections().insert(value); db.collections().update(value)
                        }
                        db.collections().addAll(objects(config, "members").map { CollectionBook(requireNotNull(collectionIds[it.getString("collectionId")]), it.getString("bookId")) })
                        profiles.forEach { db.profiles().save(it) }
                        db.profiles().insert(Profile())
                        objects(config, "history").forEach { h -> db.profiles().saveHistory(ReadingHistory(h.getString("profileId"), h.getString("bookId"), h.getString("title"), h.getLong("totalMillis"), h.getLong("lastReadAt"))) }
                        settings.restoreConfig(restoredSettings)
                    }
                    committed = true
                } catch (error: Throwable) { settings.restoreConfig(previousSettings); throw error }
            }
            "Recovered ${books.size} library entries." + if (unavailable > 0) " $unavailable need their PDF archive restored before reading." else ""
        } finally { if (!committed) folder.deleteRecursively() }
    }
}
