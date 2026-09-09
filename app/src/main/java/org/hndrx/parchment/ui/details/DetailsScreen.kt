package org.hndrx.parchment.ui.details

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(id: String, viewModel: LibraryViewModel, back: () -> Unit) {
    val book by viewModel.observeBook(id).collectAsStateWithLifecycle(null)
    val current = book
    if (current == null) { Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }; return }
    var draft by remember(current.id) { mutableStateOf(current) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.collectionMembers.collectAsStateWithLifecycle()
    val names = collections.filter { collection -> members.any { it.bookId == id && it.collectionId == collection.id } }.joinToString { it.name }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setCover(id, it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Document details") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(enabled = !state.managing, onClick = { viewModel.saveDetails(draft); back() }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text("Save changes") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(File(current.displayedCover), "Current PDF cover", Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit)
            OutlinedButton(enabled = !state.managing, onClick = { coverPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose custom cover") }
            if (current.customCoverPath != null) TextButton(enabled = !state.managing, onClick = { viewModel.revertCover(id) }) { Text("Revert to original cover") }
            if (state.managing) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(draft.title, { draft = draft.copy(title = it) }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(draft.author, { draft = draft.copy(author = it) }, Modifier.fillMaxWidth(), label = { Text("Author") }, singleLine = true)
            if (names.isNotBlank()) Text("Collections: $names", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(draft.category, { draft = draft.copy(category = it) }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
            OutlinedTextField(draft.tags, { draft = draft.copy(tags = it) }, Modifier.fillMaxWidth(), label = { Text("Tags") }, supportingText = { Text("Separate tags with commas") })
            OutlinedTextField(draft.description, { draft = draft.copy(description = it) }, Modifier.fillMaxWidth().heightIn(min = 110.dp), label = { Text("Notes") }, minLines = 3)
            Text("${current.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
