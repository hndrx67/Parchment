package org.hndrx.parchment.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfPageRenderer(filePath: String, context: Context? = null) : AutoCloseable {
    private val cachedPdf = if (filePath.startsWith("content://")) {
        val owner = requireNotNull(context)
        File.createTempFile("reader-", ".pdf", owner.cacheDir).also { file ->
            try {
                requireNotNull(owner.contentResolver.openInputStream(Uri.parse(filePath))).use { input -> file.outputStream().use(input::copyTo) }
            } catch (error: Throwable) { file.delete(); throw error }
        }
    } else null
    private val descriptor = ParcelFileDescriptor.open(cachedPdf ?: File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try { PdfRenderer(descriptor) } catch (error: Throwable) {
        descriptor.close(); cachedPdf?.delete(); throw error
    }
    private var closed = false
    val pageCount get() = renderer.pageCount

    suspend fun render(pageIndex: Int, targetWidth: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(renderer) {
            check(!closed) { "Reader is closed" }
            renderer.openPage(pageIndex).use { page ->
                val width = targetWidth.coerceIn(480, 1800)
                val height = (width * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }

    override fun close() {
        synchronized(renderer) {
            if (!closed) {
                closed = true
                renderer.close()
                descriptor.close()
                cachedPdf?.delete()
            }
        }
    }
}
