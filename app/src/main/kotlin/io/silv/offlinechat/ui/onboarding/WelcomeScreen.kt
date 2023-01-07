package io.silv.offlinechat.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeScreen(
    navigate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        val permissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_NETWORK_STATE)
        
        Text(text = "Welcome Screen")
        LaunchedEffect(key1 = true) {
            
        }
        Button(onClick = {
            navigate()
        }) {
           Text("Next ->")
        }
    }
}