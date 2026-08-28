package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classflow_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val SCHOOL_START_TIME_KEY = stringPreferencesKey("school_start_time")
        val SILENCER_ENABLED_KEY = booleanPreferencesKey("silencer_enabled")
        val FIRST_PERIOD_TIME_KEY = stringPreferencesKey("first_period_time")
        val SECOND_PERIOD_TIME_KEY = stringPreferencesKey("second_period_time")
        val ONBOARDING_FINISHED_KEY = booleanPreferencesKey("onboarding_finished")
        val NEPALI_DATE_OFFSET_KEY = androidx.datastore.preferences.core.intPreferencesKey("nepali_date_offset")
        val WEEKLY_HOLIDAYS_KEY = androidx.datastore.preferences.core.stringSetPreferencesKey("weekly_holidays")
    }

    val weeklyHolidays: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[WEEKLY_HOLIDAYS_KEY] ?: emptySet()
    }

    suspend fun saveWeeklyHolidays(holidays: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[WEEKLY_HOLIDAYS_KEY] = holidays
        }
    }

    val nepaliDateOffset: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[NEPALI_DATE_OFFSET_KEY] ?: 0
    }

    val onboardingFinished: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_FINISHED_KEY] ?: false
    }

    val schoolStartTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCHOOL_START_TIME_KEY] ?: "08:00"
    }

    val silencerEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SILENCER_ENABLED_KEY] ?: false
    }

    val firstPeriodTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FIRST_PERIOD_TIME_KEY] ?: "08:30"
    }

    val secondPeriodTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SECOND_PERIOD_TIME_KEY] ?: "09:20"
    }

    suspend fun saveSchoolStartTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[SCHOOL_START_TIME_KEY] = time
        }
    }

    suspend fun saveSilencerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENCER_ENABLED_KEY] = enabled
        }
    }

    suspend fun saveFirstPeriodTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_PERIOD_TIME_KEY] = time
        }
    }

    suspend fun saveSecondPeriodTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[SECOND_PERIOD_TIME_KEY] = time
        }
    }

    suspend fun saveOnboardingFinished(finished: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_FINISHED_KEY] = finished
        }
    }

    suspend fun saveNepaliDateOffset(offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[NEPALI_DATE_OFFSET_KEY] = offset
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
