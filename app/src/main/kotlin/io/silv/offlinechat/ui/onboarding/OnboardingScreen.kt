@file:OptIn(ExperimentalAnimationApi::class)

package io.silv.offlinechat.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.silv.offlinechat.OnboardScreen
import io.silv.offlinechat.OnboardViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun OnboardScreen(
    viewModel: OnboardViewModel = getViewModel<OnboardViewModel>(),
    finishedOnboard: () -> Unit
) {

    var prevIdx by remember {
        mutableStateOf(0)
    }

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
            OnboardScreen.AccessWifiState -> AccessWifiScreen {
                viewModel.navigateToNextScreen(OnboardScreen.AccessWifiState)
            }
            OnboardScreen.AccessLocation -> AccessLocationScreen {
                viewModel.navigateToNextScreen(OnboardScreen.Done)
            }
            OnboardScreen.Done -> finishedOnboard()
        }
    }
}