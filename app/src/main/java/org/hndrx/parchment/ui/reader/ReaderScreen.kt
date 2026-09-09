package org.hndrx.parchment.ui.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import org.hndrx.parchment.ui.profile.TrackReading
import org.hndrx.parchment.pdf.PdfPageRenderer
import org.hndrx.parchment.settings.*
import org.hndrx.parchment.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(id: String, viewModel: LibraryViewModel, back: () -> Unit) {
    val lookup by remember(id) { viewModel.observeBook(id).map { true to it } }.collectAsStateWithLifecycle(false to null)
    val preferences by viewModel.readerPreferences.collectAsStateWithLifecycle()
    val current = lookup.second
    if (current == null) {
        Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (!lookup.first) CircularProgressIndicator() else Text("This document is no longer in the library.")
            TextButton(back) { Text("Back") }
        }
        return
    }
    ApplyScreenPreferences(preferences)
    val context = LocalContext.current
    val loaded by produceState<Result<PdfPageRenderer>?>(null, current.filePath) {
        var opened: PdfPageRenderer? = null
        value = null
        try {
            withContext(Dispatchers.IO) { opened = PdfPageRenderer(current.filePath, context) }
            value = Result.success(requireNotNull(opened))
            awaitCancellation()
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) { value = Result.failure(error); awaitCancellation() }
        finally { opened?.close() }
    }
    val renderer = loaded?.getOrNull()
    if (renderer == null) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (loaded == null) { CircularProgressIndicator(); Text("Opening PDF…") }
            else Text("This PDF could not be opened. Check that its storage folder is available and accessible.")
            TextButton(back) { Text("Back") }
        }
        return
    }
    var chromeVisible by remember { mutableStateOf(true) }
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    TrackReading(viewModel, current, profile.id)
    var settingsOpen by remember { mutableStateOf(false) }
    var visiblePage by remember { mutableIntStateOf(current.currentPage.coerceIn(0, renderer.pageCount - 1)) }
    val background = preferences.readerColor.background()
    LaunchedEffect(visiblePage) { viewModel.setProgress(id, visiblePage) }

    Scaffold(
        containerColor = background,
        topBar = {
            if (chromeVisible) TopAppBar(
                title = { Column {
                    Text(current.title, maxLines = 1)
                    if (preferences.showPageNumber) Text("Page ${visiblePage + 1} of ${renderer.pageCount}", style = MaterialTheme.typography.labelSmall)
                } },
                navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton({ settingsOpen = true }) { Icon(Icons.Default.Tune, "Reader settings") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background, titleContentColor = background.contrast(),
                    navigationIconContentColor = background.contrast(), actionIconContentColor = background.contrast()
                )
            )
        },
        bottomBar = {
            if (chromeVisible) ReaderBottomBar(visiblePage, renderer.pageCount, background) { visiblePage = it }
        }
    ) { padding ->
        ReaderPages(renderer, preferences, visiblePage, padding, { visiblePage = it }) { chromeVisible = !chromeVisible }
    }

    if (settingsOpen) ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
        ReaderQuickSettings(preferences, viewModel::updatePreferences)
    }
}

