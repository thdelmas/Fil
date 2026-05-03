package com.fil.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.contributionDataStore: DataStore<Preferences> by preferencesDataStore(name = "contribution")

class ContributionPreferences(private val context: Context) {

    private val lastPromptKey = longPreferencesKey("last_prompt_timestamp")

    val lastPromptTimestamp: Flow<Long> = context.contributionDataStore.data.map { prefs ->
        prefs[lastPromptKey] ?: 0L
    }

    suspend fun markPromptShown(now: Long = System.currentTimeMillis()) {
        context.contributionDataStore.edit { it[lastPromptKey] = now }
    }

    suspend fun initializeIfFirstLaunch(now: Long = System.currentTimeMillis()) {
        context.contributionDataStore.edit { prefs ->
            if (prefs[lastPromptKey] == null) {
                prefs[lastPromptKey] = now
            }
        }
    }

    companion object {
        const val CADENCE_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
