package com.shjamolov.mediastreamplayer.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.shjamolov.mediastreamplayer.R

@Composable
fun ParentalControlScreen(viewModel: ParentalControlViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxWidth < 600.dp
    Column(
        Modifier.fillMaxSize().padding(horizontal = if (compact) 20.dp else 64.dp, vertical = if (compact) 16.dp else 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.parental_control), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(if (!state.hasPin || state.changingPin) R.string.create_pin else R.string.enter_pin))
        PinField(pin, compact) { pin = it.filter(Char::isDigit).take(8) }
        if (!state.hasPin || state.changingPin) PinField(confirmation, compact) { confirmation = it.filter(Char::isDigit).take(8) }
        state.message?.let { Text(messageText(it, state.lockRemainingMillis), color = messageColor(it), modifier = Modifier.padding(12.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.unlocked && !state.changingPin) {
                Button(onClick = viewModel::lock) { Text(stringResource(R.string.lock_adult)) }
                Button(onClick = viewModel::startChangePin) { Text(stringResource(R.string.change_pin)) }
            } else {
                Button(onClick = {
                    viewModel.submit(pin, confirmation)
                    pin = ""
                    confirmation = ""
                }, enabled = !state.working && state.lockRemainingMillis == 0L) {
                    Text(stringResource(if (state.hasPin && !state.changingPin) R.string.unlock_adult else R.string.save_pin))
                }
            }
        }
        Text(stringResource(R.string.parental_session_note), Modifier.padding(top = 20.dp), color = Color(0xFF9CB3C5))
    }
    }
}

@Composable
private fun PinField(value: String, compact: Boolean, onChange: (String) -> Unit) {
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        textStyle = TextStyle(color = Color.White, fontSize = 24.sp),
        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(if (compact) 1f else 0.5f)
            .background(Color(0xFF17384B), RoundedCornerShape(10.dp)).padding(16.dp),
        decorationBox = { inner -> if (value.isEmpty()) Text(stringResource(R.string.pin_hint), color = Color.Gray); inner() },
    )
}

@Composable
private fun messageText(message: PinMessage, remaining: Long): String = when (message) {
    PinMessage.INVALID_FORMAT -> stringResource(R.string.pin_invalid)
    PinMessage.MISMATCH -> stringResource(R.string.pin_mismatch)
    PinMessage.CREATED -> stringResource(R.string.pin_created)
    PinMessage.UNLOCKED -> stringResource(R.string.adult_unlocked)
    PinMessage.INCORRECT -> stringResource(R.string.pin_incorrect)
    PinMessage.LOCKED -> stringResource(R.string.pin_locked, ((remaining + 59_999) / 60_000).coerceAtLeast(1))
}

private fun messageColor(message: PinMessage) = if (message == PinMessage.CREATED || message == PinMessage.UNLOCKED) Color(0xFF76E39A) else Color(0xFFFFA5A5)
