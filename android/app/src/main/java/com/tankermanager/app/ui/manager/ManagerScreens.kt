package com.tankermanager.app.ui.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.BoreExpenseRequest
import com.tankermanager.app.data.model.BoreExpenseResponse
import com.tankermanager.app.data.model.BoreRequest
import com.tankermanager.app.data.model.BoreResponse
import com.tankermanager.app.data.model.BookTripRequest
import com.tankermanager.app.util.MapsLinkResolver
import com.tankermanager.app.data.model.CreateStaffRequest
import com.tankermanager.app.data.model.CustomerResponse
import com.tankermanager.app.data.model.DashboardResponse
import com.tankermanager.app.data.model.DistancePreviewRequest
import com.tankermanager.app.data.model.DriverResponse
import com.tankermanager.app.data.model.ExpenseRequest
import com.tankermanager.app.data.model.ExpenseResponse
import com.tankermanager.app.data.model.SalaryRequest
import com.tankermanager.app.data.model.SwapTripRequest
import com.tankermanager.app.data.model.TankerRequest
import com.tankermanager.app.data.model.TankerResponse
import com.tankermanager.app.data.model.TripResponse
import com.tankermanager.app.data.model.VehicleReportResponse
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.CoralButton
import com.tankermanager.app.ui.components.EmptyState
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.ScreenScaffold
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.StatChip
import com.tankermanager.app.ui.components.StatusPill
import com.tankermanager.app.ui.components.friendlyStatus
import com.tankermanager.app.ui.theme.Coral
import com.tankermanager.app.ui.theme.Lagoon
import com.tankermanager.app.ui.theme.LagoonDeep
import com.tankermanager.app.ui.theme.Sun
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ManagerShell(
    repo: TankerRepository,
    onLogout: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onOpenTrack: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showBook by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (tab == 1 || tab == 0) {
                FloatingActionButton(
                    onClick = { showBook = true },
                    containerColor = Coral,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("+ Trip", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val items = listOf("Home", "Trips", "Fleet", "Money")
                items.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Rounded.WaterDrop
                                    1 -> Icons.Rounded.Route
                                    2 -> Icons.Rounded.LocalShipping
                                    else -> Icons.Rounded.AccountBalanceWallet
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                0 -> DashboardTab(repo, onLogout)
                1 -> TripsTab(repo, onOpenTrip, onOpenTrack)
                2 -> FleetHub(repo)
                3 -> MoneyTab(repo)
            }
        }
    }

    if (showBook) {
        BookTripSheet(
            repo = repo,
            onDismiss = { showBook = false },
            onBooked = { id ->
                showBook = false
                tab = 1
                onOpenTrip(id)
            }
        )
    }
}

