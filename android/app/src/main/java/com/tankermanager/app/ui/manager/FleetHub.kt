package com.tankermanager.app.ui.manager

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.util.lastKnownLocation
import com.tankermanager.app.util.MapsLinkResolver
import com.tankermanager.app.data.model.BoreRequest
import com.tankermanager.app.data.model.CreateStaffRequest
import com.tankermanager.app.data.model.CustomerLocationRequest
import com.tankermanager.app.data.model.CustomerRequest
import com.tankermanager.app.data.model.CustomerResponse
import com.tankermanager.app.data.model.DriverResponse
import com.tankermanager.app.data.model.StaffResponse
import com.tankermanager.app.data.model.TankerRequest
import com.tankermanager.app.data.model.TankerResponse
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.ScreenScaffold
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.theme.Coral
import com.tankermanager.app.ui.theme.LagoonDeep
import kotlinx.coroutines.launch

/**
 * Owner creates managers & drivers.
 * Owner + Manager add customers.
 */
@Composable
fun FleetHub(repo: TankerRepository) {
    val role by repo.session().role.collectAsState(initial = "")
    val isOwner = role == "OWNER"
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var managers by remember { mutableStateOf<List<StaffResponse>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerResponse>>(emptyList()) }
    var section by remember { mutableIntStateOf(0) }
    var vehicle by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("5000") }
    var staffName by remember { mutableStateOf("") }
    var staffPhone by remember { mutableStateOf("") }
    var staffPass by remember { mutableStateOf("Pass@123") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var customerMapsLink by remember { mutableStateOf("") }
    var customerGpsLat by remember { mutableStateOf<Double?>(null) }
    var customerGpsLng by remember { mutableStateOf<Double?>(null) }
    var customerLocLabel by remember { mutableStateOf("Home") }
    var customerTripRate by remember { mutableStateOf("") }
    var addingLocationForId by remember { mutableStateOf<Long?>(null) }
    var extraLocLabel by remember { mutableStateOf("") }
    var extraLocMaps by remember { mutableStateOf("") }
    var extraGpsLat by remember { mutableStateOf<Double?>(null) }
    var extraGpsLng by remember { mutableStateOf<Double?>(null) }
    var extraLocAddress by remember { mutableStateOf("") }
    var extraLocRate by remember { mutableStateOf("") }
    var boreName by remember { mutableStateOf("Main Bore") }
    var boreAddress by remember { mutableStateOf("") }
    var boreLat by remember { mutableStateOf("17.3850") }
    var boreLng by remember { mutableStateOf("78.4867") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var gpsTarget by remember { mutableStateOf(0) } // 1 = new customer, 2 = extra location
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!ok) {
            msg = "Location permission is needed for current GPS"
            return@rememberLauncherForActivityResult
        }
        val loc = lastKnownLocation(context)
        if (loc == null) {
            msg = "Could not read GPS — turn on location and try again"
            return@rememberLauncherForActivityResult
        }
        val lat = loc.latitude
        val lng = loc.longitude
        when (gpsTarget) {
            1 -> {
                customerGpsLat = loc.latitude
                customerGpsLng = loc.longitude
            }
            2 -> {
                extraGpsLat = loc.latitude
                extraGpsLng = loc.longitude
            }
        }
        msg = "GPS location ready — or paste a Maps link"
    }
    val tabs = listOf("Tankers", "Bore", "Drivers", "Managers", "Customers")

    fun requestGpsFor(target: Int) {
        gpsTarget = target
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun refresh() {
        scope.launch {
            repo.safe { tankers() }.onSuccess { tankers = it }.onFailure { msg = it.message }
            repo.safe { drivers() }.onSuccess { drivers = it }.onFailure { msg = it.message }
            repo.safe { managers() }.onSuccess { managers = it }.onFailure { msg = it.message }
            repo.safe { customers() }.onSuccess { customers = it }.onFailure { msg = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(section) {
        if (section == 4) refresh()
    }

    ScreenScaffold(
        title = "Fleet",
        subtitle = if (isOwner) "Owner: tankers, drivers, managers, customers & bore"
        else "Manager: customers, trips & bore"
    ) {
        // Bore is the 5th chip — must scroll on narrow phones or it is clipped off-screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { i, label ->
                FilterChip(selected = section == i, onClick = { section = i }, label = { Text(label) })
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ErrorBanner(msg)

        when (section) {
            0 -> {
                SoftField(vehicle, { vehicle = it.uppercase() }, "Vehicle number")
                SoftField(capacity, { capacity = it }, "Capacity (litres)")
                PrimaryButton("Add tanker", onClick = {
                    scope.launch {
                        repo.safe { addTanker(TankerRequest(vehicle, capacityLitres = capacity.toIntOrNull())) }
                            .onSuccess { vehicle = ""; refresh(); msg = "Tanker added" }
                            .onFailure { msg = it.message }
                    }
                })
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tankers, key = { it.id }) { t ->
                        GlassCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(t.vehicleNumber, fontWeight = FontWeight.Bold)
                                    Text("${t.capacityLitres ?: "—"} L", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(t.status)
                            }
                        }
                    }
                }
            }
            1 -> {
                SoftField(boreName, { boreName = it }, "Bore name")
                SoftField(boreAddress, { boreAddress = it }, "Bore address")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftField(boreLat, { boreLat = it }, "Lat", modifier = Modifier.weight(1f))
                    SoftField(boreLng, { boreLng = it }, "Lng", modifier = Modifier.weight(1f))
                }
                PrimaryButton("Save primary bore", onClick = {
                    scope.launch {
                        repo.safe {
                            addBore(
                                BoreRequest(
                                    name = boreName,
                                    address = boreAddress.ifBlank { "Bore yard" },
                                    latitude = boreLat.toDoubleOrNull() ?: 17.385,
                                    longitude = boreLng.toDoubleOrNull() ?: 78.4867,
                                    primaryBore = true
                                )
                            )
                        }.onSuccess { msg = "Bore saved" }.onFailure { msg = it.message }
                    }
                })
            }
            2 -> {
                if (isOwner) {
                    SoftField(staffName, { staffName = it }, "Driver name")
                    SoftField(staffPhone, { staffPhone = it }, "Driver phone")
                    SoftField(staffPass, { staffPass = it }, "Login password", password = true)
                    PrimaryButton("Add driver", onClick = {
                        scope.launch {
                            repo.safe {
                                createStaff(
                                    CreateStaffRequest(
                                        fullName = staffName.trim(),
                                        phone = staffPhone.trim(),
                                        password = staffPass,
                                        role = "DRIVER",
                                        monthlySalary = 15000.0
                                    )
                                )
                            }.onSuccess { staffName = ""; staffPhone = ""; refresh(); msg = "Driver created" }
                                .onFailure { msg = it.message }
                        }
                    })
                } else {
                    Text("Only the owner can create drivers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(drivers, key = { it.id }) { d ->
                        GlassCard {
                            Text(d.fullName ?: "Driver", fontWeight = FontWeight.Bold)
                            Text(d.phone ?: "")
                            Text(
                                if (d.available == true) "Available" else "On duty",
                                color = if (d.available == true) LagoonDeep else Coral
                            )
                        }
                    }
                }
            }
            3 -> {
                if (isOwner) {
                    SoftField(staffName, { staffName = it }, "Manager name")
                    SoftField(staffPhone, { staffPhone = it }, "Manager phone")
                    SoftField(staffPass, { staffPass = it }, "Login password", password = true)
                    PrimaryButton("Add manager", onClick = {
                        scope.launch {
                            repo.safe {
                                createStaff(
                                    CreateStaffRequest(
                                        fullName = staffName.trim(),
                                        phone = staffPhone.trim(),
                                        password = staffPass,
                                        role = "MANAGER"
                                    )
                                )
                            }.onSuccess {
                                staffName = ""; staffPhone = ""; refresh()
                                msg = "Manager created — can login & add customers"
                            }.onFailure { msg = it.message }
                        }
                    })
                } else {
                    Text("Only the owner can create managers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(managers, key = { it.id ?: 0L }) { m ->
                        GlassCard {
                            Text(m.fullName ?: "Manager", fontWeight = FontWeight.Bold)
                            Text(m.phone ?: "")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftField(customerName, { customerName = it }, "Customer name")
                            SoftField(customerPhone, { customerPhone = it }, "Customer phone")
                            SoftField(customerLocLabel, { customerLocLabel = it }, "Location shortcut (Home / Office / Site)")
                            SoftField(customerTripRate, { customerTripRate = it }, "Trip rate ₹ for this location")
                            SoftField(customerAddress, { customerAddress = it }, "Address / landmark (optional)")
                            SoftField(customerMapsLink, { customerMapsLink = it }, "Paste Google Maps link from WhatsApp")
                            TextButton(onClick = { requestGpsFor(1) }) {
                                Text("Or use current phone location")
                            }
                            Text(
                                "Only paste the shared Maps link — the app reads the pin automatically. " +
                                    "Trip rate is per drop; changes apply to new trips only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrimaryButton("Save customer + first location", onClick = {
                                if (customerMapsLink.isBlank() && (customerGpsLat == null || customerGpsLng == null)) {
                                    msg = "Paste the Google Maps link or use current location"
                                    return@PrimaryButton
                                }
                                scope.launch {
                                    msg = if (customerMapsLink.isNotBlank()) "Reading location from Maps link…" else null
                        val (link, lat, lng) = MapsLinkResolver.resolveForApi(
                            context,
                            customerMapsLink,
                                        customerGpsLat,
                                        customerGpsLng
                                    )
                        if (link.isNullOrBlank() && (lat == null || lng == null)) {
                            msg = "Paste the Google Maps link or use current location"
                            return@launch
                        }
                        if (lat == null || lng == null) {
                            msg = "Could not read this Maps link. Open it in Google Maps, tap Share, and paste again — or use current location."
                            return@launch
                        }
                        repo.safe {
                                        upsertCustomer(
                                            CustomerRequest(
                                                name = customerName.trim(),
                                                phone = customerPhone.trim(),
                                                defaultAddress = customerAddress.trim().ifBlank { null },
                                                defaultLat = lat,
                                                defaultLng = lng,
                                                mapsLink = link,
                                                locationLabel = customerLocLabel.trim().ifBlank { "Delivery" }
                                            )
                                        )
                                    }.onSuccess { saved ->
                                        val rate = customerTripRate.toDoubleOrNull()
                                        val locId = saved.locations?.firstOrNull()?.id
                                        if (rate != null && locId != null) {
                                            repo.safe {
                                                updateCustomerLocation(
                                                    saved.id,
                                                    locId,
                                                    CustomerLocationRequest(tripRate = rate)
                                                )
                                            }
                                        }
                                        customerName = ""; customerPhone = ""; customerAddress = ""
                                        customerMapsLink = ""
                                        customerGpsLat = null; customerGpsLng = null
                                        customerLocLabel = "Home"; customerTripRate = ""
                                        refresh(); msg = "Customer & delivery location saved"
                                    }.onFailure { msg = it.message }
                                }
                            })
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Saved customers (${customers.size})",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (customers.isEmpty()) {
                                Text(
                                    "No customers yet — save one above. Each customer can have multiple delivery sites.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(customers, key = { it.id }) { c ->
                        GlassCard {
                            Text(c.name ?: "Customer", fontWeight = FontWeight.Bold)
                            Text(c.phone ?: "")
                            val locs = c.locations.orEmpty()
                            if (locs.isEmpty()) {
                                Text(c.defaultAddress ?: "No locations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(
                                    "Delivery sites (${locs.size})",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                locs.forEach { loc ->
                                    Text(
                                        "• ${loc.label}: ${loc.address}" +
                                            (loc.tripRate?.let { " · ₹${"%.0f".format(it)}/trip" } ?: ""),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (addingLocationForId == c.id) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SoftField(extraLocLabel, { extraLocLabel = it }, "New location shortcut name")
                                SoftField(extraLocAddress, { extraLocAddress = it }, "Landmark (optional)")
                                SoftField(extraLocRate, { extraLocRate = it }, "Trip rate ₹")
                                SoftField(extraLocMaps, { extraLocMaps = it }, "Paste Google Maps link")
                                TextButton(onClick = { requestGpsFor(2) }) {
                                    Text("Or use current phone location")
                                }
                                PrimaryButton("Add this location", onClick = {
                                    if (extraLocMaps.isBlank() && (extraGpsLat == null || extraGpsLng == null)) {
                                        msg = "Paste the Google Maps link or use current location"
                                        return@PrimaryButton
                                    }
                                    scope.launch {
                                        msg = if (extraLocMaps.isNotBlank()) "Reading location from Maps link…" else null
                                        val (link, lat, lng) = MapsLinkResolver.resolveForApi(
                                            context,
                                            extraLocMaps,
                                            extraGpsLat,
                                            extraGpsLng
                                        )
                        if (link.isNullOrBlank() && (lat == null || lng == null)) {
                            msg = "Paste the Google Maps link or use current location"
                            return@launch
                        }
                        if (lat == null || lng == null) {
                            msg = "Could not read this Maps link. Open it in Google Maps, tap Share, and paste again — or use current location."
                            return@launch
                        }
                        repo.safe {
                                            addCustomerLocation(
                                                c.id,
                                                CustomerLocationRequest(
                                                    label = extraLocLabel.trim().ifBlank { "Delivery" },
                                                    address = extraLocAddress.trim().ifBlank { null },
                                                    mapsLink = link,
                                                    latitude = lat,
                                                    longitude = lng,
                                                    tripRate = extraLocRate.toDoubleOrNull()
                                                )
                                            )
                                        }.onSuccess {
                                            addingLocationForId = null
                                            extraLocLabel = ""; extraLocMaps = ""
                                            extraGpsLat = null; extraGpsLng = null
                                            extraLocAddress = ""; extraLocRate = ""
                                            refresh(); msg = "Location added — available when booking trips"
                                        }.onFailure { msg = it.message }
                                    }
                                })
                                TextButton(onClick = {
                                    addingLocationForId = null
                                    extraLocLabel = ""; extraLocMaps = ""; extraLocAddress = ""; extraLocRate = ""
                                    extraGpsLat = null; extraGpsLng = null
                                }) { Text("Cancel") }
                            } else {
                                TextButton(onClick = {
                                    addingLocationForId = c.id
                                    extraLocLabel = ""
                                    extraLocMaps = ""
                                    extraLocAddress = ""
                                    extraLocRate = ""
                                }) { Text("+ Add another location") }
                            }
                        }
                    }
                }
            }
        }
    }
}
