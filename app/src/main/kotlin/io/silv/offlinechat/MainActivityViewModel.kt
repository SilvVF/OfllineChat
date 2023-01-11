package io.silv.offlinechat

import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silv.offlinechat.data.*
import io.silv.offlinechat.data.room.UriAsStringSerializer
import io.silv.offlinechat.repositories.MessageRepo
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.P2pEvent
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable


class MainActivityViewModel(
    private val receiver: WifiP2pReceiver,
    val attachmentReceiver: ImageReceiver,
    private val messageRepo: MessageRepo
    //private val db: DatabaseRepo
): ViewModel() {

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()

    var peers by mutableStateOf<List<WifiP2pDevice>>(emptyList())
        private set
    var messages by mutableStateOf(emptyList<Chat>())
        private set
    var connectionInfo by mutableStateOf<WifiP2pInfo?>(null)
        private set

    private var isServer by mutableStateOf<Boolean?>(null)
    init {
        viewModelScope.launch {
            receiver.eventReceiveFlow.collect { handleP2pEvent(it) }
        }
    }

    private suspend fun handleP2pEvent(event: P2pEvent) {
        when (event) {
            is P2pEvent.PeersChanged -> { peers = event.peers }
            is P2pEvent.ConnectionChanged -> handleConnectionChange(event.p2pInfo, event.networkInfo)
            is P2pEvent.DeviceChanged -> {}
            is P2pEvent.Error -> {}
            is P2pEvent.StateChanged -> {
                if (event.enabled) receiver.refreshPeers {
                    mutableSideEffectChannel.send("peers refreshed")
                }
            }
        }
    }


    private fun handleConnectionChange(wifiP2pInfo: WifiP2pInfo?, networkInfo: NetworkInfo?) = viewModelScope.launch {
        if (networkInfo?.isConnected == true && isServer == null) {
            wifiP2pInfo.also { connectionInfo = it }?.let { info ->
                val ownerAddress = info.groupOwnerAddress.hostAddress ?: throw IllegalStateException()
                isServer = wifiP2pInfo?.isGroupOwner == true
                messageRepo.startListeningForMessages(isServer ?: false, ownerAddress) { data ->
                    receivedLocalData(data)
                }
            } ?: run { isServer = null }
        } else run { isServer = null }
    }

    private suspend fun receivedLocalData(localData: LocalData) {
        when(localData) {
            is Message -> {
                messages = buildList {
                    addAll(messages)
                    add(Chat.ReceivedMessage(localData.content, System.currentTimeMillis()))
                }
            }
            is LocalImage -> {
                messages = buildList {
                    addAll(messages)
                    add(Chat.ReceivedImage(localData.uri, localData.time))
                }
            }
        }
    }

    fun sendMessageUsingKtor(message: String) = viewModelScope.launch {
        messageRepo.sendMessage(message, isServer ?: false, attachmentReceiver) { sentChats ->
           messages = messages + sentChats
        }
    }

    fun connectToDevice(device: WifiP2pDevice) = viewModelScope.launch {
        receiver.connectToDevice(
            device,
            onFailure = {},
            onSuccess = {}
        )
    }
}
sealed interface Chat {
    @Serializable
    data class SentMessage(val s: String, val time: Long): Chat
    @Serializable
    data class ReceivedMessage(val s: String, val time: Long): Chat
    @Serializable
    data class SentImage(
        @Serializable(UriAsStringSerializer::class) val uri: Uri,
        val time: Long
    ): Chat
    @Serializable
    data class ReceivedImage(
        @Serializable(UriAsStringSerializer::class) val uri: Uri,
        val time: Long
    ): Chat
}
