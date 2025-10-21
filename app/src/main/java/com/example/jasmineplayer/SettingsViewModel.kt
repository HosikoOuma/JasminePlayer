package com.example.jasmineplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepository = SettingsRepository.getInstance(app)

    val crossfadeDuration: StateFlow<Int> = settingsRepository.crossfadeDuration

    fun setCrossfadeDuration(duration: Int) {
        settingsRepository.setCrossfadeDuration(duration)
    }
}
