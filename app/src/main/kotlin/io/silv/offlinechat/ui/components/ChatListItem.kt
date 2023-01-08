package io.silv.offlinechat.ui.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.silv.offlinechat.Chat

@Composable
fun ChatListItem(it: Chat) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    when(it) {
        is Chat.ReceivedImage -> {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                AndroidView(
                    modifier = Modifier.size(100.dp),
                    factory = { context ->
                        ImageView(context).apply {
                            setImageURI(it.uri)
                        }
                    },
                    update = { iv ->
                        iv.setImageURI(it.uri)
                    }
                )
            }
        }
        is Chat.ReceivedMessage-> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                ChatMessage(
                    modifier = Modifier.widthIn(10.dp, (screenWidth * 0.6).dp),
                    isReceived = true,
                    message = it.s
                )
            }
        }
        is Chat.SentImage -> {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                AndroidView(
                    modifier = Modifier.size(100.dp),
                    factory = { context ->
                        ImageView(context).apply {
                            setImageURI(it.uri)
                        }
                    },
                    update = { iv ->
                        iv.setImageURI(it.uri)
                    }
                )
            }
        }
        is Chat.SentMessage -> {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp), contentAlignment = Alignment.CenterEnd) {
                ChatMessage(
                    modifier = Modifier.widthIn(10.dp, (screenWidth * 0.6).dp),
                    isReceived = false,
                    message = it.s
                )
            }
        }
    }
}

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