@Composable
private fun ReaderPages(
    renderer: PdfPageRenderer,
    preferences: ReaderPreferences,
    requestedPage: Int,
    contentPadding: PaddingValues,
    onPageChanged: (Int) -> Unit,
    toggleChrome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val count = renderer.pageCount
    val spacing = preferences.pageSpacing.dp
    when (preferences.readingMode) {
        ReadingMode.PAGED_HORIZONTAL -> {
            val pager = rememberPagerState(initialPage = requestedPage, pageCount = { count })
            LaunchedEffect(pager.currentPage) { onPageChanged(pager.currentPage) }
            LaunchedEffect(requestedPage) { if (pager.currentPage != requestedPage) pager.moveToPage(preferences.animationsEnabled, requestedPage) }
            HorizontalPager(pager, Modifier.fillMaxSize().padding(contentPadding), pageSpacing = spacing) { page ->
                PdfPage(renderer, page, preferences, Modifier.fillMaxSize(),
                    { scope.launch { pager.moveToPage(preferences.animationsEnabled, (page - 1).coerceAtLeast(0)) } },
                    { scope.launch { pager.moveToPage(preferences.animationsEnabled, (page + 1).coerceAtMost(count - 1)) } }, toggleChrome)
            }
        }
        ReadingMode.PAGED_VERTICAL -> {
            val pager = rememberPagerState(initialPage = requestedPage, pageCount = { count })
            LaunchedEffect(pager.currentPage) { onPageChanged(pager.currentPage) }
            LaunchedEffect(requestedPage) { if (pager.currentPage != requestedPage) pager.moveToPage(preferences.animationsEnabled, requestedPage) }
            VerticalPager(pager, Modifier.fillMaxSize().padding(contentPadding), pageSpacing = spacing) { page ->
                PdfPage(renderer, page, preferences, Modifier.fillMaxSize(),
                    { scope.launch { pager.moveToPage(preferences.animationsEnabled, (page - 1).coerceAtLeast(0)) } },
                    { scope.launch { pager.moveToPage(preferences.animationsEnabled, (page + 1).coerceAtMost(count - 1)) } }, toggleChrome)
            }
        }
        ReadingMode.CONTINUOUS_HORIZONTAL -> {
            val list = rememberLazyListState(initialFirstVisibleItemIndex = requestedPage)
            val page by remember { derivedStateOf { list.firstVisibleItemIndex } }
            LaunchedEffect(page) { onPageChanged(page) }
            LaunchedEffect(requestedPage) { if (page != requestedPage) list.moveToItem(preferences.animationsEnabled, requestedPage) }
            LazyRow(
                state = list, modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(spacing), horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically
            ) {
                items(count, key = { it }) { index ->
                    PdfPage(renderer, index, preferences, Modifier.fillParentMaxWidth().fillParentMaxHeight(),
                        { scope.launch { list.moveToItem(preferences.animationsEnabled, (index - 1).coerceAtLeast(0)) } },
                        { scope.launch { list.moveToItem(preferences.animationsEnabled, (index + 1).coerceAtMost(count - 1)) } }, toggleChrome)
                }
            }
        }
        ReadingMode.CONTINUOUS_VERTICAL -> {
            val list = rememberLazyListState(initialFirstVisibleItemIndex = requestedPage)
            val page by remember { derivedStateOf { list.firstVisibleItemIndex } }
            LaunchedEffect(page) { onPageChanged(page) }
            LaunchedEffect(requestedPage) { if (page != requestedPage) list.moveToItem(preferences.animationsEnabled, requestedPage) }
            LazyColumn(
                state = list, modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(vertical = spacing, horizontal = preferences.pageMargin.dp),
                verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(count, key = { it }) { index ->
                    PdfPage(renderer, index, preferences, Modifier.fillMaxWidth().aspectRatio(.707f),
                        { scope.launch { list.moveToItem(preferences.animationsEnabled, (index - 1).coerceAtLeast(0)) } },
                        { scope.launch { list.moveToItem(preferences.animationsEnabled, (index + 1).coerceAtMost(count - 1)) } }, toggleChrome)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfPageRenderer,
    index: Int,
    preferences: ReaderPreferences,
    modifier: Modifier,
    previous: () -> Unit,
    next: () -> Unit,
    toggleChrome: () -> Unit
) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }
    var zoom by remember(index) { mutableFloatStateOf(1f) }
    BoxWithConstraints(
        modifier.background(preferences.readerColor.background()).pointerInput(index, preferences.tapNavigation) {
            detectTapGestures(
                onDoubleTap = { zoom = if (zoom > 1f) 1f else 2f },
                onTap = { point -> when {
                    preferences.tapNavigation && point.x < size.width * .28f -> previous()
                    preferences.tapNavigation && point.x > size.width * .72f -> next()
                    else -> toggleChrome()
                } }
            )
        }, contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.coerceAtLeast(720)
        LaunchedEffect(index, width) {
            bitmap?.recycle()
            bitmap = renderer.render(index, width)
        }
        val image = bitmap
        if (image == null) CircularProgressIndicator(color = preferences.readerColor.background().contrast())
        else Image(
            bitmap = image.asImageBitmap(), contentDescription = "Page ${index + 1}",
            modifier = when (preferences.fitMode) {
                FitMode.WIDTH -> Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height)
                FitMode.PAGE -> Modifier.fillMaxSize().padding(4.dp)
            }.graphicsLayer(scaleX = zoom, scaleY = zoom),
            contentScale = if (preferences.fitMode == FitMode.PAGE) ContentScale.Fit else ContentScale.FillWidth,
            colorFilter = if (preferences.invertPdfColors) ColorFilter.colorMatrix(invertMatrix) else null
        )
    }
    DisposableEffect(index) { onDispose { bitmap?.recycle() } }
}

@Composable
private fun ReaderBottomBar(page: Int, pageCount: Int, background: Color, jump: (Int) -> Unit) {
    BottomAppBar(containerColor = background, contentColor = background.contrast()) {
        IconButton({ jump((page - 1).coerceAtLeast(0)) }) { Icon(Icons.Default.ChevronLeft, "Previous page") }
        Slider(
            value = page.toFloat(), onValueChange = { jump(it.toInt()) }, modifier = Modifier.weight(1f),
            valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
            colors = SliderDefaults.colors(thumbColor = background.contrast(), activeTrackColor = background.contrast())
        )
        IconButton({ jump((page + 1).coerceAtMost(pageCount - 1)) }) { Icon(Icons.Default.ChevronRight, "Next page") }
    }
}

@Composable
private fun ReaderQuickSettings(prefs: ReaderPreferences, update: (ReaderPreferences) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Reader settings", style = MaterialTheme.typography.titleLarge)
        Text("Reading direction", style = MaterialTheme.typography.labelLarge)
        ReadingMode.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mode -> FilterChip(mode == prefs.readingMode, { update(prefs.copy(readingMode = mode)) }, { Text(mode.label()) }, Modifier.weight(1f)) }
            }
        }
        Text("Background", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderColor.entries.forEach { color -> FilterChip(color == prefs.readerColor, { update(prefs.copy(readerColor = color)) }, { Text(color.label()) }, Modifier.weight(1f)) }
        }
        Text("Page fit", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FitMode.entries.forEach { fit -> FilterChip(fit == prefs.fitMode, { update(prefs.copy(fitMode = fit)) }, { Text(fit.label()) }, Modifier.weight(1f)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Invert PDF colors", Modifier.weight(1f)); Switch(prefs.invertPdfColors, { update(prefs.copy(invertPdfColors = it)) })
        }
    }
}

