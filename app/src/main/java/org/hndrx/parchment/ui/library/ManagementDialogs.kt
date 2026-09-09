package org.hndrx.parchment.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.ui.LibraryViewModel

@Composable
fun ManagementWindow(count: Int, enabled: Boolean, onSelectMore: () -> Unit, onExit: () -> Unit, action: (String) -> Unit) {
    AlertDialog(onDismissRequest = onSelectMore, title = { Text("Manage $count selected") },
        text = { Column {
            Text("Choose an action, or select more PDFs.")
            listOf("collections" to "Add to collection", "tags" to "Assign tags", "adult" to "Tag as Adult Content",
                "favorite" to "Add favorites", "unfavorite" to "Remove favorites", "delete" to "Delete PDFs").forEach { (key, label) ->
                TextButton(enabled = enabled && count > 0, onClick = { action(key) }) { Text(label) }
            }
        } },
        confirmButton = { TextButton(onSelectMore) { Text("Select more") } },
        dismissButton = { TextButton(onExit) { Text("Exit mode") } })
}

@Composable
fun BookActionsDialog(book: PdfBook, viewModel: LibraryViewModel, onDismiss: () -> Unit, edit: () -> Unit, collect: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = { Column {
            TextButton(collect) { Text("Manage collections") }
            TextButton({ viewModel.setFavorites(setOf(book.id), !book.isFavorite); onDismiss() }) { Text(if (book.isFavorite) "Remove favorite" else "Add favorite") }
            TextButton(edit) { Text("Edit details") }
            TextButton({ deleting = true }) { Text("Delete") }
        } },
        confirmButton = { TextButton(onDismiss) { Text("Close") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Remove document?") },
        text = { Text("This removes the entry and its stored PDF copy.") },
        confirmButton = { TextButton({ viewModel.deleteBooks(setOf(book.id)); onDismiss() }) { Text("Remove") } },
        dismissButton = { TextButton({ deleting = false }) { Text("Cancel") } })
}

@Composable
fun AssignTagsDialog(onDismiss: () -> Unit, assign: (List<String>) -> Unit) {
    var tags by remember { mutableStateOf("") }
    var adult by remember { mutableStateOf(false) }
    val values = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() } + if (adult) listOf("Adult Content") else emptyList()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Assign tags") },
        text = { Column {
            OutlinedTextField(tags, { tags = it }, label = { Text("Tags, separated by commas") }, supportingText = { Text("Adds tags to every selected PDF and keeps existing tags.") })
            Row { Checkbox(adult, { adult = it }); Text("Adult Content", Modifier.padding(top = 12.dp)) }
        } },
        confirmButton = { TextButton(enabled = values.isNotEmpty(), onClick = { assign(values) }) { Text("Assign") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
fun BulkCollectionDialog(viewModel: LibraryViewModel, ids: Set<String>, onDismiss: () -> Unit) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add ${ids.size} PDFs to a collection") },
        text = { Column {
            LazyColumn(Modifier.heightIn(max = 240.dp)) {
                items(collections, key = { it.id }) { collection ->
                    TextButton({ viewModel.addBooksToCollection(ids, collection.id); onDismiss() }) { Text(collection.name) }
                }
            }
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("New collection name") }, singleLine = true)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { viewModel.createCollectionForBooks(name, ids); onDismiss() }) { Text("Create and add") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}
