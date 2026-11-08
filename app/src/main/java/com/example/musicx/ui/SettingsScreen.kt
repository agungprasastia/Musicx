package com.example.musicx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.musicx.scrobble.ScrobbleState

@Composable
fun SettingsScreen(
    scrobble: ScrobbleState,
    onConnectListenBrainz: (String) -> Unit,
    onDisconnectListenBrainz: () -> Unit,
    onBack: () -> Unit,
) {
    // Not rememberSaveable: the token must not land in the saved-instance-state bundle.
    var token by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onBack),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "← Back",
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            "Settings",
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        SectionHeading("ListenBrainz")
        when (scrobble) {
            is ScrobbleState.Connected -> {
                Text("Scrobbling as ${scrobble.userName}", color = MaterialTheme.colorScheme.onBackground)
                scrobble.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDisconnectListenBrainz) { Text("Disconnect ListenBrainz") }
            }

            ScrobbleState.Validating -> Text("Checking token…", color = MaterialTheme.colorScheme.onSurfaceVariant)

            ScrobbleState.Disconnected, is ScrobbleState.Error -> {
                Text(
                    "Paste your user token from listenbrainz.org/settings. Musicx stores it encrypted on this device and sends it only to api.listenbrainz.org.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                (scrobble as? ScrobbleState.Error)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("User token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                )
                Button(
                    onClick = {
                        onConnectListenBrainz(token)
                        token = ""
                    },
                    enabled = token.isNotBlank(),
                ) { Text("Connect ListenBrainz") }
            }
        }

        SectionHeading("Not available")
        Text(
            "Last.fm needs a registered API key. Discord presence needs a Discord application. Neither is configured in this build.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
