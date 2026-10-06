package com.tankermanager.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.tankermanager.app.data.model.BiometricChallengeRequest
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

/** App lock: re-validate MPIN / biometric with backend before entering home. */
@Composable
fun UnlockScreen(
    repo: TankerRepository,
    onUnlocked: (role: String?) -> Unit,
    onUsePassword: () -> Unit
) {
    val phone by repo.session().phone.collectAsState(initial = "")
    val biometricOn by repo.session().biometricEnabled.collectAsState(initial = false)
    var mpin by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? FragmentActivity

    fun unlockWithMpin() {
        error = null
        if (!mpin.matches(Regex("^\\d{4,6}$"))) {
            error = "Enter MPIN"
            return
        }
        loading = true
        scope.launch {
            try {
                val digest = SecureCrypto.mpinDigest(mpin)
                mpin = ""
                val payload = SecureCrypto.encryptObject(
                    "phone" to phone.orEmpty().trim(),
                    "mpin" to digest
                )
                val result = repo.safe { mpinLogin(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess { auth ->
                    repo.session().save(
                        token = auth.token!!,
                        role = auth.role,
                        name = auth.fullName,
                        operator = auth.operatorName,
                        phone = auth.phone,
                        mpinEnabled = auth.mpinEnabled == true,
                        biometricEnabled = auth.biometricEnabled == true,
                        unlocked = true
                    )
                    onUnlocked(auth.role)
                }.onFailure { error = it.message }
            } catch (e: Exception) {
                loading = false
                error = e.message
            }
        }
    }

    fun unlockWithBiometric() {
        if (activity == null) {
            error = "Biometric unavailable"
            return
        }
        loading = true
        scope.launch {
            try {
                val deviceId = repo.session().deviceId()
                val p = phone.orEmpty().trim()
                val challenge = repo.safe {
                    biometricChallenge(BiometricChallengeRequest(p, deviceId))
                }.getOrElse {
                    loading = false
                    error = it.message
                    return@launch
                }
                val signPayload = "${challenge.challengeId}|${challenge.nonce}|$p|$deviceId"
                val signature = BiometricKeyStore.signWithBiometric(activity, signPayload)
                val payload = SecureCrypto.encryptObject(
                    "phone" to p,
                    "deviceId" to deviceId,
                    "challengeId" to challenge.challengeId,
                    "signatureBase64" to signature
                )
                val result = repo.safe { biometricVerify(EncryptedPayloadRequest(payload)) }
                loading = false
                result.onSuccess { auth ->
                    repo.session().save(
                        token = auth.token!!,
                        role = auth.role,
                        name = auth.fullName,
                        operator = auth.operatorName,
                        phone = auth.phone,
                        mpinEnabled = auth.mpinEnabled == true,
                        biometricEnabled = auth.biometricEnabled == true,
                        unlocked = true
                    )
                    onUnlocked(auth.role)
                }.onFailure { error = it.message }
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
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Unlock TankerFlow", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(phone.orEmpty(), color = Color.White.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorBanner(error)
                    SoftField(mpin, { if (it.length <= 6 && it.all(Char::isDigit)) mpin = it }, "MPIN", password = true)
                    PrimaryButton("Unlock with MPIN", loading = loading, onClick = { unlockWithMpin() })
                    if (biometricOn && BiometricKeyStore.hasKey()) {
                        PrimaryButton("Unlock with biometrics", loading = loading, onClick = { unlockWithBiometric() })
                    }
                    TextButton(onClick = onUsePassword) { Text("Use password login") }
                }
            }
        }
    }
}
