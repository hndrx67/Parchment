package org.hndrx.parchment.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(viewModel: LibraryViewModel, back: () -> Unit) {
    val state by viewModel.recoveryState.collectAsStateWithLifecycle()
    var restore by remember { mutableStateOf<Pair<Uri, Boolean>?>(null) }
    val configBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { viewModel.backup(it, false) } }
    val pdfBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let { viewModel.backup(it, true) } }
    val configRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { restore = it to false } }
    val pdfRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { restore = it to true } }
    BackHandler(state.busy) {}
    Scaffold(topBar = { TopAppBar(title = { Text("Recovery · Experimental") }, navigationIcon = { IconButton(enabled = !state.busy, onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Experimental feature", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Keep your original PDFs and backups until you have checked the recovered library.")
            Text("Configuration", style = MaterialTheme.typography.titleLarge)
            Text("A JSON file containing your imported-PDF list, settings, collections, profiles, and reading history. It does not contain PDF files or profile pictures.")
            Button(enabled = !state.busy, onClick = { configBackup.launch("Parchment-config-${System.currentTimeMillis()}.json") }) { Text("Back up configuration") }
            OutlinedButton(enabled = !state.busy, onClick = { configRestore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Recover configuration") }
            HorizontalDivider()
            Text("Imported PDF files", style = MaterialTheme.typography.titleLarge)
            Text("A ZIP archive with your PDFs, covers, profile pictures, and configuration. Recovered PDFs are saved in app storage.")
            Button(enabled = !state.busy, onClick = { pdfBackup.launch("Parchment-PDFs-${System.currentTimeMillis()}.zip") }) { Text("Back up PDFs and configuration") }
            OutlinedButton(enabled = !state.busy, onClick = { pdfRestore.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Recover PDF archive") }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.message.isNotBlank()) Text(state.message, style = MaterialTheme.typography.bodyLarge)
        }
    }
    restore?.let { (uri, archive) -> AlertDialog(onDismissRequest = { restore = null }, title = { Text("Recover this backup?") },
        text = { Text("Library entries will be merged, and saved settings will be replaced by the backup. Existing PDF files are kept." + if (!archive) " On another device, recover the PDF archive too before opening the restored entries." else "") },
        confirmButton = { TextButton({ restore = null; viewModel.restore(uri, archive) }) { Text("Recover") } },
        dismissButton = { TextButton({ restore = null }) { Text("Cancel") } }) }
}
