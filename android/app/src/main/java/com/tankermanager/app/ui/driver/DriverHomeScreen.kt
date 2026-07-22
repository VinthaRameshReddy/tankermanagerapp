package com.tankermanager.app.ui.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tankermanager.app.data.model.UpdateTripStatusRequest
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.EmptyState
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.PulsingTruck
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.components.friendlyStatus
import com.tankermanager.app.ui.components.nextStatus
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DriverHomeScreen(repo: TankerRepository, onLogout: () -> Unit) {
    val name by repo.session().fullName.collectAsState(initial = "")
    var trips by remember { mutableStateOf<List<TripResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<TripResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* granted checked on share */ }

    fun refresh() {
        scope.launch {
            repo.safe { driverActiveTrips() }
                .onSuccess {
                    trips = it
                    if (selected == null) selected = it.firstOrNull()
                    else selected = it.find { t -> t.id == selected?.id } ?: it.firstOrNull()
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

    LaunchedEffect(sharing, selected?.id) {
        val trip = selected ?: return@LaunchedEffect
        while (sharing && trip.status !in listOf("COMPLETED", "CANCELLED")) {
            val loc = currentLocation(context)
            if (loc != null) {
                repo.safe {
                    updateLocation(
                        trip.id,
                        LocationUpdateRequest(loc.latitude, loc.longitude, loc.speed * 3.6f)
                    )
                }
            }
            delay(8000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(LagoonDeep, Lagoon)))
                .padding(20.dp)
        ) {
            Column {
                TextButton(onClick = onLogout) { Text("Logout", color = Color.White) }
                PulsingTruck()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Hey ${name ?: "Driver"}", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Your assigned water runs", color = Color.White.copy(alpha = 0.9f))
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorBanner(error)
            if (trips.isEmpty()) {
                EmptyState("No active trips. Relax — new jobs will appear here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, false)) {
                    items(trips, key = { it.id }) { trip ->
                        GlassCard(onClick = { selected = trip }) {
                            Text(trip.tripCode ?: "", fontWeight = FontWeight.Bold)
                            StatusPill(trip.status)
                            Text("${trip.customerName} • ${trip.customerPhone}")
                            Text(trip.dropAddress ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                selected?.let { trip ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    GlassCard {
                        Text("Current job", fontWeight = FontWeight.Bold)
                        Text(friendlyStatus(trip.status), color = LagoonDeep, style = MaterialTheme.typography.titleLarge)
                        Text("Bore: ${trip.boreName}")
                        Text("Drop: ${trip.dropAddress}")
                        if (trip.distanceKm != null) {
                            Text("Distance ~ ${trip.distanceKm} km", fontWeight = FontWeight.SemiBold)
                        }
                        if (trip.etaMinutes != null) {
                            Text("ETA ~ ${trip.etaMinutes} min")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!trip.mapsNavigateUrl.isNullOrBlank()) {
                            PrimaryButton("Navigate to drop (Maps)", onClick = {
                                try {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(trip.mapsNavigateUrl)
                                        )
                                    )
                                } catch (_: Exception) {
                                    error = "Could not open Maps"
                                }
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        val next = nextStatus(trip.status)
                        if (next != null) {
                            PrimaryButton("Update → ${friendlyStatus(next)}", onClick = {
                                scope.launch {
                                    repo.safe {
                                        driverUpdateStatus(trip.id, UpdateTripStatusRequest(next))
                                    }.onSuccess {
                                        selected = it
                                        refresh()
                                        if (next == "EN_ROUTE" || next == "GOING_FOR_LOADING") {
                                            sharing = true
                                        }
                                        if (next == "COMPLETED") sharing = false
                                    }.onFailure { error = it.message }
                                }
                            })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = if (sharing) "Sharing live location ✓" else "Start sharing location",
                            onClick = { sharing = !sharing },
                            enabled = trip.status !in listOf("COMPLETED", "CANCELLED")
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun currentLocation(context: android.content.Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        return null
    }
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
}
