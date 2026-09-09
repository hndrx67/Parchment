package org.hndrx.parchment.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.hndrx.parchment.BuildConfig
import org.hndrx.parchment.R
import org.hndrx.parchment.data.Profile
import org.hndrx.parchment.ui.profile.ProfileAvatar

@Composable
fun ParchmentBranding() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Parchment logo",
            modifier = Modifier.size(108.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFC36B42))
        )
        Text("Parchment", style = MaterialTheme.typography.headlineLarge)
        Text("by hndrx", style = MaterialTheme.typography.titleMedium)
        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SplashScreen(animationsEnabled: Boolean = true, profile: Profile = Profile()) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ParchmentBranding()
            Spacer(Modifier.height(16.dp))
            Text("hndrx.org", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(32.dp))
            ProfileAvatar(profile, Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))
            Text("Welcome back, ${profile.username}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            if (animationsEnabled) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text("Loading…", style = MaterialTheme.typography.bodySmall)
        }
    }
}
