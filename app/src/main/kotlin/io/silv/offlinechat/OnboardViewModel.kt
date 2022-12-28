package io.silv.offlinechat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OnboardViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val currentScreenKey = "current_screen"

    var currentScreen by mutableStateOf<OnboardScreen>(savedStateHandle[currentScreenKey] ?: OnboardScreen.WelcomeScreen)
        private set

    fun navigateToNextScreen(current: OnboardScreen) = viewModelScope.launch {
        currentScreen = when(current) {
            is OnboardScreen.WelcomeScreen  -> OnboardScreen.AccessWifiState
            is OnboardScreen.AccessWifiState -> OnboardScreen.AccessLocation
            else -> OnboardScreen.Done
        }.also { savedStateHandle[currentScreenKey] = it }
    }
}

sealed class OnboardScreen(val index: Int) {
    object WelcomeScreen: OnboardScreen(0)
    object AccessWifiState: OnboardScreen(1)
    object AccessLocation: OnboardScreen(2)
    object Done: OnboardScreen(3)
}

/*
add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
 */