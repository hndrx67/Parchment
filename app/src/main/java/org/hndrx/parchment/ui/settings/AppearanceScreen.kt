package org.hndrx.parchment.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hndrx.parchment.settings.*
import org.hndrx.parchment.ui.LibraryViewModel
import org.hndrx.parchment.ui.theme.paletteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(viewModel: LibraryViewModel, back: () -> Unit) {
    val prefs by viewModel.readerPreferences.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Appearance") }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Display mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AppTheme.entries.forEach { mode ->
                FilterChip(prefs.appTheme == mode, { viewModel.updatePreferences(prefs.copy(appTheme = mode)) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) })
            } }
            Text("Color scheme", style = MaterialTheme.typography.titleLarge)
            Text("Each scheme includes light and dark colors. OLED Black uses a pure black background in dark mode.", style = MaterialTheme.typography.bodyMedium)
            AppPalette.entries.forEach { palette ->
                OutlinedCard(Modifier.fillMaxWidth().clickable { viewModel.updatePreferences(prefs.copy(palette = palette)) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(palette.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (prefs.palette == palette) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                    Row(Modifier.fillMaxWidth()) { listOf(false, true).forEach { dark ->
                        val colors = paletteColors(palette, dark)
                        Column(Modifier.weight(1f).background(colors.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (dark) "Dark" else "Light", color = colors.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(colors.primary, colors.primaryContainer, colors.surfaceContainerHigh).forEach { color ->
                                Box(Modifier.size(28.dp).background(color, MaterialTheme.shapes.small))
                            } }
                        }
                    } }
                }
            }
        }
    }
}
