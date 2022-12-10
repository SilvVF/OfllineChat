package io.silv.offlinechat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.silv.offlinechat.MainActivityViewModel

@Composable
fun ContentMain(
    viewModel: MainActivityViewModel,
    showToast: (String) -> Unit,
    navigateToMessage: () -> Unit
) {
    val peers by viewModel.peers.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()

    LaunchedEffect(key1 = connectionInfo) {
        if (connectionInfo?.groupFormed == true) {
            navigateToMessage()
        }
    }

    LaunchedEffect(key1 = peers) {
        showToast("Peers Refreshed")
    }

    LaunchedEffect(key1 = true) {
        viewModel.sideEffects.collect {
            showToast(it)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column {
                Text(text = "Connection Info")
                if (connectionInfo != null) {
                    Text(text = connectionInfo.toString())
                }
            }
        }
        item {
            Button(onClick = {  }) {
                Text(text = "Connect")
            }
        }
        items(peers) {
            PeerListItem(
                device = it,
                modifier = Modifier
                    .clickable {
                        viewModel.connectToDevice(it)
                    }
                    .fillMaxWidth(0.75f)
                    .wrapContentHeight()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