@Composable
private fun ApplyScreenPreferences(prefs: ReaderPreferences) {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(prefs.orientation, prefs.keepScreenOn, prefs.fullscreen, prefs.brightness) {
        activity.requestedOrientation = when (prefs.orientation) {
            ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        if (prefs.keepScreenOn) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = prefs.brightness }
        if (prefs.fullscreen) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = -1f }
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private val invertMatrix = ColorMatrix(floatArrayOf(
    -1f, 0f, 0f, 0f, 255f,
    0f, -1f, 0f, 0f, 255f,
    0f, 0f, -1f, 0f, 255f,
    0f, 0f, 0f, 1f, 0f
))

private fun ReaderColor.background(): Color = when (this) {
    ReaderColor.LIGHT -> Color(0xFFF5F2EC)
    ReaderColor.DARK -> Color(0xFF101010)
    ReaderColor.SEPIA -> Color(0xFFE6D8BE)
}

private fun Color.contrast(): Color = if ((red + green + blue) / 3f < .5f) Color.White else Color(0xFF251F1A)
private fun Enum<*>.label(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

private suspend fun androidx.compose.foundation.pager.PagerState.moveToPage(animated: Boolean, page: Int) {
    if (animated) animateScrollToPage(page) else scrollToPage(page)
}

private suspend fun androidx.compose.foundation.lazy.LazyListState.moveToItem(animated: Boolean, index: Int) {
    if (animated) animateScrollToItem(index) else scrollToItem(index)
}
