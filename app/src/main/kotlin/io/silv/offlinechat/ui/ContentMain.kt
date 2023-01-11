package io.silv.offlinechat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.silv.offlinechat.MainActivityViewModel
import kotlinx.coroutines.delay

@Composable
fun ContentMain(
    viewModel: MainActivityViewModel,
    showToast: (String) -> Unit,
    navigateToMessage: () -> Unit
) {
    val peers = viewModel.peers
    val connectionInfo = viewModel.connectionInfo

    LaunchedEffect(key1 = connectionInfo) {
        if (connectionInfo?.isGroupOwner == true) {
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
    val baseText = "Searching for peer devices"
    var devicesFound by remember {
        mutableStateOf(baseText)
    }
    LaunchedEffect(key1 = true) {
        fun getDots(i: Int): String {
            return when(i) {
                1 -> "."
                2 -> ".."
                else -> "..."
            }
        }
        var i = 0
        while (true) {
            if (i >= 3) {
                i = 0
            }
            delay(250)
            devicesFound = baseText + getDots(i)
            i++
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        item { 
            Text(text = devicesFound, modifier = Modifier.padding(8.dp), fontSize = 22.sp)
        }
        items(peers) {
            DeviceConnectionCard(
                deviceName = it.deviceName,
                deviceAddress = it.deviceAddress,
                deviceType = it.primaryDeviceType,
                onConnectClicked = {viewModel.connectToDevice(it)},
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }
    }
}

