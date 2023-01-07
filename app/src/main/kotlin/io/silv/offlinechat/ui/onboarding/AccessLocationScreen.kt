package io.silv.offlinechat.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccessLocationScreen(
    navigate: () -> Unit
) {

    val permissionList = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val permissions = rememberMultiplePermissionsState(permissions = permissionList)
    LaunchedEffect(key1 = permissions) {
        if (permissions.permissions.any { it.status.isGranted }) {
            navigate()
        }
    }

    LaunchedEffect (true) {
        if (permissionList.isEmpty()) {
            navigate()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Access Location Screen")
        Button(
            onClick = {
                permissions.launchMultiplePermissionRequest()
            }
        ) {
           Text("Next ->")
        }
    }
}