package com.example.jasmineplayer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _crossfadeDuration = MutableStateFlow(prefs.getInt("crossfade_duration", 0))
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration

    fun setCrossfadeDuration(duration: Int) {
        _crossfadeDuration.value = duration
        prefs.edit().putInt("crossfade_duration", duration).apply()
    }

    companion object {
        @Volatile private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
