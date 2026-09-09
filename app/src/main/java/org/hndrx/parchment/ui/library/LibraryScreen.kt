package org.hndrx.parchment.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.ui.BookSort
import org.hndrx.parchment.ui.LibraryViewModel
import org.hndrx.parchment.ui.hideCover
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import java.io.File
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import org.hndrx.parchment.ui.profile.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel, openBook: (String) -> Unit, editBook: (String) -> Unit, openSettings: () -> Unit,
    openCollections: () -> Unit, openProfile: () -> Unit = {}, collectionId: String? = null, back: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.collectionMembers.collectAsStateWithLifecycle()
    val memberIds = members.filter { it.collectionId == collectionId }.map { it.bookId }.toSet()
    val books = if (collectionId == null) state.books else state.books.filter { it.id in memberIds }
    val collectionLabels = remember(collections, members) {
        val names = collections.associate { it.id to it.name }
        members.groupBy { it.bookId }.mapValues { (_, links) -> links.mapNotNull { names[it.collectionId] }.sorted().joinToString(", ") }
    }
    var collectionBook by remember { mutableStateOf<PdfBook?>(null) }
    var management by rememberSaveable(collectionId) { mutableStateOf(false) }
    var managementWindow by rememberSaveable(collectionId) { mutableStateOf(false) }
    var selection by rememberSaveable(collectionId) { mutableStateOf(listOf<String>()) }
    var actionBook by remember { mutableStateOf<PdfBook?>(null) }
    var batchAction by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(books) { selection = selection.filter { id -> books.any { it.id == id } } }
    fun toggleSelection(id: String) { selection = if (id in selection) selection - id else selection + id }
    fun finishManagement() { management = false; managementWindow = false; selection = emptyList() }
    fun startManagement(id: String) {
        management = true
        if (id !in selection) selection = selection + id
        managementWindow = true
    }
    BackHandler(management) { finishManagement() }
    var tagsOpen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        viewModel.importAll(it)
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (management) IconButton({ finishManagement() }) { Icon(Icons.Default.Close, "Exit Management Mode") }
                    else if (collectionId != null) IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") }
                    Text(if (management) "${selection.size} selected" else if (collectionId == null) "Parchment" else collections.find { it.id == collectionId }?.name ?: "Collection",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(openProfile) { ProfileAvatar(profile, Modifier.size(28.dp)) }
                    IconButton(viewModel::toggleFavorites) {
                        Icon(if (state.favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorites")
                    }
                    IconButton(viewModel::toggleLayout) {
                        Icon(if (state.gridMode) Icons.Default.ViewList else Icons.Default.GridView, "Change layout")
                    }
                    IconButton(openSettings) { Icon(Icons.Default.Settings, "Settings") }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.Sort, "Sort") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text("Recently added") }, onClick = { viewModel.setSort(BookSort.RECENTLY_ADDED); menu = false })
                            DropdownMenuItem({ Text("Title") }, onClick = { viewModel.setSort(BookSort.TITLE); menu = false })
                            DropdownMenuItem({ Text("Last opened") }, onClick = { viewModel.setSort(BookSort.LAST_OPENED); menu = false })
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!management) {
                    if (collectionId == null) TextButton(openCollections) { Icon(Icons.Default.Folder, null); Spacer(Modifier.width(6.dp)); Text("Collections") }
                    TextButton({ tagsOpen = true }) {
                        Icon(Icons.Default.Label, null); Spacer(Modifier.width(6.dp))
                        Text(if (state.selectedTags.isEmpty()) "Filter tags" else "Tags (${state.selectedTags.size})")
                    }
                    } else {
                        TextButton({ selection = if (selection.size == books.size) emptyList() else books.map { it.id } }) { Text(if (selection.size == books.size && books.isNotEmpty()) "Clear selection" else "Select all") }
                        Text("Tap entries to select more", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.managing) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search your library") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (state.query.isNotEmpty()) {{ IconButton({ viewModel.setQuery("") }) { Icon(Icons.Default.Close, "Clear") } }} else null
                )
            }
        },
        bottomBar = { if (management) Surface(shadowElevation = 4.dp) {
            Button(enabled = selection.isNotEmpty() && !state.managing, onClick = { managementWindow = true },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text("Manage ${selection.size} selected") }
        } },
        floatingActionButton = { if (collectionId == null && !management) {
            ExtendedFloatingActionButton(
                onClick = { picker.launch(arrayOf("application/pdf")) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Import PDF") }
            )
        } }
    ) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                books.isEmpty() && collectionId != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("No documents in this view", style = MaterialTheme.typography.titleLarge)
                    Text("Long-press a document in your library to add it to a collection. Check active filters if documents are missing.")
                }
                books.isEmpty() -> EmptyLibrary(filtered = state.query.isNotBlank() || state.favoritesOnly || state.selectedTags.isNotEmpty(), importPdf = { picker.launch(arrayOf("application/pdf")) })
                state.gridMode -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(preferences.gridSize.dp),
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) { items(books, key = { it.id }) { book -> BookCard(book,
                    { if (management) toggleSelection(book.id) else openBook(book.id) },
                    { startManagement(book.id) }, { actionBook = book },
                    management, book.id in selection, preferences.publicMode, state.refreshVersion,
                    collectionLabels[book.id]?.takeIf { it.isNotBlank() } ?: book.category.ifBlank { "Unsorted" })
                } }
                else -> LazyColumn(contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(books, key = { it.id }) { book -> BookRow(book,
                        { if (management) toggleSelection(book.id) else openBook(book.id) },
                        { startManagement(book.id) }, { actionBook = book },
                        management, book.id in selection, preferences.publicMode, state.refreshVersion,
                        collectionLabels[book.id]?.takeIf { it.isNotBlank() } ?: book.category.ifBlank { "Unsorted" })
                    }
                }
            }
        }
    }
    collectionBook?.let { book -> CollectionPickerDialog(viewModel, book, onDismiss = { collectionBook = null }) }
    if (managementWindow) ManagementWindow(selection.size, enabled = !state.managing, onSelectMore = { managementWindow = false }, onExit = { finishManagement() }) { action ->
        managementWindow = false
        when (action) {
            "adult" -> viewModel.addTags(selection.toSet(), listOf("Adult Content"))
            "favorite" -> viewModel.setFavorites(selection.toSet(), true)
            "unfavorite" -> viewModel.setFavorites(selection.toSet(), false)
            else -> batchAction = action
        }
    }
    actionBook?.let { book -> BookActionsDialog(book, viewModel, onDismiss = { actionBook = null },
        edit = { actionBook = null; editBook(book.id) }, collect = { actionBook = null; collectionBook = book }) }
    if (batchAction == "tags") AssignTagsDialog(onDismiss = { batchAction = null }, assign = { tags -> viewModel.addTags(selection.toSet(), tags); batchAction = null })
    if (batchAction == "collections") BulkCollectionDialog(viewModel, selection.toSet(), onDismiss = { batchAction = null })
    if (batchAction == "delete") AlertDialog(onDismissRequest = { batchAction = null }, title = { Text("Delete ${selection.size} PDFs?") },
        text = { Text("This removes the selected entries and their stored PDF copies.") },
        confirmButton = { TextButton({ viewModel.deleteBooks(selection.toSet()); batchAction = null; selection = emptyList() }) { Text("Delete") } },
        dismissButton = { TextButton({ batchAction = null }) { Text("Cancel") } })
    if (tagsOpen) AlertDialog(
        onDismissRequest = { tagsOpen = false }, title = { Text("Filter tags") },
        text = {
            Column {
                Text("Show documents matching any selected tag.")
                if (state.availableTags.isEmpty()) Text("Add comma-separated tags in a document’s details.")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items((state.availableTags + state.selectedTags).distinct().sorted()) { tag ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.toggleTag(tag) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(tag in state.selectedTags, { viewModel.toggleTag(tag) }); Text(tag)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ tagsOpen = false }) { Text("Done") } },
        dismissButton = { TextButton(viewModel::clearTags) { Text("Clear filters") } }
    )
    if (state.busy) Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (preferences.animationsEnabled) CircularProgressIndicator()
                else LinearProgressIndicator(progress = { state.importCompleted.toFloat() / state.importTotal.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Text("Importing Files", style = MaterialTheme.typography.titleLarge)
                Text("${state.importCompleted} of ${state.importTotal} checked")
                Text("Checking for duplicates before adding files.", style = MaterialTheme.typography.bodySmall)
                TextButton(viewModel::abortImport) { Text("Abort") }
            }
        }
    }
    if (state.duplicates.isNotEmpty()) AlertDialog(
        onDismissRequest = viewModel::clearDuplicates,
        title = { Text("Duplicates skipped") },
        text = { Column { Text("${state.importedCount} new PDFs imported. These duplicates were skipped:"); LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(state.duplicates) { Text(it, Modifier.padding(vertical = 8.dp)) }
        } } },
        confirmButton = { TextButton(viewModel::clearDuplicates) { Text("OK") } }
    )
}

