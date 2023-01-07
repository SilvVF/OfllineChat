package io.silv.offlinechat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatMessage(
    modifier: Modifier = Modifier,
    isReceived: Boolean,
    message: String
) {
    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topEnd = 12.dp,
                    topStart = 12.dp,
                    bottomEnd = if (!isReceived) 0.dp else 12.dp,
                    bottomStart = if (isReceived) 0.dp else 12.dp
                )
            )
            .background(
                if (isReceived) Color(0xffe2e8f0) else Color(0xff38bdf8)
            )
            .padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(text = message, color = if (isReceived) MaterialTheme.colors.onSurface else MaterialTheme.colors.surface)
    }
}