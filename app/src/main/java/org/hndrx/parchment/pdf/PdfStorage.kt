package org.hndrx.parchment.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import org.hndrx.parchment.data.PdfBook
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class PdfStorage(private val context: Context) {
    private val documents = File(context.filesDir, "documents").apply { mkdirs() }
    private val covers = File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun importPdf(uri: Uri): PdfBook {
        val id = UUID.randomUUID().toString()
        val displayName = queryName(uri) ?: "Untitled.pdf"
        val pdfFile = File(documents, "$id.pdf")
        val coverFile = File(covers, "$id.png")
        try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open the selected PDF" }
            pdfFile.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val size = input.read(buffer)
                    if (size < 0) break
                    output.write(buffer, 0, size)
                }
            }
        }
        currentCoroutineContext().ensureActive()
        val pageCount = renderCover(pdfFile, coverFile)
        currentCoroutineContext().ensureActive()
        return PdfBook(
            id = id,
            title = displayName.substringBeforeLast('.').ifBlank { "Untitled" },
            filePath = pdfFile.absolutePath,
            coverPath = coverFile.absolutePath,
            pageCount = pageCount
        )
        } catch (error: Throwable) {
            pdfFile.delete()
            coverFile.delete()
            throw error
        }
    }

    suspend fun fingerprint(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        openInput(path).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun copyCover(id: String, uri: Uri): String {
        val file = File(covers, "${id}_custom_${UUID.randomUUID()}")
        try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            file.outputStream().use(input::copyTo)
        }
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth > 0 && options.outHeight > 0) { "Select a supported image for the cover." }
        return file.absolutePath
        } catch (error: Throwable) { file.delete(); throw error }
    }

    fun deleteCover(path: String?) { path?.let { File(it).delete() } }

    fun deleteFiles(book: PdfBook) {
        if (book.filePath.startsWith("content://")) {
            check(DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(book.filePath))) { "Could not delete the stored PDF" }
        } else File(book.filePath).delete()
        listOfNotNull(book.coverPath, book.customCoverPath).forEach { File(it).delete() }
    }

    private fun openInput(path: String) = if (path.startsWith("content://")) {
        requireNotNull(context.contentResolver.openInputStream(Uri.parse(path))) { "The stored PDF is unavailable. Check its folder permission." }
    } else File(path).inputStream()

    suspend fun moveToDirectory(book: PdfBook, tree: Uri): PdfBook {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val document = requireNotNull(DocumentsContract.createDocument(context.contentResolver, parent, "application/pdf", "${book.title.take(80).replace('/', '_')}-${book.id}.pdf")) {
            "Unable to create a PDF in the selected folder"
        }
        try {
            openInput(book.filePath).use { input ->
                requireNotNull(context.contentResolver.openOutputStream(document, "w")).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val size = input.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            File(book.filePath).delete()
            return book.copy(filePath = document.toString())
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, document) }
            throw error
        }
    }

    private fun renderCover(pdf: File, output: File): Int {
        val descriptor = android.os.ParcelFileDescriptor.open(pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { renderer ->
            require(renderer.pageCount > 0) { "This PDF has no pages" }
            renderer.openPage(0).use { page ->
                val width = 720
                val height = (width * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
                bitmap.recycle()
            }
            return renderer.pageCount
        }
    }

    private fun queryName(uri: Uri): String? = context.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}
