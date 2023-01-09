package io.silv.offlinechat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.ui.components.ChatBox
import io.silv.offlinechat.ui.components.ChatListItem


@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {

    Box(modifier = Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(viewModel.messages) {
                ChatListItem(it = it)
            }
        }
        ChatBox(
            modifier = Modifier.fillMaxWidth().heightIn(0.dp, 200.dp),
            onSend = { message ->
                viewModel.sendMessageUsingKtor(message)
            },
            imageReceiver = viewModel.attachmentReceiver,
        )
    }
}


