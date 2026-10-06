package com.tankermanager.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore("tanker_session")

/**
 * Session + device metadata only.
 * Never stores MPIN, password, or biometric secrets.
 */
class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val roleKey = stringPreferencesKey("role")
    private val nameKey = stringPreferencesKey("name")
    private val operatorKey = stringPreferencesKey("operator")
    private val phoneKey = stringPreferencesKey("phone")
    private val deviceIdKey = stringPreferencesKey("device_id")
    private val mpinEnabledKey = booleanPreferencesKey("mpin_enabled")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")
    private val unlockedKey = booleanPreferencesKey("session_unlocked")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val role: Flow<String?> = context.dataStore.data.map { it[roleKey] }
    val fullName: Flow<String?> = context.dataStore.data.map { it[nameKey] }
    val operatorName: Flow<String?> = context.dataStore.data.map { it[operatorKey] }
    val phone: Flow<String?> = context.dataStore.data.map { it[phoneKey] }
    val mpinEnabled: Flow<Boolean> = context.dataStore.data.map { it[mpinEnabledKey] == true }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[biometricEnabledKey] == true }
    val sessionUnlocked: Flow<Boolean> = context.dataStore.data.map { it[unlockedKey] == true }

    suspend fun deviceId(): String {
        val existing = context.dataStore.data.map { it[deviceIdKey] }.first()
        if (!existing.isNullOrBlank()) return existing
        val id = UUID.randomUUID().toString().replace("-", "")
        context.dataStore.edit { it[deviceIdKey] = id }
        return id
    }

    suspend fun save(
        token: String,
        role: String?,
        name: String?,
        operator: String?,
        phone: String?,
        mpinEnabled: Boolean? = null,
        biometricEnabled: Boolean? = null,
        unlocked: Boolean = true
    ) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[roleKey] = role.orEmpty()
            it[nameKey] = name.orEmpty()
            it[operatorKey] = operator.orEmpty()
            it[phoneKey] = phone.orEmpty()
            if (mpinEnabled != null) it[mpinEnabledKey] = mpinEnabled
            if (biometricEnabled != null) it[biometricEnabledKey] = biometricEnabled
            it[unlockedKey] = unlocked
        }
    }

    suspend fun setSecurityFlags(mpinEnabled: Boolean?, biometricEnabled: Boolean?) {
        context.dataStore.edit {
            if (mpinEnabled != null) it[mpinEnabledKey] = mpinEnabled
            if (biometricEnabled != null) it[biometricEnabledKey] = biometricEnabled
        }
    }

    suspend fun setUnlocked(unlocked: Boolean) {
        context.dataStore.edit { it[unlockedKey] = unlocked }
    }

    suspend fun clear() {
        val device = context.dataStore.data.map { it[deviceIdKey] }.first()
        context.dataStore.edit {
            it.clear()
            if (!device.isNullOrBlank()) it[deviceIdKey] = device
        }
    }
}
