package com.tankermanager.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tankermanager.app.data.model.EncryptedPayloadRequest
import com.tankermanager.app.data.repo.TankerRepository
import com.tankermanager.app.ui.components.ErrorBanner
import com.tankermanager.app.ui.components.GlassCard
import com.tankermanager.app.ui.components.PrimaryButton
import com.tankermanager.app.ui.components.SoftField
import com.tankermanager.app.ui.components.WaveBackground
import com.tankermanager.app.util.BiometricKeyStore
import com.tankermanager.app.util.SecureCrypto
import kotlinx.coroutines.launch

/**
 * After password login: set MPIN (server-side hash only) and optionally register biometric public key.
 */
@Composable
fun SecuritySetupScreen(
    repo: TankerRepository,
    onDone: () -> Unit
) {
    var mpin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(0) } // 0 mpin, 1 biometric offer
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun setMpin() {
        error = null
        if (mpin != confirm) {
            error = "MPINs do not match"
            return
        }
        if (!mpin.matches(Regex("^\\d{4,6}$"))) {
            error = "MPIN must be 4–6 digits"
            return
        }
        loading = true
        scope.launch {
            try {
                val digest = SecureCrypto.mpinDigest(mpin)
                mpin = ""
                confirm = ""
                val payload = SecureCrypto.encryptObject("mpin" to digest)
                val result = repo.safe { setMpin(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess { auth ->
                    repo.session().setSecurityFlags(auth.mpinEnabled == true, auth.biometricEnabled == true)
                    step = 1
                }.onFailure { error = it.message }
            } catch (e: Exception) {
                loading = false
                error = e.message
            }
        }
    }

    fun enableBiometric() {
        error = null
        if (activity == null) {
            error = "Biometric UI unavailable"
            return
        }
        if (!BiometricKeyStore.canAuthenticate(context)) {
            error = "No strong biometric enrolled on this device"
            return
        }
        loading = true
        scope.launch {
            try {
                val publicKey = BiometricKeyStore.createKeyPair()
                // Prove user can unlock key before registering
                BiometricKeyStore.signWithBiometric(
                    activity,
                    "register|${repo.session().deviceId()}",
                    title = "Enable biometric login",
                    subtitle = "Confirm to register this device with the server"
                )
                val deviceId = repo.session().deviceId()
                val payload = SecureCrypto.encryptObject(
                    "deviceId" to deviceId,
                    "publicKeyBase64" to publicKey,
                    "deviceLabel" to android.os.Build.MODEL
                )
                val result = repo.safe { registerBiometric(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess {
                    repo.session().setSecurityFlags(mpinEnabled = true, biometricEnabled = true)
                    onDone()
                }.onFailure {
                    BiometricKeyStore.deleteKey()
                    error = it.message
                }
            } catch (e: Exception) {
                loading = false
                BiometricKeyStore.deleteKey()
                error = e.message
            }
        }
    }

    WaveBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("Secure your account", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "MPIN is verified only on the server (SHA-256 + BCrypt). App never stores it.",
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorBanner(error)
                    if (step == 0) {
                        Text("Create MPIN", fontWeight = FontWeight.SemiBold)
                        SoftField(mpin, { if (it.length <= 6 && it.all(Char::isDigit)) mpin = it }, "New MPIN", password = true)
                        SoftField(confirm, { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it }, "Confirm MPIN", password = true)
                        PrimaryButton("Save MPIN", loading = loading, onClick = { setMpin() })
                        TextButton(onClick = onDone) { Text("Skip for now") }
                    } else {
                        Text("Enable biometric?", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Registers only a public key with the server. Fingerprint/face never leaves this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton("Enable biometrics", loading = loading, onClick = { enableBiometric() })
                        TextButton(onClick = onDone) { Text("Not now") }
                    }
                }
            }
        }
    }
}
