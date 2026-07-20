package com.tankermanager.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.CreateOperatorRequest
import com.tankermanager.app.data.model.OperatorResponse
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.ScreenScaffold
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.theme.LagoonDeep
import com.tankermanager.app.ui.theme.Success
import kotlinx.coroutines.launch

@Composable
fun SuperAdminScreen(repo: TankerRepository, onLogout: () -> Unit) {
    var operators by remember { mutableStateOf<List<OperatorResponse>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("Owner@123") }
    var plan by remember { mutableStateOf("BASIC") }
    var msg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            repo.safe { listOperators() }
                .onSuccess { operators = it }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    ScreenScaffold(
        title = "Super Admin",
        subtitle = "Register operators & owners",
        topBarExtra = {
            TextButton(onClick = onLogout) { Text("Logout") }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ErrorBanner(error)
            if (!msg.isNullOrBlank()) {
                Text(msg!!, color = Success, fontWeight = FontWeight.SemiBold)
            }

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New operator (sell product)", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Creates the business + owner login. Owner later adds managers & drivers.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SoftField(name, { name = it }, "Operator / business name")
                    SoftField(code, { code = it.uppercase() }, "Operator code")
                    SoftField(ownerName, { ownerName = it }, "Owner name")
                    SoftField(ownerPhone, { ownerPhone = it }, "Owner phone (login)")
                    SoftField(ownerPassword, { ownerPassword = it }, "Owner password", password = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BASIC", "PRO", "ENTERPRISE").forEach {
                            FilterChip(selected = plan == it, onClick = { plan = it }, label = { Text(it) })
                        }
                    }
                    PrimaryButton("Register owner", onClick = {
                        error = null
                        msg = null
                        scope.launch {
                            repo.safe {
                                createOperator(
                                    CreateOperatorRequest(
                                        name = name.trim(),
                                        code = code.trim(),
                                        phone = ownerPhone.trim(),
                                        ownerName = ownerName.trim(),
                                        ownerPhone = ownerPhone.trim(),
                                        ownerPassword = ownerPassword,
                                        plan = plan
                                    )
                                )
                            }.onSuccess {
                                msg = "Owner registered. They can login with ${it.phone ?: ownerPhone}"
                                name = ""; code = ""; ownerName = ""; ownerPhone = ""
                                refresh()
                            }.onFailure { error = it.message }
                        }
                    })
                }
            }

            Text("Registered operators", style = MaterialTheme.typography.titleLarge)
            operators.forEach { op ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(op.name ?: "—", fontWeight = FontWeight.Bold)
                            Text("${op.code} • ${op.phone ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Plan: ${op.plan ?: "BASIC"}", color = LagoonDeep)
                        }
                        Text(
                            if (op.active == true) "Active" else "Off",
                            color = if (op.active == true) Success else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
