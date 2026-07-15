package com.tankermanager.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("tanker_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val roleKey = stringPreferencesKey("role")
    private val nameKey = stringPreferencesKey("name")
    private val operatorKey = stringPreferencesKey("operator")
    private val phoneKey = stringPreferencesKey("phone")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val role: Flow<String?> = context.dataStore.data.map { it[roleKey] }
    val fullName: Flow<String?> = context.dataStore.data.map { it[nameKey] }
    val operatorName: Flow<String?> = context.dataStore.data.map { it[operatorKey] }
    val phone: Flow<String?> = context.dataStore.data.map { it[phoneKey] }

    suspend fun save(
        token: String,
        role: String?,
        name: String?,
        operator: String?,
        phone: String?
    ) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[roleKey] = role.orEmpty()
            it[nameKey] = name.orEmpty()
            it[operatorKey] = operator.orEmpty()
            it[phoneKey] = phone.orEmpty()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