@Composable
private fun DashboardTab(repo: TankerRepository, onLogout: () -> Unit) {
    val name by repo.session().fullName.collectAsState(initial = "")
    val operator by repo.session().operatorName.collectAsState(initial = "")
    var dash by remember { mutableStateOf<DashboardResponse?>(null) }
    var reports by remember { mutableStateOf<List<VehicleReportResponse>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repo.safe { dashboard() }.onSuccess { dash = it }.onFailure { error = it.message }
        repo.safe { vehicleReports() }.onSuccess { reports = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(
                    Brush.verticalGradient(listOf(LagoonDeep, Lagoon))
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = operator?.ifBlank { "Your company" } ?: "Your company",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Logout", color = Color.White, maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Hi, ${name ?: "Manager"} 👋",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Live pulse of your water business",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorBanner(error)
            AnimatedVisibility(visible = dash != null, enter = fadeIn() + slideInVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip("Active trips", "${dash?.activeTrips ?: 0}", Lagoon, Icons.Rounded.Route, Modifier.weight(1f))
                        StatChip("Done", "${dash?.completedTrips ?: 0}", Sun, Icons.Rounded.WaterDrop, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip("Tankers", "${dash?.availableTankers ?: 0}/${dash?.totalTankers ?: 0}", Coral, Icons.Rounded.LocalShipping, Modifier.weight(1f))
                        StatChip("Drivers", "${dash?.totalDrivers ?: 0}", LagoonDeep, Icons.Rounded.Person, Modifier.weight(1f))
                    }
                    GlassCard {
                        Text("This month spend", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "₹${"%.0f".format((dash?.totalExpenses ?: 0.0) + (dash?.totalBoreExpenses ?: 0.0))}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = LagoonDeep,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Diesel + maintenance + bore power", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("Per vehicle", style = MaterialTheme.typography.titleLarge)
            if (reports.isEmpty()) {
                EmptyState("Add tankers to see performance")
            } else {
                reports.forEach { r ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(r.vehicleNumber ?: "—", fontWeight = FontWeight.Bold)
                                Text("${r.completedTrips ?: 0} trips completed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("₹${"%.0f".format(r.totalExpenses ?: 0.0)}", color = Coral, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TripsTab(
    repo: TankerRepository,
    onOpenTrip: (Long) -> Unit,
    onOpenTrack: (String) -> Unit
) {
    var trips by remember { mutableStateOf<List<TripResponse>>(emptyList()) }
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("ALL") }
    var period by remember { mutableStateOf("ALL") }
    var selectedTankerIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var swapFromId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            val tankerCsv = selectedTankerIds.takeIf { it.isNotEmpty() }?.joinToString(",")
            repo.safe { trips(period = period.takeIf { it != "ALL" }, tankerIds = tankerCsv) }
                .onSuccess { trips = it }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) {
        repo.safe { tankers() }.onSuccess { tankers = it }
        reload()
    }
    LaunchedEffect(period, selectedTankerIds) { reload() }

    val filtered = when (filter) {
        "ACTIVE" -> trips.filter { it.status !in listOf("COMPLETED", "CANCELLED") }
        "QUEUED" -> trips.filter { it.status == "QUEUED" }
        "DONE" -> trips.filter { it.status == "COMPLETED" }
        else -> trips
    }

    ScreenScaffold(title = "Trips", subtitle = "Filter by day / week / month · queue & swap") {
        ErrorBanner(error)
        Text("Time range", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All", "DAY" to "Today", "WEEK" to "This week", "MONTH" to "This month")
                .forEach { (key, label) ->
                    FilterChip(selected = period == key, onClick = { period = key }, label = { Text(label) })
                }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tankers", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTankerIds.isEmpty(),
                onClick = { selectedTankerIds = emptySet() },
                label = { Text("All tankers") }
            )
            tankers.forEach { t ->
                FilterChip(
                    selected = t.id in selectedTankerIds,
                    onClick = {
                        selectedTankerIds = if (t.id in selectedTankerIds) {
                            selectedTankerIds - t.id
                        } else {
                            selectedTankerIds + t.id
                        }
                    },
                    label = { Text(t.vehicleNumber) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All", "ACTIVE" to "Live", "QUEUED" to "Queued", "DONE" to "Done").forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) }
                )
            }
        }
        if (swapFromId != null) {
            Text(
                "Tap another queued/assigned trip on the same tanker to swap order.",
                color = Coral,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { swapFromId = null }) { Text("Cancel swap") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyState("No trips for this filter — tap + Trip to book")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                itemsIndexed(filtered, key = { _, t -> t.id }) { _, trip ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 4 }) {
                        TripCard(
                            trip = trip,
                            swapSelected = swapFromId == trip.id,
                            onOpen = { onOpenTrip(trip.id) },
                            onTrack = { trip.trackingToken?.let(onOpenTrack) },
                            onSwap = {
                                val from = swapFromId
                                if (from == null) {
                                    if (trip.status == "QUEUED" || trip.status == "ASSIGNED") {
                                        swapFromId = trip.id
                                    } else {
                                        error = "Only queued or assigned trips can be swapped"
                                    }
                                } else if (from == trip.id) {
                                    swapFromId = null
                                } else {
                                    scope.launch {
                                        repo.safe { swapTrips(from, SwapTripRequest(trip.id)) }
                                            .onSuccess {
                                                swapFromId = null
                                                error = null
                                                reload()
                                            }
                                            .onFailure { error = it.message }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(
    trip: TripResponse,
    onOpen: () -> Unit,
    onTrack: () -> Unit,
    onSwap: (() -> Unit)? = null,
    swapSelected: Boolean = false
) {
    GlassCard(onClick = onOpen) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(trip.tripCode ?: "Trip", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            StatusPill(trip.status)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(trip.customerName ?: "Customer", style = MaterialTheme.typography.titleMedium)
        Text(trip.customerPhone ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${trip.tankerNumber ?: "—"}  •  ${trip.driverName ?: "—"}")
        if (trip.status == "QUEUED" && trip.queuePosition != null) {
            Text(
                "Queue #${trip.queuePosition} — starts after current trip",
                color = Sun,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(trip.dropAddress ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        if ((trip.tripAmount ?: 0.0) > 0 || (trip.amountDue ?: 0.0) > 0) {
            Text(
                "₹${"%.0f".format(trip.tripAmount ?: 0.0)} trip" +
                    (trip.amountDue?.takeIf { it > 0 }?.let { " · due ₹${"%.0f".format(it)}" } ?: ""),
                fontWeight = FontWeight.Medium
            )
        }
        if (trip.distanceKm != null || trip.etaMinutes != null) {
            Text(
                listOfNotNull(
                    trip.distanceKm?.let { "~$it km to drop" },
                    trip.etaMinutes?.let { "ETA ${it}m" },
                    trip.boreName?.let { "via $it" }
                ).joinToString(" • "),
                color = Lagoon,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trip.trackingEnabled == true && !trip.trackingToken.isNullOrBlank()) {
                TextButton(onClick = onTrack, contentPadding = PaddingValues(0.dp)) {
                    Text("Open live tracking")
                }
            }
            if (onSwap != null && (trip.status == "QUEUED" || trip.status == "ASSIGNED")) {
                TextButton(onClick = onSwap, contentPadding = PaddingValues(0.dp)) {
                    Text(if (swapSelected) "Selected — tap other trip" else "Swap order")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookTripSheet(
    repo: TankerRepository,
    onDismiss: () -> Unit,
    onBooked: (Long) -> Unit
) {
    var customers by remember { mutableStateOf<List<CustomerResponse>>(emptyList()) }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedLocationId by remember { mutableStateOf<Long?>(null) }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var mapsLink by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var tripFare by remember { mutableStateOf("") }
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var bores by remember { mutableStateOf<List<BoreResponse>>(emptyList()) }
    var tankerId by remember { mutableStateOf<Long?>(null) }
    var driverId by remember { mutableStateOf<Long?>(null) }
    var boreId by remember { mutableStateOf<Long?>(null) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }
    var etaMinutes by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var customerMenuOpen by remember { mutableStateOf(false) }
    var locationMenuOpen by remember { mutableStateOf(false) }
    var boreMenuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val locations = selectedCustomer?.locations.orEmpty()
    val selectedLocation = locations.firstOrNull { it.id == selectedLocationId }
    val selectedBore = bores.firstOrNull { it.id == boreId }

    fun refreshDistance() {
        scope.launch {
            if (selectedLocationId == null && mapsLink.isBlank()) {
                distanceKm = null
                etaMinutes = null
                return@launch
            }
            val (link, dropLat, dropLng) = MapsLinkResolver.resolveForApi(
                context,
                mapsLink.trim().ifBlank { null },
                null,
                null
            )
            repo.safe {
                distancePreview(
                    DistancePreviewRequest(
                        boreId = boreId,
                        customerLocationId = selectedLocationId,
                        dropLat = dropLat,
                        dropLng = dropLng,
                        mapsLink = link ?: mapsLink.trim().ifBlank { null }
                    )
                )
            }.onSuccess {
                distanceKm = it.distanceKm
                etaMinutes = it.etaMinutes
                if (boreId == null) boreId = it.boreId
            }.onFailure {
                distanceKm = null
                etaMinutes = null
            }
        }
    }

    LaunchedEffect(selectedLocationId) {
        selectedLocation?.tripRate?.let { tripFare = "%.0f".format(it) }
        refreshDistance()
    }
    LaunchedEffect(boreId, mapsLink) { refreshDistance() }

    LaunchedEffect(Unit) {
        repo.safe { customers() }.onSuccess { customers = it }
        repo.safe { tankers() }.onSuccess { tankers = it }
        repo.safe { drivers() }.onSuccess { list ->
            drivers = list.filter { it.active != false }
        }.onFailure {
            error = it.message ?: "Could not load drivers"
        }
        repo.safe { bores() }.onSuccess { list ->
            bores = list
            boreId = list.firstOrNull { it.primaryBore == true }?.id ?: list.firstOrNull()?.id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Book a trip", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Pick customer, bore & location. Busy tankers get the trip in queue (one-by-one).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ErrorBanner(error)

                Text("Customer", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = customerMenuOpen,
                    onExpandedChange = { customerMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "Select customer",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenuOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = customerMenuOpen,
                        onDismissRequest = { customerMenuOpen = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.name} (${c.phone})") },
                                onClick = {
                                    selectedCustomerId = c.id
                                    phone = c.phone.orEmpty()
                                    name = c.name.orEmpty()
                                    selectedLocationId = null
                                    address = ""
                                    mapsLink = ""
                                    customerMenuOpen = false
                                }
                            )
                        }
                    }
                }

                SoftField(phone, {
                    phone = it
                    selectedCustomerId = null
                    selectedLocationId = null
                }, "Or type customer phone")
                SoftField(name, { name = it }, "Customer name (if new)")

                Text(
                    "Delivery location" +
                        if (locations.isNotEmpty()) " (${locations.size} saved)" else "",
                    fontWeight = FontWeight.SemiBold
                )
                if (locations.isEmpty()) {
                    Text(
                        "No saved sites — paste Maps link below or add location in Fleet → Customers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = locationMenuOpen,
                        onExpandedChange = { locationMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLocation?.let { "${it.label ?: "Site"} — ${it.address.orEmpty()}" }
                                ?: "Select delivery location",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationMenuOpen) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = locationMenuOpen,
                            onDismissRequest = { locationMenuOpen = false }
                        ) {
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${loc.label ?: "Site"} — ${loc.address.orEmpty()}" +
                                                (loc.tripRate?.let { " · ₹${"%.0f".format(it)}" } ?: "")
                                        )
                                    },
                                    onClick = {
                                        selectedLocationId = loc.id
                                        address = loc.address.orEmpty()
                                        mapsLink = ""
                                        loc.tripRate?.let { tripFare = "%.0f".format(it) }
                                        locationMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
                SoftField(address, { address = it }, "Drop address / landmark")
                SoftField(mapsLink, {
                    mapsLink = it
                    if (it.isNotBlank()) selectedLocationId = null
                }, "Or paste new Google Maps link (short OK)")

                SoftField(tripFare, { tripFare = it }, "Trip price ₹ (this trip)")
                Text(
                    "Filled from the location rate when you pick a site — change here for this trip only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Bore location", fontWeight = FontWeight.SemiBold)
                if (bores.isEmpty()) {
                    Text(
                        "No bores yet — add one in Fleet → Bore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = boreMenuOpen,
                        onExpandedChange = { boreMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = selectedBore?.let {
                                (it.name ?: "Bore") + if (it.primaryBore == true) " (Primary)" else ""
                            } ?: "Select bore",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boreMenuOpen) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = boreMenuOpen,
                            onDismissRequest = { boreMenuOpen = false }
                        ) {
                            bores.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            (b.name ?: "Bore") +
                                                if (b.primaryBore == true) " · Primary" else ""
                                        )
                                    },
                                    onClick = {
                                        boreId = b.id
                                        boreMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (distanceKm != null || etaMinutes != null) {
                    Text(
                        listOfNotNull(
                            distanceKm?.let { "Distance bore → customer: ~$it km" },
                            etaMinutes?.let { "ETA ~${it} min" }
                        ).joinToString(" · "),
                        color = Lagoon,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text("Select tanker", fontWeight = FontWeight.SemiBold)
                if (tankers.isEmpty()) {
                    Text(
                        "No tankers yet — add one in Fleet → Tankers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tankers.forEach { t ->
                            val busy = t.status != null && t.status != "AVAILABLE"
                            val statusLabel = if (busy) "On trip → will queue" else "Available now"
                            FilterChip(
                                selected = tankerId == t.id,
                                onClick = { tankerId = t.id },
                                enabled = t.status != "MAINTENANCE" && t.status != "INACTIVE",
                                label = {
                                    Text("${t.vehicleNumber} · $statusLabel")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Text("Select driver (active only)", fontWeight = FontWeight.SemiBold)
                if (drivers.isEmpty()) {
                    Text(
                        "No active drivers — owner adds drivers in Fleet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        drivers.forEach { d ->
                            val busy = d.available == false
                            val statusLabel = if (busy) "On trip (OK for queue)" else "Available"
                            FilterChip(
                                selected = driverId == d.id,
                                onClick = { driverId = d.id },
                                label = {
                                    Text("${d.fullName ?: d.phone ?: "Driver"} · $statusLabel")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                PrimaryButton("Assign trip", loading = loading, onClick = {
                    val tid = tankerId
                    val did = driverId
                    if (tid == null || did == null) {
                        error = "Pick tanker and driver"
                        return@PrimaryButton
                    }
                    if (phone.isBlank()) {
                        error = "Customer phone required"
                        return@PrimaryButton
                    }
                    if (selectedLocationId == null && mapsLink.isBlank() && address.isBlank()) {
                        error = "Pick a delivery location or paste Maps link"
                        return@PrimaryButton
                    }
                    loading = true
                    error = null
                    scope.launch {
                        val linkPaste = mapsLink.trim()
                        if (selectedLocationId == null && linkPaste.isNotBlank()) {
                            error = "Reading location from Maps link…"
                        }
                        val (link, dropLat, dropLng) = MapsLinkResolver.resolveForApi(
                            context,
                            linkPaste.ifBlank { null },
                            null,
                            null
                        )
                        val result = repo.safe {
                            bookTrip(
                                BookTripRequest(
                                    customerPhone = phone.trim(),
                                    customerName = name.trim().ifBlank { null },
                                    tankerId = tid,
                                    driverId = did,
                                    boreId = boreId,
                                    customerLocationId = selectedLocationId,
                                    dropAddress = address.trim().ifBlank { null },
                                    dropLat = dropLat,
                                    dropLng = dropLng,
                                    mapsLink = link ?: linkPaste.ifBlank { null },
                                    tripAmount = tripFare.toDoubleOrNull()
                                )
                            )
                        }
                        loading = false
                        result.onSuccess { booked ->
                            if (booked.status == "QUEUED") {
                                error = "Queued at #${booked.queuePosition} — starts after current trip"
                            }
                            onBooked(booked.id)
                        }.onFailure { error = it.message }
                    }
                })
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun FleetTab(repo: TankerRepository) {
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var section by remember { mutableIntStateOf(0) }
    var vehicle by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("5000") }
    var driverName by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf("") }
    var driverPass by remember { mutableStateOf("Driver@123") }
    var boreName by remember { mutableStateOf("Main Bore") }
    var boreAddress by remember { mutableStateOf("") }
    var boreLat by remember { mutableStateOf("17.3850") }
    var boreLng by remember { mutableStateOf("78.4867") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            repo.safe { tankers() }.onSuccess { tankers = it }.onFailure { msg = it.message }
            repo.safe { drivers() }.onSuccess { drivers = it }.onFailure { msg = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    ScreenScaffold(title = "Fleet", subtitle = "Tankers, drivers & bore") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tankers", "Drivers", "Bore").forEachIndexed { i, label ->
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
                        repo.safe {
                            addTanker(TankerRequest(vehicle, capacityLitres = capacity.toIntOrNull()))
                        }.onSuccess {
                            vehicle = ""
                            refresh()
                            msg = "Tanker added"
                        }.onFailure { msg = it.message }
                    }
                })
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                val role by repo.session().role.collectAsState(initial = "")
                val isOwner = role == "OWNER"
                if (isOwner) {
                    SoftField(driverName, { driverName = it }, "Driver name")
                    SoftField(driverPhone, { driverPhone = it }, "Driver phone")
                    SoftField(driverPass, { driverPass = it }, "Login password", password = true)
                    PrimaryButton("Add driver", onClick = {
                        scope.launch {
                            repo.safe {
                                createStaff(
                                    CreateStaffRequest(
                                        fullName = driverName.trim(),
                                        phone = driverPhone.trim(),
                                        password = driverPass,
                                        role = "DRIVER",
                                        monthlySalary = 15000.0
                                    )
                                )
                            }.onSuccess {
                                driverName = ""
                                driverPhone = ""
                                refresh()
                                msg = "Driver created — they can login with phone + password"
                            }.onFailure { msg = it.message }
                        }
                    })
                } else {
                    Text(
                        "Only the owner can create drivers. Ask owner, or open Fleet hub as owner.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (drivers.isEmpty()) {
                    Text("No drivers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(drivers, key = { it.id }) { d ->
                        GlassCard {
                            Text(d.fullName ?: "Driver", fontWeight = FontWeight.Bold)
                            Text(d.phone ?: "")
                            Text(
                                "${d.totalTripsCompleted ?: 0} trips • score ${d.performanceScore ?: 100}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (d.available == true) "Available" else "On duty",
                                color = if (d.available == true) LagoonDeep else Coral
                            )
                        }
                    }
                }
            }
            else -> {
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
                        }.onSuccess { msg = "Bore saved" }
                            .onFailure { msg = it.message }
                    }
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneyTab(repo: TankerRepository) {
    var side by remember { mutableStateOf("add") } // add | view

    Row(modifier = Modifier.fillMaxSize()) {
        MoneySideBar(
            selectedAdd = side == "add",
            onAdd = { side = "add" },
            onView = { side = "view" }
        )
        Box(modifier = Modifier.weight(1f)) {
            when (side) {
                "view" -> ViewExpensesPanel(repo)
                else -> AddExpensePanel(repo)
            }
        }
    }
}

@Composable
private fun MoneySideBar(
    selectedAdd: Boolean,
    onAdd: () -> Unit,
    onView: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .fillMaxSize()
            .background(Color(0xFFE3F0ED))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Money",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = LagoonDeep,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        MoneySideItem(label = "Add\nexpense", selected = selectedAdd, onClick = onAdd)
        MoneySideItem(label = "View\nexpenses", selected = !selectedAdd, onClick = onView)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneySideItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) LagoonDeep else Color.White,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else LagoonDeep
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpensePanel(repo: TankerRepository) {
    var amount by remember { mutableStateOf("") }
    var vehicleId by remember { mutableStateOf<Long?>(null) }
    var vehicleMenuOpen by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf("VEHICLE") } // VEHICLE | BORE | DRIVER
    var kindMenuOpen by remember { mutableStateOf(false) }
    var vehicleType by remember { mutableStateOf("DIESEL") }
    var boreType by remember { mutableStateOf("POWER") }
    var boreId by remember { mutableStateOf<Long?>(null) }
    var boreMenuOpen by remember { mutableStateOf(false) }
    var driverId by remember { mutableStateOf<Long?>(null) }
    var driverMenuOpen by remember { mutableStateOf(false) }
    var salaryBase by remember { mutableStateOf("15000") }

    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var bores by remember { mutableStateOf<List<BoreResponse>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun reload() {
        scope.launch {
            repo.safe { tankers() }.onSuccess {
                tankers = it
                if (vehicleId == null) vehicleId = it.firstOrNull()?.id
            }
            repo.safe { bores() }.onSuccess {
                bores = it
                if (boreId == null) boreId = it.firstOrNull()?.id
            }
            repo.safe { drivers() }.onSuccess {
                drivers = it
                if (driverId == null) driverId = it.firstOrNull()?.id
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val vehicle = tankers.firstOrNull { it.id == vehicleId }
    val bore = bores.firstOrNull { it.id == boreId }
    val driver = drivers.firstOrNull { it.id == driverId }
    val kindLabel = when (kind) {
        "BORE" -> "Bore"
        "DRIVER" -> "Driver salary"
        else -> "Vehicle maintenance"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Add expense", style = MaterialTheme.typography.headlineMedium)
        Text("Choose vehicle context, then expense type", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        ErrorBanner(msg)

        GlassCard {
            Text("Vehicle (for fleet records)", fontWeight = FontWeight.SemiBold, color = LagoonDeep)
            Text(
                vehicle?.vehicleNumber ?: "Select vehicle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text("Expenses are tagged to this tanker in reports", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = vehicleMenuOpen, onExpandedChange = { vehicleMenuOpen = it }) {
                OutlinedTextField(
                    value = vehicle?.vehicleNumber ?: "Choose vehicle",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleMenuOpen) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = vehicleMenuOpen, onDismissRequest = { vehicleMenuOpen = false }) {
                    tankers.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.vehicleNumber) },
                            onClick = {
                                vehicleId = t.id
                                vehicleMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Expense type", fontWeight = FontWeight.SemiBold)
        ExposedDropdownMenuBox(expanded = kindMenuOpen, onExpandedChange = { kindMenuOpen = it }) {
            OutlinedTextField(
                value = kindLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindMenuOpen) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = kindMenuOpen, onDismissRequest = { kindMenuOpen = false }) {
                DropdownMenuItem(text = { Text("Vehicle — diesel / tyre / toll") }, onClick = {
                    kind = "VEHICLE"; kindMenuOpen = false
                })
                DropdownMenuItem(text = { Text("Bore — power / maintenance") }, onClick = {
                    kind = "BORE"; kindMenuOpen = false
                })
                DropdownMenuItem(text = { Text("Driver — monthly salary") }, onClick = {
                    kind = "DRIVER"; kindMenuOpen = false
                })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SoftField(amount, { amount = it }, "Amount ₹")

        when (kind) {
            "BORE" -> {
                Text("Select bore", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = boreMenuOpen, onExpandedChange = { boreMenuOpen = it }) {
                    OutlinedTextField(
                        value = bore?.name ?: "Choose bore",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boreMenuOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = boreMenuOpen, onDismissRequest = { boreMenuOpen = false }) {
                        bores.forEach { b ->
                            DropdownMenuItem(text = { Text(b.name ?: "Bore") }, onClick = {
                                boreId = b.id; boreMenuOpen = false
                            })
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("POWER", "MAINTENANCE", "OTHER").forEach {
                        FilterChip(
                            selected = boreType == it,
                            onClick = { boreType = it },
                            label = { Text(it.lowercase().replaceFirstChar { c -> c.titlecase() }) }
                        )
                    }
                }
                PrimaryButton("Save bore expense", onClick = {
                    val bid = boreId
                    val amt = amount.toDoubleOrNull()
                    if (bid == null) { msg = "Select a bore"; return@PrimaryButton }
                    if (amt == null || amt <= 0) { msg = "Enter valid amount"; return@PrimaryButton }
                    scope.launch {
                        repo.safe {
                            addBoreExpense(
                                BoreExpenseRequest(boreId = bid, type = boreType, amount = amt)
                            )
                        }.onSuccess {
                            msg = "Bore expense saved"
                            amount = ""
                        }.onFailure { msg = it.message }
                    }
                })
            }
            "DRIVER" -> {
                Text("Select driver", fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(expanded = driverMenuOpen, onExpandedChange = { driverMenuOpen = it }) {
                    OutlinedTextField(
                        value = driver?.fullName ?: driver?.phone ?: "Choose driver",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverMenuOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = driverMenuOpen, onDismissRequest = { driverMenuOpen = false }) {
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.fullName ?: d.phone ?: "Driver") },
                                onClick = { driverId = d.id; driverMenuOpen = false }
                            )
                        }
                    }
                }
                SoftField(salaryBase, { salaryBase = it }, "Salary base ₹")
                CoralButton("Mark salary ${YearMonth.now()}", onClick = {
                    val did = driverId ?: return@CoralButton
                    scope.launch {
                        repo.safe {
                            addSalary(
                                SalaryRequest(
                                    driverId = did,
                                    salaryMonth = YearMonth.now().toString(),
                                    baseAmount = salaryBase.toDoubleOrNull() ?: 0.0,
                                    markPaid = true
                                )
                            )
                        }.onSuccess { msg = "Salary recorded for ${driver?.fullName}" }
                            .onFailure { msg = it.message }
                    }
                })
            }
            else -> {
                Text("Maintenance category", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DIESEL", "MAINTENANCE", "TYRE", "TOLL", "OTHER").forEach {
                        FilterChip(
                            selected = vehicleType == it,
                            onClick = { vehicleType = it },
                            label = { Text(it.lowercase().replaceFirstChar { c -> c.titlecase() }) }
                        )
                    }
                }
                PrimaryButton(
                    "Save for ${vehicle?.vehicleNumber ?: "vehicle"}",
                    onClick = {
                        val tid = vehicleId
                        val amt = amount.toDoubleOrNull()
                        if (tid == null) { msg = "Select vehicle"; return@PrimaryButton }
                        if (amt == null || amt <= 0) { msg = "Enter valid amount"; return@PrimaryButton }
                        scope.launch {
                            repo.safe {
                                addExpense(
                                    ExpenseRequest(
                                        tankerId = tid,
                                        type = vehicleType,
                                        amount = amt,
                                        expenseDate = LocalDate.now().toString()
                                    )
                                )
                            }.onSuccess {
                                msg = "Saved ${vehicleType.lowercase()} for ${vehicle?.vehicleNumber}"
                                amount = ""
                            }.onFailure { msg = it.message }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewExpensesPanel(repo: TankerRepository) {
    var filterTankerId by remember { mutableStateOf<Long?>(null) }
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseResponse>>(emptyList()) }
    var boreExpenses by remember { mutableStateOf<List<BoreExpenseResponse>>(emptyList()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun load() {
        scope.launch {
            repo.safe { tankers() }.onSuccess { tankers = it }
            repo.safe { expenses() }.onSuccess { expenses = it }
            repo.safe { boreExpenses() }.onSuccess { boreExpenses = it }
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered = if (filterTankerId == null) expenses else expenses.filter { it.tankerId == filterTankerId }
    val vehicleTotal = expenses.sumOf { it.amount ?: 0.0 }
    val boreTotal = boreExpenses.sumOf { it.amount ?: 0.0 }
    val shownVehicleTotal = filtered.sumOf { it.amount ?: 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("View expenses", style = MaterialTheme.typography.headlineMedium)
        Text("Fleet + bore spend", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        ErrorBanner(msg)

        GlassCard {
            Text("All vehicles", style = MaterialTheme.typography.titleMedium)
            Text(
                "₹${"%.0f".format(vehicleTotal)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = LagoonDeep
            )
            Text("Bore (all): ₹${"%.0f".format(boreTotal)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (filterTankerId != null) {
                val v = tankers.firstOrNull { it.id == filterTankerId }?.vehicleNumber ?: "Vehicle"
                Text("Filtered $v: ₹${"%.0f".format(shownVehicleTotal)}", color = Coral, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Filter by vehicle", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterTankerId == null,
                onClick = { filterTankerId = null },
                label = { Text("All") }
            )
            tankers.forEach { t ->
                FilterChip(
                    selected = filterTankerId == t.id,
                    onClick = { filterTankerId = t.id },
                    label = { Text(t.vehicleNumber) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Vehicle expenses", style = MaterialTheme.typography.titleLarge)
        if (filtered.isEmpty()) {
            Text("No vehicle expenses for this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { e ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(e.vehicleNumber ?: "—", fontWeight = FontWeight.Bold)
                            Text(
                                "${e.type?.lowercase()?.replaceFirstChar { c -> c.titlecase() }} • ${e.expenseDate ?: ""}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("₹${"%.0f".format(e.amount ?: 0.0)}", fontWeight = FontWeight.Bold, color = Coral)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Bore expenses", style = MaterialTheme.typography.titleLarge)
        if (boreExpenses.isEmpty()) {
            Text("No bore expenses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            boreExpenses.take(20).forEach { e ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(e.boreName ?: "Bore", fontWeight = FontWeight.Bold)
                            Text(
                                "${e.type?.lowercase()} • ${e.expenseDate ?: ""}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("₹${"%.0f".format(e.amount ?: 0.0)}", fontWeight = FontWeight.Bold, color = Coral)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
