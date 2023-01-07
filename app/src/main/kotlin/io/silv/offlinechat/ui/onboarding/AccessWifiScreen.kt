package io.silv.offlinechat.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccessWifiScreen(
    navigate: () -> Unit
) {

    val permissionList = remember {
        buildList {
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    val permissions = rememberMultiplePermissionsState(permissions = permissionList)

    LaunchedEffect(key1 = permissions) {
        if(permissions.allPermissionsGranted) {
            navigate()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Access Wifi Screen")
        Button(
            onClick = {
                permissions.launchMultiplePermissionRequest()
            }
        ) {
           Text("Next ->")
        }
    }
}