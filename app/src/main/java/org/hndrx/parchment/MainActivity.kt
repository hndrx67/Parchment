package org.hndrx.parchment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import org.hndrx.parchment.ui.ParchmentApp

class MainActivity : ComponentActivity() {
    private val libraryViewModel: org.hndrx.parchment.ui.LibraryViewModel by viewModels()

    private fun openIntent(value: android.content.Intent?) {
        if (value?.action == android.content.Intent.ACTION_VIEW) {
            value.data?.takeIf { it.scheme == "content" || it.scheme == "file" }?.let(libraryViewModel::openExternal)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openIntent(intent)
    }
    private val motionScale = object : MotionDurationScale {
        var enabled by mutableStateOf(true)
        var systemScale by mutableFloatStateOf(1f)
        override val scaleFactor: Float get() = if (enabled) systemScale else 0f
    }

    override fun onResume() {
        super.onResume()
        motionScale.systemScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openIntent(intent)
        val content = ComposeView(this)
        setContentView(content)
        content.setParentCompositionContext(content.createLifecycleAwareWindowRecomposer(motionScale, lifecycle))
        content.setContent { ParchmentApp(viewModel = libraryViewModel, onAnimationsChanged = { motionScale.enabled = it }) }
    }
}
