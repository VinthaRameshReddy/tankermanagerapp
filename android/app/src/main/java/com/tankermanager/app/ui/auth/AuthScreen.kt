package com.tankermanager.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tankermanager.app.data.model.LoginRequest
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GhostButton
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.PulsingTruck
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.WaveBackground
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repo: TankerRepository,
    onLoggedIn: (role: String?) -> Unit,
    onTrackTap: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    WaveBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            PulsingTruck()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "TankerFlow",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Book. Track. Deliver water smarter.",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(28.dp))

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Login with the account created for you. Owners are registered by Super Admin.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ErrorBanner(error)
                    SoftField(phone, { phone = it }, "Phone number")
                    SoftField(password, { password = it }, "Password", password = true)
                    PrimaryButton(
                        text = "Login",
                        loading = loading,
                        onClick = {
                            error = null
                            loading = true
                            scope.launch {
                                val result = repo.safe {
                                    login(LoginRequest(phone.trim(), password))
                                }
                                loading = false
                                result.onSuccess { auth ->
                                    val token = auth.token
                                    if (token.isNullOrBlank()) {
                                        error = "No token returned"
                                        return@onSuccess
                                    }
                                    repo.session().save(
                                        token = token,
                                        role = auth.role,
                                        name = auth.fullName,
                                        operator = auth.operatorName,
                                        phone = auth.phone
                                    )
                                    onLoggedIn(auth.role)
                                }.onFailure { error = it.message }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            GhostButton(
                text = "Track a delivery",
                onClick = onTrackTap,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