@Composable
private fun EmptyLibrary(filtered: Boolean, importPdf: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.MenuBook, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        Text(if (filtered) "No matching documents" else "Your library is empty", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(if (filtered) "Try another search or filter." else "Import a PDF to create your first cover.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!filtered) { Spacer(Modifier.height(20.dp)); Button(importPdf) { Text("Choose PDF") } }
    }
}

@Composable
private fun BookCard(book: PdfBook, open: () -> Unit, hold: () -> Unit, menu: () -> Unit, management: Boolean, selected: Boolean, publicMode: Boolean, refreshVersion: Int, collectionLabel: String) {
    Column(Modifier
        .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f), RoundedCornerShape(4.dp)).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) else Modifier)
        .semantics { this.selected = selected }
        .combinedClickable(onClick = open, onLongClickLabel = "Enter Management Mode", onLongClick = hold)) {
        BoxWithConstraints {
            BookCover(book, publicMode, refreshVersion, Modifier.fillMaxWidth().aspectRatio(.70f).clip(RoundedCornerShape(4.dp)))
            if (management) SelectionMark(selected, Modifier.align(Alignment.TopStart).padding(4.dp))
            else {
                val menuSize = (maxWidth * .14f).coerceIn(16.dp, 36.dp)
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(menuSize).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .9f)).clickable(onClick = menu), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreVert, "Actions for ${book.title}", Modifier.size(menuSize * .72f))
                }
            }
            if (book.progress > 0) LinearProgressIndicator({ book.progress }, Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter))
        }
        Spacer(Modifier.height(8.dp))
        Text(book.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(collectionLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${book.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookRow(book: PdfBook, open: () -> Unit, hold: () -> Unit, menu: () -> Unit, management: Boolean, selected: Boolean, publicMode: Boolean, refreshVersion: Int, collectionLabel: String) {
    Row(Modifier.fillMaxWidth()
        .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f)).border(2.dp, MaterialTheme.colorScheme.primary) else Modifier)
        .semantics { this.selected = selected }
        .combinedClickable(onClick = open, onLongClickLabel = "Enter Management Mode", onLongClick = hold).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            BookCover(book, publicMode, refreshVersion, Modifier.size(66.dp, 92.dp).clip(RoundedCornerShape(3.dp)))
            if (management) SelectionMark(selected, Modifier.align(Alignment.TopStart).padding(3.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(book.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(collectionLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (book.progress > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator({ book.progress }, Modifier.fillMaxWidth()) }
        }
        if (!management) IconButton(menu) { Icon(Icons.Default.MoreVert, "Actions for ${book.title}", Modifier.size(18.dp)) }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier) {
    Icon(if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        if (selected) "Selected" else "Not selected", modifier.size(20.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp)),
        tint = MaterialTheme.colorScheme.primary)
}

@Composable
private fun BookCover(book: PdfBook, publicMode: Boolean, refreshVersion: Int, modifier: Modifier) {
    if (book.hideCover(publicMode)) {
        Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Warning, "Adult Content cover hidden in Public Mode", Modifier.size(36.dp), tint = Color(0xFFFFC107))
        }
    } else {
        val context = LocalContext.current
        val request = remember(book.displayedCover, refreshVersion) {
            val file = File(book.displayedCover)
            ImageRequest.Builder(context).data(file).memoryCacheKey("${file.path}:${file.lastModified()}:$refreshVersion").build()
        }
        AsyncImage(request, "Cover for ${book.title}", modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
    }
}
