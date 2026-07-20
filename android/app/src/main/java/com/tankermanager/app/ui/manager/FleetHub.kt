package com.tankermanager.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.BoreRequest
import com.tankermanager.app.data.model.CreateStaffRequest
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
    var boreName by remember { mutableStateOf("Main Bore") }
    var boreAddress by remember { mutableStateOf("") }
    var boreLat by remember { mutableStateOf("17.3850") }
    var boreLng by remember { mutableStateOf("78.4867") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Tankers", "Drivers", "Managers", "Customers", "Bore")

    fun refresh() {
        scope.launch {
            repo.safe { tankers() }.onSuccess { tankers = it }
            repo.safe { drivers() }.onSuccess { drivers = it }
            repo.safe { managers() }.onSuccess { managers = it }
            repo.safe { customers() }.onSuccess { customers = it }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    ScreenScaffold(
        title = "Fleet",
        subtitle = if (isOwner) "Owner: create managers, drivers & customers"
        else "Manager: add customers and run trips"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            2 -> {
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
            3 -> {
                SoftField(customerName, { customerName = it }, "Customer name")
                SoftField(customerPhone, { customerPhone = it }, "Customer phone")
                SoftField(customerAddress, { customerAddress = it }, "Default address")
                PrimaryButton("Save customer", onClick = {
                    scope.launch {
                        repo.safe {
                            upsertCustomer(
                                CustomerRequest(
                                    name = customerName.trim(),
                                    phone = customerPhone.trim(),
                                    defaultAddress = customerAddress.trim().ifBlank { null }
                                )
                            )
                        }.onSuccess {
                            customerName = ""; customerPhone = ""; customerAddress = ""
                            refresh(); msg = "Customer saved"
                        }.onFailure { msg = it.message }
                    }
                })
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(customers, key = { it.id }) { c ->
                        GlassCard {
                            Text(c.name ?: "Customer", fontWeight = FontWeight.Bold)
                            Text(c.phone ?: "")
                            Text(c.defaultAddress ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        }.onSuccess { msg = "Bore saved" }.onFailure { msg = it.message }
                    }
                })
            }
        }
    }
}
