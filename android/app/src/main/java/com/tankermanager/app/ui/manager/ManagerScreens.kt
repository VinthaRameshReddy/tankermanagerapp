package com.tankermanager.app.ui.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.BoreExpenseRequest
import com.tankermanager.app.data.model.BoreRequest
import com.tankermanager.app.data.model.BookTripRequest
import com.tankermanager.app.data.model.CreateStaffRequest
import com.tankermanager.app.data.model.DashboardResponse
import com.tankermanager.app.data.model.DriverResponse
import com.tankermanager.app.data.model.ExpenseRequest
import com.tankermanager.app.data.model.SalaryRequest
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
                    Column {
                        Text("Hi, ${name ?: "Manager"} 👋", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text(operator ?: "Your fleet", color = Color.White.copy(alpha = 0.85f))
                    }
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
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
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        repo.safe { trips() }
            .onSuccess { trips = it }
            .onFailure { error = it.message }
    }

    val filtered = when (filter) {
        "ACTIVE" -> trips.filter { it.status !in listOf("COMPLETED", "CANCELLED") }
        "DONE" -> trips.filter { it.status == "COMPLETED" }
        else -> trips
    }

    ScreenScaffold(title = "Trips", subtitle = "Every delivery at a glance") {
        ErrorBanner(error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "All", "ACTIVE" to "Live", "DONE" to "Done").forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyState("No trips yet — tap + Trip to book")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                itemsIndexed(filtered, key = { _, t -> t.id }) { _, trip ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 4 }) {
                        TripCard(trip, onOpen = { onOpenTrip(trip.id) }, onTrack = {
                            trip.trackingToken?.let(onOpenTrack)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(trip: TripResponse, onOpen: () -> Unit, onTrack: () -> Unit) {
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
        Text(trip.dropAddress ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        if (trip.etaMinutes != null && trip.status !in listOf("COMPLETED", "CANCELLED")) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("ETA ~ ${trip.etaMinutes} min", color = LagoonDeep, fontWeight = FontWeight.SemiBold)
        }
        if (trip.trackingEnabled == true && !trip.trackingToken.isNullOrBlank()) {
            TextButton(onClick = onTrack, contentPadding = PaddingValues(0.dp)) {
                Text("Open live tracking")
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
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("17.3850") }
    var lng by remember { mutableStateOf("78.4867") }
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var tankerId by remember { mutableStateOf<Long?>(null) }
    var driverId by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repo.safe { tankers() }.onSuccess {
            tankers = it.filter { t -> t.status == "AVAILABLE" }
            tankerId = tankers.firstOrNull()?.id
        }
        repo.safe { availableDrivers() }.onSuccess {
            drivers = it
            driverId = it.firstOrNull()?.id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Book a trip", style = MaterialTheme.typography.headlineMedium)
                Text("Customer is matched by phone number", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ErrorBanner(error)
                SoftField(phone, { phone = it }, "Customer phone")
                SoftField(name, { name = it }, "Customer name (if new)")
                SoftField(address, { address = it }, "Drop address")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftField(lat, { lat = it }, "Lat", modifier = Modifier.weight(1f))
                    SoftField(lng, { lng = it }, "Lng", modifier = Modifier.weight(1f))
                }

                Text("Select tanker", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    tankers.take(4).forEach { t ->
                        FilterChip(
                            selected = tankerId == t.id,
                            onClick = { tankerId = t.id },
                            label = { Text(t.vehicleNumber) }
                        )
                    }
                }
                Text("Select driver", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    drivers.take(4).forEach { d ->
                        FilterChip(
                            selected = driverId == d.id,
                            onClick = { driverId = d.id },
                            label = { Text(d.fullName ?: d.phone ?: "Driver") }
                        )
                    }
                }

                PrimaryButton("Assign trip", loading = loading, onClick = {
                    val tid = tankerId
                    val did = driverId
                    if (tid == null || did == null) {
                        error = "Pick tanker and driver"
                        return@PrimaryButton
                    }
                    loading = true
                    error = null
                    scope.launch {
                        val result = repo.safe {
                            bookTrip(
                                BookTripRequest(
                                    customerPhone = phone.trim(),
                                    customerName = name.trim().ifBlank { null },
                                    tankerId = tid,
                                    driverId = did,
                                    dropAddress = address.trim(),
                                    dropLat = lat.toDoubleOrNull() ?: 17.385,
                                    dropLng = lng.toDoubleOrNull() ?: 78.4867
                                )
                            )
                        }
                        loading = false
                        result.onSuccess { onBooked(it.id) }
                            .onFailure { error = it.message }
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
            repo.safe { tankers() }.onSuccess { tankers = it }
            repo.safe { drivers() }.onSuccess { drivers = it }
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
                Spacer(modifier = Modifier.height(12.dp))
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

@Composable
private fun MoneyTab(repo: TankerRepository) {
    var amount by remember { mutableStateOf("") }
    var tankerId by remember { mutableStateOf<Long?>(null) }
    var tankers by remember { mutableStateOf<List<TankerResponse>>(emptyList()) }
    var type by remember { mutableStateOf("DIESEL") }
    var expenseList by remember { mutableStateOf(listOf<String>()) }
    var salaryDriver by remember { mutableStateOf<Long?>(null) }
    var drivers by remember { mutableStateOf<List<DriverResponse>>(emptyList()) }
    var salaryBase by remember { mutableStateOf("15000") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repo.safe { tankers() }.onSuccess {
            tankers = it
            tankerId = it.firstOrNull()?.id
        }
        repo.safe { drivers() }.onSuccess {
            drivers = it
            salaryDriver = it.firstOrNull()?.id
        }
        repo.safe { expenses() }.onSuccess {
            expenseList = it.take(12).map { e ->
                "${e.vehicleNumber} • ${e.type} • ₹${e.amount}"
            }
        }
    }

    ScreenScaffold(title = "Money", subtitle = "Diesel, maintenance, salaries, bore") {
        ErrorBanner(msg)
        Text("Vehicle expense", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DIESEL", "MAINTENANCE", "TYRE", "TOLL").forEach {
                FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it.lowercase().replaceFirstChar { c -> c.titlecase() }) })
            }
        }
        SoftField(amount, { amount = it }, "Amount ₹")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tankers.take(3).forEach { t ->
                FilterChip(selected = tankerId == t.id, onClick = { tankerId = t.id }, label = { Text(t.vehicleNumber) })
            }
        }
        PrimaryButton("Save expense", onClick = {
            val tid = tankerId ?: return@PrimaryButton
            scope.launch {
                repo.safe {
                    addExpense(
                        ExpenseRequest(
                            tankerId = tid,
                            type = type,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            expenseDate = LocalDate.now().toString()
                        )
                    )
                }.onSuccess {
                    msg = "Expense saved"
                    amount = ""
                }.onFailure { msg = it.message }
            }
        })

        Spacer(modifier = Modifier.height(16.dp))
        Text("Driver salary", style = MaterialTheme.typography.titleLarge)
        SoftField(salaryBase, { salaryBase = it }, "Base amount")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            drivers.take(3).forEach { d ->
                FilterChip(selected = salaryDriver == d.id, onClick = { salaryDriver = d.id }, label = { Text(d.fullName ?: "D") })
            }
        }
        CoralButton("Mark salary for ${YearMonth.now()}", onClick = {
            val did = salaryDriver ?: return@CoralButton
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
                }.onSuccess { msg = "Salary recorded" }
                    .onFailure { msg = it.message }
            }
        })

        Spacer(modifier = Modifier.height(16.dp))
        Text("Bore power / maintenance", style = MaterialTheme.typography.titleLarge)
        SoftField(amount, { amount = it }, "Bore expense ₹")
        PrimaryButton("Add bore POWER charge", onClick = {
            scope.launch {
                val bores = repo.safe { bores() }.getOrNull()
                val bore = bores?.firstOrNull()
                if (bore == null) {
                    msg = "Add a bore in Fleet first"
                    return@launch
                }
                repo.safe {
                    addBoreExpense(
                        BoreExpenseRequest(
                            boreId = bore.id,
                            type = "POWER",
                            amount = amount.toDoubleOrNull() ?: 0.0
                        )
                    )
                }.onSuccess { msg = "Bore expense saved" }
                    .onFailure { msg = it.message }
            }
        })

        Spacer(modifier = Modifier.height(12.dp))
        Text("Recent expenses", style = MaterialTheme.typography.titleMedium)
        expenseList.forEach {
            Text("• $it", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
