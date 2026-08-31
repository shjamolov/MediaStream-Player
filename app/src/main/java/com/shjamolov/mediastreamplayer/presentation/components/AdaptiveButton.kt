package com.shjamolov.mediastreamplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shjamolov.mediastreamplayer.presentation.theme.AppAccent
import com.shjamolov.mediastreamplayer.presentation.theme.AppSurfaceRaised

@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val background = when {
        !enabled -> AppSurfaceRaised.copy(alpha = .45f)
        selected -> AppAccent
        else -> AppSurfaceRaised
    }
    Box(
        modifier = modifier.onFocusChanged { focused = it.hasFocus }
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else if (selected) AppAccent else Color(0xFF355064), shape)
            .clip(shape).background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled).padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
