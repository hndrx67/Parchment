package org.hndrx.parchment.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.data.BookCollection
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.ui.LibraryViewModel
import org.hndrx.parchment.ui.settings.CollectionEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(viewModel: LibraryViewModel, back: () -> Unit, openCollection: (String) -> Unit, customize: () -> Unit = {}) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.collectionMembers.collectAsStateWithLifecycle()
    val prefs by viewModel.readerPreferences.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<BookCollection?>(null) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Collections") }, navigationIcon = {
            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = { IconButton(customize) { Icon(Icons.Default.Palette, "Customize collections") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New collection") }) }
    ) { padding ->
        if (collections.isEmpty()) Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Create a collection, then long-press a document in your library to add it.")
        } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(collections, key = { it.id }) { collection ->
                CollectionEntry(collection, members.count { it.collectionId == collection.id }, prefs.collectionCards, { openCollection(collection.id) }) {
                    IconButton({ deleting = collection }) { Icon(Icons.Default.Delete, "Delete collection") }
                }
            }
        }
    }
    if (creating) NewCollectionDialog(onDismiss = { creating = false }, create = { viewModel.createCollection(it); creating = false })
    deleting?.let { collection ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete collection?") },
            text = { Text("Delete “${collection.name}”? Its PDFs will remain in your library.") },
            confirmButton = { TextButton({ viewModel.deleteCollection(collection); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } })
    }
}

@Composable
fun CollectionPickerDialog(viewModel: LibraryViewModel, book: PdfBook, onDismiss: () -> Unit) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.collectionMembers.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collections for ${book.title}") },
        text = {
            Column {
                if (collections.isEmpty()) Text("Create your first collection below.")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(collections, key = { it.id }) { collection ->
                        val selected = members.any { it.collectionId == collection.id && it.bookId == book.id }
                        val toggle = {
                            if (selected) viewModel.removeFromCollection(collection.id, book.id)
                            else viewModel.addToCollection(collection.id, book.id)
                            Unit
                        }
                        Row(Modifier.fillMaxWidth().clickable(onClick = toggle), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(selected, onCheckedChange = { toggle() })
                            Text(collection.name)
                        }
                    }
                }
                TextButton({ creating = true }) { Text("New collection") }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Done") } }
    )
    if (creating) NewCollectionDialog(onDismiss = { creating = false }, create = {
        viewModel.createCollection(it, book.id); creating = false
    })
}

@Composable
private fun NewCollectionDialog(onDismiss: () -> Unit, create: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New collection") },
        text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Collection name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { create(name.trim()) }) { Text("Create") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
