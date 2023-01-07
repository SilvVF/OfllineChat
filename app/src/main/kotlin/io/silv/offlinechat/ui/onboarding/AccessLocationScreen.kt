package io.silv.offlinechat.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccessLocationScreen(
    permissionsState: MultiplePermissionsState,
    navigate: () -> Unit
) {

    val permissionList = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val permissions = remember(permissionsState) {
        derivedStateOf {
            permissionsState.permissions.filter { it.permission in permissionList }
        }
    }.value

    LaunchedEffect(key1 = permissions) {
        if (permissions.all { it.status.isGranted }) {
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
                permissions.forEach { it.launchPermissionRequest() }
            }
        ) {
           Text("Next ->")
        }
    }
}