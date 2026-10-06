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
import com.tankermanager.app.data.model.BoreResponse
import com.tankermanager.app.data.model.CreateStaffRequest
import com.tankermanager.app.data.model.CustomerLocationRequest
import com.tankermanager.app.data.model.CustomerRequest
import com.tankermanager.app.data.model.CustomerResponse
import com.tankermanager.app.data.model.DriverResponse
import com.tankermanager.app.data.model.DriverStatusRequest
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
    var bores by remember { mutableStateOf<List<BoreResponse>>(emptyList()) }
    var managers by remember { mutableStateOf<List<StaffResponse>>(emptyList()) }
    var customers by remember { mutableStateOf<List<CustomerResponse>>(emptyList()) }
    var section by remember { mutableIntStateOf(0) }
    var vehicle by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("5000") }
    var editingTankerId by remember { mutableStateOf<Long?>(null) }
    var editingBoreId by remember { mutableStateOf<Long?>(null) }
    var boreMapsLink by remember { mutableStateOf("") }
    var showResignedDrivers by remember { mutableStateOf(false) }
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
            repo.safe { drivers(includeResigned = showResignedDrivers || isOwner) }
                .onSuccess { drivers = it }.onFailure { msg = it.message }
            repo.safe { bores() }.onSuccess { bores = it }.onFailure { msg = it.message }
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
                PrimaryButton(
                    if (editingTankerId == null) "Add tanker" else "Save tanker changes",
                    onClick = {
                        scope.launch {
                            val body = TankerRequest(vehicle, capacityLitres = capacity.toIntOrNull())
                            val editId = editingTankerId
                            val result = if (editId == null) {
                                repo.safe { addTanker(body) }
                            } else {
                                repo.safe { updateTanker(editId, body) }
                            }
                            result.onSuccess {
                                vehicle = ""; capacity = "5000"; editingTankerId = null
                                refresh(); msg = if (editId == null) "Tanker added" else "Tanker updated"
                            }.onFailure { msg = it.message }
                        }
                    }
                )
                if (editingTankerId != null) {
                    TextButton(onClick = {
                        editingTankerId = null; vehicle = ""; capacity = "5000"
                    }) { Text("Cancel edit") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tankers, key = { it.id }) { t ->
                        GlassCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.vehicleNumber, fontWeight = FontWeight.Bold)
                                    Text("${t.capacityLitres ?: "—"} L", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(t.status)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    editingTankerId = t.id
                                    vehicle = t.vehicleNumber
                                    capacity = (t.capacityLitres ?: 5000).toString()
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    scope.launch {
                                        repo.safe { deleteTanker(t.id) }
                                            .onSuccess { refresh(); msg = "Tanker removed" }
                                            .onFailure { msg = it.message }
                                    }
                                }) { Text("Delete", color = Coral) }
                            }
                        }
                    }
                }
            }
            1 -> {
                SoftField(boreName, { boreName = it }, "Bore name")
                SoftField(boreAddress, { boreAddress = it }, "Bore address / landmark")
                SoftField(boreMapsLink, { boreMapsLink = it }, "Paste Google Maps link")
                Text(
                    "Paste Maps link preferred — lat/lng optional if link is provided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftField(boreLat, { boreLat = it }, "Lat (optional)", modifier = Modifier.weight(1f))
                    SoftField(boreLng, { boreLng = it }, "Lng (optional)", modifier = Modifier.weight(1f))
                }
                PrimaryButton(
                    if (editingBoreId == null) "Add bore" else "Save bore changes",
                    onClick = {
                        if (boreName.isBlank()) {
                            msg = "Bore name required"
                            return@PrimaryButton
                        }
                        if (boreMapsLink.isBlank() && (boreLat.toDoubleOrNull() == null || boreLng.toDoubleOrNull() == null)) {
                            msg = "Paste Maps link or enter lat/lng"
                            return@PrimaryButton
                        }
                        scope.launch {
                            msg = if (boreMapsLink.isNotBlank()) "Reading bore location from Maps…" else null
                            val (link, lat, lng) = MapsLinkResolver.resolveForApi(
                                context,
                                boreMapsLink.ifBlank { null },
                                boreLat.toDoubleOrNull(),
                                boreLng.toDoubleOrNull()
                            )
                            if (lat == null || lng == null) {
                                msg = "Could not read bore location — check Maps link or lat/lng"
                                return@launch
                            }
                            val body = BoreRequest(
                                name = boreName.trim(),
                                address = boreAddress.ifBlank { "Bore yard" },
                                mapsLink = link ?: boreMapsLink.ifBlank { null },
                                latitude = lat,
                                longitude = lng,
                                primaryBore = editingBoreId == null || bores.none { it.primaryBore == true }
                            )
                            val editId = editingBoreId
                            val result = if (editId == null) {
                                repo.safe { addBore(body) }
                            } else {
                                repo.safe { updateBore(editId, body.copy(primaryBore = bores.any { it.id == editId && it.primaryBore == true })) }
                            }
                            result.onSuccess {
                                boreName = "Main Bore"; boreAddress = ""; boreMapsLink = ""
                                boreLat = "17.3850"; boreLng = "78.4867"; editingBoreId = null
                                refresh(); msg = if (editId == null) "Bore added" else "Bore updated"
                            }.onFailure { msg = it.message }
                        }
                    }
                )
                if (editingBoreId != null) {
                    TextButton(onClick = {
                        editingBoreId = null
                        boreName = "Main Bore"; boreAddress = ""; boreMapsLink = ""
                    }) { Text("Cancel edit") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Bores (${bores.size})", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bores, key = { it.id }) { b ->
                        GlassCard {
                            Text(
                                (b.name ?: "Bore") + if (b.primaryBore == true) " · Primary" else "",
                                fontWeight = FontWeight.Bold
                            )
                            Text(b.address ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!b.mapsLink.isNullOrBlank()) {
                                Text("Maps link saved", style = MaterialTheme.typography.bodySmall, color = LagoonDeep)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    editingBoreId = b.id
                                    boreName = b.name.orEmpty()
                                    boreAddress = b.address.orEmpty()
                                    boreMapsLink = b.mapsLink.orEmpty()
                                    boreLat = b.latitude?.toString() ?: ""
                                    boreLng = b.longitude?.toString() ?: ""
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    scope.launch {
                                        repo.safe { updateBore(b.id, BoreRequest(
                                            name = b.name ?: "Bore",
                                            address = b.address,
                                            mapsLink = b.mapsLink,
                                            latitude = b.latitude,
                                            longitude = b.longitude,
                                            primaryBore = true
                                        )) }
                                            .onSuccess { refresh(); msg = "Set as primary bore" }
                                            .onFailure { msg = it.message }
                                    }
                                }) { Text("Make primary") }
                                TextButton(onClick = {
                                    scope.launch {
                                        repo.safe { deleteBore(b.id) }
                                            .onSuccess { refresh(); msg = "Bore removed" }
                                            .onFailure { msg = it.message }
                                    }
                                }) { Text("Delete", color = Coral) }
                            }
                        }
                    }
                }
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
                    FilterChip(
                        selected = showResignedDrivers,
                        onClick = {
                            showResignedDrivers = !showResignedDrivers
                            refresh()
                        },
                        label = { Text(if (showResignedDrivers) "Showing resigned too" else "Show resigned") }
                    )
                } else {
                    Text("Only the owner can create drivers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(drivers, key = { it.id }) { d ->
                        val resigned = d.active == false
                        GlassCard {
                            Text(d.fullName ?: "Driver", fontWeight = FontWeight.Bold)
                            Text(d.phone ?: "")
                            Text(
                                when {
                                    resigned -> "Resigned"
                                    d.available == true -> "Active · Available"
                                    else -> "Active · On trip"
                                },
                                color = when {
                                    resigned -> Coral
                                    d.available == true -> LagoonDeep
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (isOwner) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (resigned) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                repo.safe { setDriverStatus(d.id, DriverStatusRequest(active = true)) }
                                                    .onSuccess { refresh(); msg = "Driver set to Active" }
                                                    .onFailure { msg = it.message }
                                            }
                                        }) { Text("Make Active") }
                                    } else {
                                        TextButton(onClick = {
                                            scope.launch {
                                                repo.safe { setDriverStatus(d.id, DriverStatusRequest(active = false)) }
                                                    .onSuccess { refresh(); msg = "Driver marked Resigned" }
                                                    .onFailure { msg = it.message }
                                            }
                                        }) { Text("Mark Resigned", color = Coral) }
                                    }
                                }
                            }
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
