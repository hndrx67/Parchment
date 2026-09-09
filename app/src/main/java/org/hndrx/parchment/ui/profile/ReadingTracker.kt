package org.hndrx.parchment.ui.profile

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.hndrx.parchment.data.PdfBook
import org.hndrx.parchment.ui.LibraryViewModel

class ForegroundReadingClock(private val now: () -> Long) {
    private var started: Long? = null
    fun resume() { if (started == null) started = now() }
    fun checkpoint(): Long {
        val start = started ?: return 0
        val end = now(); started = end
        return (end - start).coerceAtLeast(0)
    }
    fun pause(): Long { val elapsed = checkpoint(); started = null; return elapsed }
}

@Composable
fun TrackReading(viewModel: LibraryViewModel, book: PdfBook, profileId: String) {
    val owner = LocalLifecycleOwner.current
    val clock = remember(book.id, profileId) { ForegroundReadingClock(SystemClock::elapsedRealtime) }
    DisposableEffect(owner, book.id, profileId) {
        fun resume() { clock.resume(); viewModel.recordReading(profileId, book, 0) }
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resume()
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.recordReading(profileId, book, clock.pause())
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); val elapsed = clock.pause(); if (elapsed > 0) viewModel.recordReading(profileId, book, elapsed) }
    }
    LaunchedEffect(clock) {
        while (true) { delay(5_000); val elapsed = clock.checkpoint(); if (elapsed > 0) viewModel.recordReading(profileId, book, elapsed) }
    }
}
