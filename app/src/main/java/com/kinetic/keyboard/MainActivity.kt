package com.kinetic.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kinetic.keyboard.data.KeyboardPrefs
import com.kinetic.keyboard.data.PrefsRepository
import com.kinetic.keyboard.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/** Setup + settings screen (SPEC.md P5.1). Declared as the IME's settingsActivity. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefsRepo = PrefsRepository(applicationContext)
        setContent {
            MaterialTheme {
                Surface {
                    SettingsScreen(
                        prefsRepo = prefsRepo,
                        onEnable = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                        onChoose = {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    prefsRepo: PrefsRepository,
    onEnable: () -> Unit,
    onChoose: () -> Unit,
) {
    val prefs by prefsRepo.prefs.collectAsState(initial = KeyboardPrefs())
    val scope = rememberCoroutineScope()
    var testText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)

        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_enable))
        }
        Button(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.setup_choose))
        }
        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            label = { Text(stringResource(R.string.setup_test_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()
        Text("Appearance", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = prefs.themeMode == mode,
                    onClick = { scope.launch { prefsRepo.setThemeMode(mode) } },
                    label = {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                        )
                    },
                )
            }
        }

        Text("Key height: ${prefs.keyHeightDp} dp")
        Slider(
            value = prefs.keyHeightDp.toFloat(),
            onValueChange = { scope.launch { prefsRepo.setKeyHeight(it.toInt()) } },
            valueRange = KeyboardPrefs.MIN_KEY_HEIGHT.toFloat()..KeyboardPrefs.MAX_KEY_HEIGHT.toFloat(),
        )

        HorizontalDivider()
        Text("Feedback", style = MaterialTheme.typography.titleMedium)

        SettingSwitch("Vibrate on keypress", prefs.haptics) {
            scope.launch { prefsRepo.setHaptics(it) }
        }
        SettingSwitch("Sound on keypress", prefs.sound) {
            scope.launch { prefsRepo.setSound(it) }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
