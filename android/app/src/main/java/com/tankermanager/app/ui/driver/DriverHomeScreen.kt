package com.tankermanager.app.ui.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tankermanager.app.data.model.LocationUpdateRequest
import com.tankermanager.app.data.model.TripResponse
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.EmptyState
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.components.friendlyStatus
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import com.tankermanager.app.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DriverHomeScreen(repo: TankerRepository, onLogout: () -> Unit) {
    val name by repo.session().fullName.collectAsState(initial = "")
    var trips by remember { mutableStateOf<List<TripResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<TripResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var liveHint by remember { mutableStateOf("Live GPS sharing is on for active trips") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    fun refresh() {
        scope.launch {
            repo.safe { driverActiveTrips() }
                .onSuccess { list ->
                    val ordered = list.sortedWith(
                        compareBy(
                            { if (it.status == "QUEUED") 1 else 0 },
                            { it.queuePosition ?: 0 }
                        )
                    )
                    trips = ordered
                    selected = when {
                        selected == null -> ordered.firstOrNull { it.status != "QUEUED" } ?: ordered.firstOrNull()
                        else -> ordered.find { t -> t.id == selected?.id }
                            ?: ordered.firstOrNull { it.status != "QUEUED" }
                            ?: ordered.firstOrNull()
                    }
                }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        refresh()
    }

    // Auto live sharing whenever there is an active (non-queued) trip — no manual toggle.
    LaunchedEffect(selected?.id, trips.map { it.id to it.status }) {
        val trip = selected ?: return@LaunchedEffect
        if (trip.status in listOf("COMPLETED", "CANCELLED", "QUEUED")) {
            if (trip.status == "QUEUED") {
                liveHint = "Next in queue #${trip.queuePosition ?: "—"} — starts after current trip"
            }
            return@LaunchedEffect
        }
        liveHint = "Sharing live location · status updates automatically"
        while (true) {
            val current = selected ?: break
            if (current.status in listOf("COMPLETED", "CANCELLED", "QUEUED")) break
            val loc = currentLocation(context)
            if (loc != null) {
                repo.safe {
                    updateLocation(
                        current.id,
                        LocationUpdateRequest(loc.latitude, loc.longitude, loc.speed * 3.6f)
                    )
                }.onSuccess { updated ->
                    selected = updated
                    // Refresh list so next trip appears after auto-complete
                    if (updated.status in listOf("COMPLETED", "CANCELLED")) {
                        refresh()
                    } else {
                        trips = trips.map { if (it.id == updated.id) updated else it }
                    }
                }
            }
            delay(8000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3FAF8))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B3D4A), LagoonDeep, Lagoon)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Driver cabin",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = onLogout) { Text("Logout", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Column {
                        Text("Hey ${name ?: "Driver"}", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text(liveHint, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorBanner(error)
            if (trips.isEmpty()) {
                EmptyState("No active trips. Relax — new jobs will appear here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, false)) {
                    items(trips, key = { it.id }) { trip ->
                        val selectedCard = selected?.id == trip.id
                        GlassCard(onClick = { selected = trip }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(trip.tripCode ?: "", fontWeight = FontWeight.Bold)
                                    Text("${trip.customerName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (trip.status == "QUEUED") {
                                        Text(
                                            "Queue #${trip.queuePosition ?: "—"}",
                                            color = Sun,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                StatusPill(trip.status)
                            }
                            if (selectedCard) {
                                Text(
                                    if (trip.status == "QUEUED") "Waiting in queue" else "Current focus",
                                    color = LagoonDeep,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                selected?.let { trip ->
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = LagoonDeep)
                            Text(friendlyStatus(trip.status), color = LagoonDeep, style = MaterialTheme.typography.titleLarge)
                        }
                        Text("Bore: ${trip.boreName}")
                        Text("Drop: ${trip.dropAddress}")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (trip.distanceKm != null) {
                                Text("~${trip.distanceKm} km", fontWeight = FontWeight.SemiBold)
                            }
                            if (trip.etaMinutes != null) {
                                Text("ETA ${trip.etaMinutes} min", color = Sun, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "Status updates automatically from your GPS near bore / drop.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!trip.mapsNavigateUrl.isNullOrBlank()) {
                            val label = if (trip.status in listOf("ASSIGNED", "GOING_FOR_LOADING", "LOADING")) {
                                "Navigate to bore"
                            } else {
                                "Navigate to drop"
                            }
                            PrimaryButton(label, onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(trip.mapsNavigateUrl))
                                    )
                                } catch (_: Exception) {
                                    error = "Could not open Maps"
                                }
                            })
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Navigation, contentDescription = null, tint = Lagoon, modifier = Modifier.size(18.dp))
                            Text("Live sharing ON", color = LagoonDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun currentLocation(context: android.content.Context): Location? =
    com.tankermanager.app.util.lastKnownLocation(context)
