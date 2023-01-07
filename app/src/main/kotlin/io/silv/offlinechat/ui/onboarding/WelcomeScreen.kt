package io.silv.offlinechat.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    multiplePermissionsState: MultiplePermissionsState,
    navigate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        
        Text(text = "Welcome Screen")
        LaunchedEffect(key1 = multiplePermissionsState.allPermissionsGranted) {
            navigate()
        }
        Button(onClick = {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }) {
           Text("Next ->")
        }
    }
}