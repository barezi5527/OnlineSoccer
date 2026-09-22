package com.onlinesoccer.app.feature.auth.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlinesoccer.app.R
import com.onlinesoccer.app.ui.AppViewModel

@Composable
fun LoginScreen(
    viewModel: AppViewModel = viewModel(),
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val loggingIn by viewModel.loggingIn.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val serverUnavailable by viewModel.serverUnavailable.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val labelColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .aspectRatio(1f, matchHeightConstraintsFirst = false),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Online Soccer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Anmeldung beim Server (os.ongapo.com)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = labelColor,
            unfocusedTextColor = labelColor,
            focusedLabelColor = labelColor,
            unfocusedLabelColor = labelColor,
            cursorColor = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("E-Mail") },
            singleLine = true,
            enabled = !loggingIn,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Passwort") },
            singleLine = true,
            enabled = !loggingIn,
            colors = fieldColors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    viewModel.login()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (serverUnavailable) {
            Spacer(Modifier.height(12.dp))
            ServerNichtErreichbarHinweis()
        } else {
            loginError?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::login,
            enabled = !loggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (loggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Anmelden", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = viewModel::guestLogin,
            enabled = !loggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Als Gast erkunden (Demo)", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Deine Zugangsdaten verlassen das Gerät nur verschlüsselt zum Server.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Hinweis, wenn die Anmeldung nicht möglich ist, weil die Website gerade nicht
 * erreichbar ist (z. B. laufender ZAT-Spieltag oder Störung). Farben stammen aus
 * dem Material-Theme und passen sich damit automatisch dem Darkmode an.
 */
@Composable
private fun ServerNichtErreichbarHinweis() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Website derzeit nicht erreichbar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "os.ongapo.com ist gerade nicht erreichbar. Das kann an einem " +
                        "laufenden ZAT-Spieltag, an einer Störung oder an deiner " +
                        "Internetverbindung liegen. Bitte versuche es später erneut.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}