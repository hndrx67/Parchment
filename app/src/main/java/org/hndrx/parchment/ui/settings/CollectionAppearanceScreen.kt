package org.hndrx.parchment.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.data.BookCollection
import org.hndrx.parchment.settings.AppPalette
import org.hndrx.parchment.ui.LibraryViewModel
import org.hndrx.parchment.ui.theme.paletteColors

fun collectionIcon(name: String) = when (name) {
    "BOOK" -> Icons.Default.MenuBook; "STUDY" -> Icons.Default.School; "FAVORITE" -> Icons.Default.Favorite
    "WORK" -> Icons.Default.Work; "STAR" -> Icons.Default.Star; else -> Icons.Default.Folder
}

@Composable
fun CollectionEntry(collection: BookCollection, count: Int, cards: Boolean, open: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val palette = runCatching { AppPalette.valueOf(collection.color) }.getOrDefault(AppPalette.PARCHMENT)
    val colors = paletteColors(palette, MaterialTheme.colorScheme.background.luminance() < .5f)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = open),
        shape = MaterialTheme.shapes.medium, color = if (cards) colors.primaryContainer else MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(if (cards) 20.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (cards) 58.dp else 40.dp).background(colors.primary, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Icon(collectionIcon(collection.icon), null, Modifier.size(if (cards) 30.dp else 24.dp), tint = colors.onPrimary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(collection.name, style = MaterialTheme.typography.titleMedium, color = if (cards) colors.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                Text("$count documents", style = MaterialTheme.typography.bodySmall, color = if (cards) colors.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionAppearanceScreen(viewModel: LibraryViewModel, back: () -> Unit) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val members by viewModel.collectionMembers.collectAsStateWithLifecycle()
    val prefs by viewModel.readerPreferences.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BookCollection?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Collection appearance") }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Display as cards", Modifier.weight(1f)); Switch(prefs.collectionCards, { viewModel.updatePreferences(prefs.copy(collectionCards = it)) })
                }
                Text("Give each collection a color and icon. Tap a collection to customize it.", Modifier.padding(horizontal = 20.dp))
            }
            if (collections.isEmpty()) item { Text("Create a collection from your library to customize it here.", Modifier.padding(20.dp)) }
            items(collections, key = { it.id }) { collection ->
                CollectionEntry(collection, members.count { it.collectionId == collection.id }, prefs.collectionCards, { editing = collection }) { Icon(Icons.Default.Edit, "Customize") }
            }
        }
    }
    editing?.let { collection ->
        var color by remember(collection.id) { mutableStateOf(collection.color) }
        var icon by remember(collection.id) { mutableStateOf(collection.icon) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text(collection.name) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Color")
                Row(Modifier.horizontalScroll(rememberScrollState())) { AppPalette.entries.forEach { palette ->
                    FilterChip(color == palette.name, { color = palette.name }, label = { Text(palette.label) }, modifier = Modifier.padding(end = 6.dp))
                } }
                Text("Icon")
                Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("FOLDER", "BOOK", "STUDY", "FAVORITE", "WORK", "STAR").forEach { value ->
                    IconToggleButton(icon == value, { icon = value }) { Icon(collectionIcon(value), value.lowercase(), tint = if (icon == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                } }
                CollectionEntry(collection.copy(color = color, icon = icon), members.count { it.collectionId == collection.id }, true, {})
            } },
            confirmButton = { TextButton({ viewModel.customizeCollection(collection.copy(color = color, icon = icon)); editing = null }) { Text("Save") } },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } })
    }
}
