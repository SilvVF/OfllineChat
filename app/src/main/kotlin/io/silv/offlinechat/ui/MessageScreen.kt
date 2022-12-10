package io.silv.offlinechat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.offlinechat.MainActivityViewModel

@Composable
fun MessageScreen(
    viewModel: MainActivityViewModel
) {
    var text by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TextField(value = text, onValueChange = {
                text = it
            })
            Button(onClick = { viewModel.sendMessageFromClient(text) }) {
                Text(text = "Send Message")
            }
        }
        items(viewModel.messages) {
            Text(text = it, modifier = Modifier.padding(8.dp))
        }
    }
}