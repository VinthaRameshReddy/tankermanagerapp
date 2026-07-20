package com.tankermanager.app.ui.manager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.TripResponse
import com.tankermanager.app.data.model.UpdateTripStatusRequest
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.CoralButton
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.ScreenScaffold
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.components.friendlyStatus
import com.tankermanager.app.ui.components.nextStatus
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import com.tankermanager.app.ui.theme.Success
import kotlinx.coroutines.launch

@Composable
fun TripDetailScreen(
    tripId: Long,
    repo: TankerRepository,
    onBack: () -> Unit,
    onTrack: (String) -> Unit
) {
    var trip by remember { mutableStateOf<TripResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            repo.safe { trip(tripId) }
                .onSuccess { trip = it }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(tripId) { load() }

    val t = trip
    ScreenScaffold(
        title = t?.tripCode ?: "Trip",
        subtitle = t?.customerName,
        topBarExtra = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        }
    ) {
        ErrorBanner(error)
        if (t == null) {
            Text("Loading…")
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(t.customerName ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(t.customerPhone ?: "")
                    }
                    StatusPill(t.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Bore → Customer", color = LagoonDeep, fontWeight = FontWeight.SemiBold)
                Text("${t.boreName}  →  ${t.dropAddress}")
                Text("${t.tankerNumber} • Driver ${t.driverName}")
                if (t.etaMinutes != null) {
                    Text("ETA ~ ${t.etaMinutes} min", fontWeight = FontWeight.Bold, color = Lagoon)
                }
            }

            GlassCard {
                Text("Status timeline", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                val steps = listOf(
                    "ASSIGNED", "GOING_FOR_LOADING", "LOADING", "LOADING_COMPLETED",
                    "EN_ROUTE", "ARRIVED", "UNLOADING", "COMPLETED"
                )
                val currentIndex = steps.indexOf(t.status).coerceAtLeast(0)
                steps.forEachIndexed { index, step ->
                    TimelineRow(
                        label = friendlyStatus(step),
                        active = index <= currentIndex,
                        done = index < currentIndex || t.status == "COMPLETED",
                        isLast = index == steps.lastIndex
                    )
                }
            }

            t.history.orEmpty().asReversed().take(6).forEach { h ->
                Text(
                    "${friendlyStatus(h.toStatus)} • ${if (h.smsSent == true) "SMS sent" else "No SMS"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        val next = nextStatus(t.status)
        if (next != null) {
            PrimaryButton(
                text = "Mark: ${friendlyStatus(next)}",
                loading = loading,
                onClick = {
                    loading = true
                    scope.launch {
                        repo.safe {
                            managerUpdateStatus(tripId, UpdateTripStatusRequest(next))
                        }.onSuccess {
                            trip = it
                            loading = false
                        }.onFailure {
                            error = it.message
                            loading = false
                        }
                    }
                }
            )
        }
        if (!t.trackingToken.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            CoralButton("Customer live track", onClick = { onTrack(t.trackingToken!!) })
        }
    }
}

@Composable
private fun TimelineRow(label: String, active: Boolean, done: Boolean, isLast: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawCircle(if (active) if (done) Success else LagoonDeep else Color(0xFFB7D0CD))
            }
            if (!isLast) {
                Canvas(modifier = Modifier.width(2.dp).height(28.dp)) {
                    drawLine(
                        color = if (done) Success else Color(0xFFB7D0CD),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 4f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.padding(top = 0.dp, bottom = if (isLast) 0.dp else 14.dp),
            fontWeight = if (active && !done) FontWeight.Bold else FontWeight.Normal,
            color = if (active) LagoonDeep else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
