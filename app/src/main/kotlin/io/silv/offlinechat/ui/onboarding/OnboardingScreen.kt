@file:OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)

package io.silv.offlinechat.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@Composable
fun OnboardScreen(
    multiplePermissionsState: MultiplePermissionsState,
    finishedOnboard: () -> Unit,
) {
    PermissionRequestScreen(
        multiplePermissionsState = multiplePermissionsState,
        navigate = { finishedOnboard() }
    )
}