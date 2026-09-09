package org.hndrx.parchment.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable
fun StorageSettings(directory: String, change: (String) -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            checking = true
            try {
                withContext(Dispatchers.IO) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val parent = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                val writable = context.contentResolver.query(parent, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use {
                    it.moveToFirst() && it.getInt(0) and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0
                } ?: false
                require(writable) { "Choose a folder that allows new files." }
                }
                change(uri.toString())
            } catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) { error = failure.localizedMessage ?: "Unable to access this folder." }
            finally { checking = false }
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PDF import location", style = MaterialTheme.typography.titleMedium)
        Text(if (directory.isEmpty()) "App storage" else "Selected folder", style = MaterialTheme.typography.labelLarge)
        Text(if (directory.isEmpty()) File(context.filesDir, "documents").absolutePath else {
            runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(directory)) }.getOrDefault(directory)
        }, style = MaterialTheme.typography.bodySmall)
        Text("New imports are saved here. Existing PDFs remain in their original locations. Covers and library metadata stay in app storage.", style = MaterialTheme.typography.bodySmall)
        if (directory.isNotEmpty()) Text("Default app folder: ${File(context.filesDir, "documents").absolutePath}", style = MaterialTheme.typography.bodySmall)
        TextButton(enabled = !checking, onClick = { picker.launch(directory.takeIf { it.isNotEmpty() }?.let(Uri::parse)) }) { Text(if (checking) "Checking folder…" else "Choose folder") }
        if (directory.isNotEmpty()) TextButton({ change("") }) { Text("Use app storage") }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Folder unavailable") },
        text = { Text(message) }, confirmButton = { TextButton({ error = null }) { Text("OK") } }) }
}
