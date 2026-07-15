package com.tankermanager.app.ui.track

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.TrackingResponse
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GhostButton
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.PulsingTruck
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.components.friendlyStatus
import com.tankermanager.app.ui.theme.Coral
import com.tankermanager.app.ui.theme.Foam
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import kotlinx.coroutines.delay

@Composable
fun TrackScreen(
    repo: TankerRepository,
    initialToken: String? = null,
    onBack: () -> Unit
) {
    var token by remember { mutableStateOf(initialToken.orEmpty()) }
    var tracking by remember { mutableStateOf<TrackingResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        if (token.isBlank()) return@LaunchedEffect
        while (true) {
            loading = true
            repo.safe { track(token.trim()) }
                .onSuccess {
                    tracking = it
                    error = null
                    loading = false
                    if (it.trackingEnabled != true) break
                }
                .onFailure {
                    error = it.message
                    loading = false
                    break
                }
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFE8F8F6), Foam, Color.White))
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Live delivery", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Paste the tracking code from your SMS", color = MaterialTheme.colorScheme.onSurfaceVariant)

        SoftField(token, { token = it }, "Tracking token")
        PrimaryButton("Track now", loading = loading, onClick = {
            // token change restarts LaunchedEffect when non-blank; force refresh by toggling
            if (token.isBlank()) error = "Enter tracking token"
        })
        ErrorBanner(error)

        tracking?.let { t ->
            GlassCard {
                Text(t.tripCode ?: "Trip", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(t.status)
                Spacer(modifier = Modifier.height(8.dp))
                Text(t.message ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (t.trackingEnabled == true) {
                    Text(
                        "ETA ~ ${t.etaMinutes ?: "—"} min",
                        color = LagoonDeep,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Text(
                        "Tracking ended — thank you!",
                        color = Coral,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            MiniMapCanvas(
                enabled = t.trackingEnabled == true,
                vehicleLat = t.vehicleLat,
                vehicleLng = t.vehicleLng,
                dropLat = t.dropLat,
                dropLng = t.dropLng,
                dropAddress = t.dropAddress
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulsingTruck()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Waiting for a tracking code…")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        GhostButton("Back", onClick = onBack)
    }
}

@Composable
private fun MiniMapCanvas(
    enabled: Boolean,
    vehicleLat: Double?,
    vehicleLng: Double?,
    dropLat: Double?,
    dropLng: Double?,
    dropAddress: String?
) {
    val pulse by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.3f,
        animationSpec = tween(800),
        label = "mapPulse"
    )

    GlassCard {
        Text(if (enabled) "Vehicle on the move" else "Location hidden", fontWeight = FontWeight.SemiBold)
        Text(dropAddress ?: "Drop location", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.radialGradient(listOf(Color(0xFFD7F1EE), Color(0xFFB8E0DB))))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val start = Offset(size.width * 0.22f, size.height * 0.7f)
                val end = Offset(size.width * 0.78f, size.height * 0.28f)
                drawLine(
                    color = Lagoon.copy(alpha = 0.7f * pulse),
                    start = start,
                    end = end,
                    strokeWidth = 8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f))
                )
                // bore
                drawCircle(LagoonDeep, radius = 18f, center = start)
                // drop
                drawCircle(Coral, radius = 18f, center = end)
                if (enabled && vehicleLat != null && vehicleLng != null && dropLat != null && dropLng != null) {
                    val t = 0.45f
                    val truck = Offset(
                        start.x + (end.x - start.x) * t,
                        start.y + (end.y - start.y) * t
                    )
                    drawCircle(Color.White, radius = 22f, center = truck)
                    drawCircle(Lagoon, radius = 14f, center = truck)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(10.dp)
            ) {
                RowLabel(Icons.Rounded.WaterDrop, "Bore", LagoonDeep)
                RowLabel(Icons.Rounded.LocationOn, "Your place", Coral)
            }
            if (!enabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tracking finished", fontWeight = FontWeight.Bold, color = LagoonDeep)
                }
            }
        }
        if (enabled && vehicleLat != null) {
            Text("Lat ${"%.5f".format(vehicleLat)} · Lng ${"%.5f".format(vehicleLng ?: 0.0)}")
        }
    }
}

@Composable
private fun RowLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
