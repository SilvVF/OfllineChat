@file:OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)

package io.silv.offlinechat.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import io.silv.offlinechat.OnboardScreen
import io.silv.offlinechat.OnboardViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun OnboardScreen(
    permissionsState: MultiplePermissionsState,
    viewModel: OnboardViewModel = getViewModel<OnboardViewModel>(),
    finishedOnboard: () -> Unit,
) {
    AnimatedContent(
        targetState = viewModel.currentScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (this.targetState.index < this.initialState.index) {
                slideInHorizontally() with slideOutHorizontally()
            } else {
                slideInHorizontally { offsetX ->
                    -offsetX
                } with slideOutHorizontally()
            }
        }
    ) {
        when (it) {
            OnboardScreen.WelcomeScreen -> WelcomeScreen {
                viewModel.navigateToNextScreen(OnboardScreen.WelcomeScreen)
            }
            OnboardScreen.AccessWifiState -> AccessWifiScreen(permissionsState) {
                viewModel.navigateToNextScreen(OnboardScreen.AccessWifiState)
            }
            OnboardScreen.AccessLocation -> AccessLocationScreen(permissionsState) {
                viewModel.navigateToNextScreen(OnboardScreen.Done)
            }
            OnboardScreen.Done -> finishedOnboard()
        }
    }
}