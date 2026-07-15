package com.tankermanager.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.PulsingTruck
import com.tankermanager.app.ui.components.WaveBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    repo: TankerRepository,
    onDone: (destination: String) -> Unit
) {
    var ready by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(700), label = "a")
    val scale by animateFloatAsState(if (ready) 1f else 0.85f, tween(700), label = "s")

    LaunchedEffect(Unit) {
        ready = true
        delay(1100)
        val token = repo.session().token.first()
        val role = repo.session().role.first()
        when {
            token.isNullOrBlank() -> onDone("auth")
            role == "SUPER_ADMIN" -> onDone("admin")
            role == "DRIVER" -> onDone("driver")
            else -> onDone("manager")
        }
    }

    WaveBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alpha).scale(scale)
            ) {
                PulsingTruck()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "TankerFlow",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("Water logistics, made friendly", color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}
