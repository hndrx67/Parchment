package org.hndrx.parchment.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.hndrx.parchment.data.Profile
import org.hndrx.parchment.ui.LibraryViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@Composable
fun ProfileAvatar(profile: Profile, modifier: Modifier = Modifier) {
    if (profile.avatarPath != null) AsyncImage(File(profile.avatarPath), "Profile picture for ${profile.username}", modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    else Icon(Icons.Default.Person, "Profile", modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: LibraryViewModel, back: () -> Unit, openBook: (String) -> Unit) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val entries = history.filter { it.profileId == profile.id }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var username by remember(profile.id, profile.username) { mutableStateOf(profile.username) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.setProfilePicture(profile, it) } }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { TopAppBar(title = { Text("Profiles") }, navigationIcon = { IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileAvatar(profile, Modifier.size(96.dp).align(Alignment.CenterHorizontally))
            TextButton(enabled = !state.managing, onClick = { picker.launch("image/*") }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Choose profile picture") }
            OutlinedTextField(username, { username = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
            Button(enabled = username.isNotBlank() && !state.managing, onClick = { viewModel.saveProfile(profile.copy(username = username.trim())) }) { Text("Save profile") }
            Text("Switch profile", style = MaterialTheme.typography.titleMedium)
            profiles.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(item.id == profile.id, { viewModel.selectProfile(item.id) })
                    ProfileAvatar(item, Modifier.size(32.dp)); TextButton({ viewModel.selectProfile(item.id) }) { Text(item.username) }
                }
            }
            TextButton({ newName = ""; creating = true }) { Text("Add profile") }
            Text("Profiles share this library. Reading history and time are tracked separately.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Reading history", style = MaterialTheme.typography.titleLarge)
            Text("${"%.2f".format(entries.sumOf { it.totalMillis } / 3_600_000.0)} hours · ${entries.size} documents", style = MaterialTheme.typography.titleMedium)
            Text("Time counts while a document is open in the foreground. Tracking starts with this version.", style = MaterialTheme.typography.bodySmall)
            if (entries.isEmpty()) Text("Open a PDF to start your reading history.")
            entries.forEach { entry ->
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text("${"%.2f".format(entry.totalMillis / 3_600_000.0)} hours (${entry.totalMillis / 60_000} min)")
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.lastReadAt)), style = MaterialTheme.typography.bodySmall)
                    TextButton({ openBook(entry.bookId) }) { Text("Open document") }
                } }
            }
        }
    }
    if (creating) AlertDialog(onDismissRequest = { creating = false }, title = { Text("New profile") },
        text = { OutlinedTextField(newName, { newName = it.take(80) }, label = { Text("Username") }) },
        confirmButton = { TextButton(enabled = newName.isNotBlank(), onClick = { viewModel.saveProfile(Profile(UUID.randomUUID().toString(), newName.trim())); creating = false }) { Text("Create") } },
        dismissButton = { TextButton({ creating = false }) { Text("Cancel") } })
}
