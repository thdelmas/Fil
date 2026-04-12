package com.fil.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "emergency")

class EmergencyPreferences(private val context: Context) {

    private val contactsKey = stringPreferencesKey("contacts_json")
    private val messageKey = stringPreferencesKey("sms_message")
    private val enabledKey = booleanPreferencesKey("fall_detection_enabled")
    private val countdownKey = intPreferencesKey("countdown_seconds")

    val contacts: Flow<List<EmergencyContact>> = context.dataStore.data.map { prefs ->
        val json = prefs[contactsKey] ?: "[]"
        parseContacts(json)
    }

    val smsMessage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[messageKey] ?: context.getString(
            com.fil.app.R.string.emergency_sms_default
        )
    }

    val fallDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[enabledKey] ?: false
    }

    val countdownSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[countdownKey] ?: 30
    }

    suspend fun saveContacts(contacts: List<EmergencyContact>) {
        val jsonArray = JSONArray()
        for (contact in contacts) {
            jsonArray.put(JSONObject().apply {
                put("name", contact.name)
                put("phone", contact.phone)
                put("isPrimary", contact.isPrimary)
            })
        }
        context.dataStore.edit { it[contactsKey] = jsonArray.toString() }
    }

    suspend fun saveSmsMessage(message: String) {
        context.dataStore.edit { it[messageKey] = message }
    }

    suspend fun setFallDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        context.dataStore.edit { it[countdownKey] = seconds }
    }

    private fun parseContacts(json: String): List<EmergencyContact> {
        val list = mutableListOf<EmergencyContact>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                EmergencyContact(
                    name = obj.getString("name"),
                    phone = obj.getString("phone"),
                    isPrimary = obj.optBoolean("isPrimary", false),
                )
            )
        }
        return list
    }
}
