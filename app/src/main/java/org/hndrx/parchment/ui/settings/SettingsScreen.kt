package org.hndrx.parchment.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.settings.*
import org.hndrx.parchment.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: LibraryViewModel, back: () -> Unit, openAbout: () -> Unit,
    openAppearance: () -> Unit = {}, openCollectionAppearance: () -> Unit = {}, openRecovery: () -> Unit = {}) {
    val saved by viewModel.readerPreferences.collectAsStateWithLifecycle()
    var prefs by remember(saved) { mutableStateOf(saved) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingsHeader("Look and Feel")
            ListItem(headlineContent = { Text("Appearance") }, supportingContent = { Text("Light and dark mode, OLED Black, Mint Green, Haze Purple, and more") }, modifier = Modifier.clickable(onClick = openAppearance))
            ListItem(headlineContent = { Text("Customize collections") }, supportingContent = { Text("Collection cards, colors, and icons") }, modifier = Modifier.clickable(onClick = openCollectionAppearance))
            SettingsHeader("Reader")
            EnumSetting("Reading mode", prefs.readingMode.label(), ReadingMode.entries) { prefs = prefs.copy(readingMode = it); viewModel.updatePreferences(prefs) }
            EnumSetting("Page fit", prefs.fitMode.label(), FitMode.entries) { prefs = prefs.copy(fitMode = it); viewModel.updatePreferences(prefs) }
            EnumSetting("Reader background", prefs.readerColor.label(), ReaderColor.entries) { prefs = prefs.copy(readerColor = it); viewModel.updatePreferences(prefs) }
            EnumSetting("Screen orientation", prefs.orientation.label(), ScreenOrientation.entries) { prefs = prefs.copy(orientation = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Page display")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Page spacing: ${prefs.pageSpacing} dp")
                Slider(prefs.pageSpacing.toFloat(), { prefs = prefs.copy(pageSpacing = it.toInt()) }, valueRange = 0f..32f, steps = 7, onValueChangeFinished = { viewModel.updatePreferences(prefs) })
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Page margin: ${prefs.pageMargin} dp")
                Slider(prefs.pageMargin.toFloat(), { prefs = prefs.copy(pageMargin = it.toInt()) }, valueRange = 0f..32f, steps = 7, onValueChangeFinished = { viewModel.updatePreferences(prefs) })
            }
            SwitchSetting("Invert PDF colors", "Makes white PDF pages dark and dark content light.", prefs.invertPdfColors) { prefs = prefs.copy(invertPdfColors = it); viewModel.updatePreferences(prefs) }
            SwitchSetting("Show page number", "Display reading position in the reader toolbar.", prefs.showPageNumber) { prefs = prefs.copy(showPageNumber = it); viewModel.updatePreferences(prefs) }
            SwitchSetting("Tap navigation", "Tap page edges for previous or next page in paged modes.", prefs.tapNavigation) { prefs = prefs.copy(tapNavigation = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Screen")
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(if (prefs.brightness < 0f) "Brightness: system default" else "Brightness: ${(prefs.brightness * 100).toInt()}%")
                Slider(
                    value = if (prefs.brightness < 0f) 0f else prefs.brightness,
                    onValueChange = { prefs = prefs.copy(brightness = it) },
                    valueRange = 0f..1f,
                    onValueChangeFinished = { viewModel.updatePreferences(prefs) }
                )
                TextButton({ prefs = prefs.copy(brightness = -1f); viewModel.updatePreferences(prefs) }) { Text("Use system brightness") }
            }
            SwitchSetting("Keep screen awake", "Prevent the display from sleeping while reading.", prefs.keepScreenOn) { prefs = prefs.copy(keepScreenOn = it); viewModel.updatePreferences(prefs) }
            SwitchSetting("Fullscreen reader", "Hide system bars while a document is open.", prefs.fullscreen) { prefs = prefs.copy(fullscreen = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Library and animation")
            SwitchSetting("Animations", "Animate navigation and page changes.", prefs.animationsEnabled) { prefs = prefs.copy(animationsEnabled = it); viewModel.updatePreferences(prefs) }
            SwitchSetting("Grid library", "Library layout is saved automatically, together with your search, sort, favorites, and tag filters.", prefs.defaultGridLayout) { prefs = prefs.copy(defaultGridLayout = it); viewModel.updatePreferences(prefs) }
            GridSizeSetting(prefs.gridSize) { prefs = prefs.copy(gridSize = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Privacy")
            SwitchSetting("Public Mode", "Replace covers tagged Adult Content with a black warning cover in the library and collections.", prefs.publicMode) { prefs = prefs.copy(publicMode = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Storage")
            StorageSettings(prefs.storageDirectory) { prefs = prefs.copy(storageDirectory = it); viewModel.updatePreferences(prefs) }
            SettingsHeader("Recovery")
            ListItem(headlineContent = { Text("Backup and Recovery · Experimental") }, supportingContent = { Text("Back up or recover configuration and imported PDF files") }, modifier = Modifier.clickable(onClick = openRecovery))
            SettingsHeader("About")
            ListItem(
                headlineContent = { Text("About Parchment") },
                supportingContent = { Text("Credits, website, and app version") },
                modifier = Modifier.clickable(onClick = openAbout)
            )
        }
    }
}

@Composable private fun SettingsHeader(text: String) = Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp))

@Composable
private fun SwitchSetting(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { change(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.width(16.dp)); Switch(checked, change)
    }
}

@Composable
private fun <T : Enum<T>> EnumSetting(title: String, selected: String, values: List<T>, change: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 20.dp, vertical = 13.dp)) {
            Text(title, Modifier.weight(1f)); Text(selected, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(value.label()) }, onClick = { change(value); open = false }) }
        }
    }
}

private fun Enum<*>.label(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

@Composable
private fun GridSizeSetting(size: Int, change: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    var input by remember(size) { mutableStateOf(size.toString()) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Library grid size", style = MaterialTheme.typography.titleMedium)
        Text("Minimum cover width: $size dp. Smaller covers fit more columns.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Small" to 100, "Medium" to 150, "Large" to 220).forEach { (label, width) ->
                FilterChip(selected = size == width, onClick = { change(width) }, label = { Text(label) })
            }
        }
        TextButton(onClick = { input = size.toString(); custom = true }) { Text("Custom size…") }
    }
    if (custom) AlertDialog(
        onDismissRequest = { custom = false },
        title = { Text("Custom grid size") },
        text = {
            OutlinedTextField(input, { input = it }, label = { Text("Cover width (dp)") },
                supportingText = { Text("Enter a whole number from 80 to 400") },
                isError = input.toIntOrNull() !in 80..400, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        },
        confirmButton = { TextButton(enabled = input.toIntOrNull() in 80..400, onClick = { change(input.toInt()); custom = false }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } }
    )
}
