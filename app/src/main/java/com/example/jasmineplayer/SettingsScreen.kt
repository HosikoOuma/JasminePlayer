package com.example.jasmineplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val crossfadeDuration by settingsViewModel.crossfadeDuration.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Crossfade", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = crossfadeDuration.toFloat(),
                onValueChange = { settingsViewModel.setCrossfadeDuration(it.toInt()) },
                valueRange = 0f..10f, // 0 to 10 seconds
                steps = 9,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "${crossfadeDuration}s")
        }
    }
}
