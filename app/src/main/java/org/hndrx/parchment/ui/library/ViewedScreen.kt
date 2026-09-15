package org.hndrx.parchment.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewedScreen(viewModel: LibraryViewModel, back: () -> Unit, openBook: (String) -> Unit) {
    val books by viewModel.viewedBooks.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<PdfBook?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Viewed") }, navigationIcon = {
        IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("PDFs opened from other apps stay here until you import them into your library or remove them.") }
            if (books.isEmpty()) item { Text("No viewed PDFs yet.") }
            items(books, key = { it.id }) { book ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium)
                        Text("${book.pageCount} pages", style = MaterialTheme.typography.bodySmall)
                        TextButton({ openBook(book.id) }) { Text("Open PDF") }
                        TextButton(enabled = !state.managing, onClick = { viewModel.importViewed(book.id) }) { Text("Import to library") }
                        TextButton(enabled = !state.managing, onClick = { removing = book }) { Text("Remove") }
                    }
                }
            }
        }
    }
    removing?.let { book ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove viewed PDF?") },
            text = { Text("Remove ${book.title} and its saved copy from Parchment? The original file is unaffected.") },
            confirmButton = { TextButton({ viewModel.deleteBooks(setOf(book.id)); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton({ removing = null }) { Text("Cancel") } })
    }
}
