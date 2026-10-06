package com.tankermanager.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.tankermanager.app.data.model.AuthResponse
import com.tankermanager.app.data.model.BiometricChallengeRequest
import com.tankermanager.app.data.model.EncryptedPayloadRequest
import com.tankermanager.app.data.model.LoginRequest
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GhostButton
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.PulsingTruck
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.WaveBackground
import com.tankermanager.app.util.BiometricKeyStore
import com.tankermanager.app.util.SecureCrypto
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repo: TankerRepository,
    onLoggedIn: (role: String?, needsSecuritySetup: Boolean) -> Unit,
    onTrackTap: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableIntStateOf(0) } // 0 password, 1 mpin, 2 biometric
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mpin by remember { mutableStateOf("") }

    fun finishLogin(auth: AuthResponse) {
        scope.launch {
            val token = auth.token
            if (token.isNullOrBlank()) {
                error = "No token returned"
                return@launch
            }
            repo.session().save(
                token = token,
                role = auth.role,
                name = auth.fullName,
                operator = auth.operatorName,
                phone = auth.phone,
                mpinEnabled = auth.mpinEnabled == true,
                biometricEnabled = auth.biometricEnabled == true,
                unlocked = true
            )
            val needsSetup = auth.mpinEnabled != true
            onLoggedIn(auth.role, needsSetup)
        }
    }

    fun doPasswordLogin() {
        error = null
        loading = true
        scope.launch {
            val result = repo.safe { login(LoginRequest(phone.trim(), password)) }
            loading = false
            password = "" // clear from memory ASAP
            result.onSuccess { finishLogin(it) }.onFailure { error = it.message }
        }
    }

    fun doMpinLogin() {
        error = null
        if (!mpin.matches(Regex("^\\d{4,6}$"))) {
            error = "Enter your 4–6 digit MPIN"
            return
        }
        loading = true
        scope.launch {
            try {
                val digest = SecureCrypto.mpinDigest(mpin)
                mpin = "" // never keep raw MPIN
                val payload = SecureCrypto.encryptObject(
                    "phone" to phone.trim(),
                    "mpin" to digest
                )
                val result = repo.safe { mpinLogin(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess { finishLogin(it) }.onFailure { error = it.message }
            } catch (e: Exception) {
                loading = false
                mpin = ""
                error = e.message
            }
        }
    }

    fun doBiometricLogin() {
        error = null
        if (activity == null) {
            error = "Biometric unavailable on this screen"
            return
        }
        if (!BiometricKeyStore.hasKey()) {
            error = "Biometric not set up on this phone — login with password/MPIN first"
            return
        }
        loading = true
        scope.launch {
            try {
                val deviceId = repo.session().deviceId()
                val challenge = repo.safe {
                    biometricChallenge(BiometricChallengeRequest(phone.trim(), deviceId))
                }.getOrElse {
                    loading = false
                    error = it.message
                    return@launch
                }
                val challengeId = challenge.challengeId
                val nonce = challenge.nonce
                if (challengeId.isNullOrBlank() || nonce.isNullOrBlank()) {
                    loading = false
                    error = "Invalid biometric challenge"
                    return@launch
                }
                val signPayload = "$challengeId|$nonce|${phone.trim()}|$deviceId"
                val signature = BiometricKeyStore.signWithBiometric(activity, signPayload)
                val payload = SecureCrypto.encryptObject(
                    "phone" to phone.trim(),
                    "deviceId" to deviceId,
                    "challengeId" to challengeId,
                    "signatureBase64" to signature
                )
                val result = repo.safe { biometricVerify(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess { finishLogin(it) }.onFailure { error = it.message }
            } catch (e: Exception) {
                loading = false
                error = e.message
            }
        }
    }

    WaveBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            PulsingTruck()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "TankerFlow",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Secure login — MPIN & biometrics verified on server",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "MPIN is SHA-256 hashed and AES-encoded. Biometric keys stay in hardware Keystore — nothing secret is stored in the app.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    ErrorBanner(error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Password") })
                        FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("MPIN") })
                        FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Biometric") })
                    }
                    SoftField(phone, { phone = it }, "Phone number")
                    when (mode) {
                        0 -> {
                            SoftField(password, { password = it }, "Password", password = true)
                            PrimaryButton("Login", loading = loading, onClick = { doPasswordLogin() })
                        }
                        1 -> {
                            SoftField(mpin, { if (it.length <= 6 && it.all(Char::isDigit)) mpin = it }, "MPIN (4–6 digits)", password = true)
                            PrimaryButton("Login with MPIN", loading = loading, onClick = { doMpinLogin() })
                        }
                        else -> {
                            Text(
                                "Unlock with fingerprint / face. Validated by backend challenge signature.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrimaryButton("Login with biometrics", loading = loading, onClick = { doBiometricLogin() })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            GhostButton(
                text = "Track a delivery",
                onClick = onTrackTap,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
