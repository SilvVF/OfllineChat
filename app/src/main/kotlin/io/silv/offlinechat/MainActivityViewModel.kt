package io.silv.offlinechat

import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.silv.offlinechat.data.*
import io.silv.offlinechat.data.room.UriAsStringSerializer
import io.silv.offlinechat.repositories.MessageRepo
import io.silv.offlinechat.ui.ImageReceiver
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

    val connectionInfo = receiver.wifiP2pInfo.asStateFlow()

    private val mutableSideEffectChannel = Channel<String>()
    val sideEffects = mutableSideEffectChannel.receiveAsFlow()

    val peers by mutableStateOf<List<WifiP2pDevice>>(emptyList())
    var messages by mutableStateOf(emptyList<Chat>())

    var isServer by mutableStateOf<Boolean>(false)
        private set
    init {
        listenForConnection()
        viewModelScope.launch {
            messages.asFlow().collect {
                println(it)
            }
        }
    }

    private fun listenForConnection() = viewModelScope.launch {
        connectionInfo.collectLatest {
            it?.let { info ->
               val ownerAddress = info.groupOwnerAddress?.hostAddress ?: return@collectLatest
               messageRepo.startListeningForMessages(info.isGroupOwner, ownerAddress) { data ->
                   receivedLocalData(data)
               }
            }
        }
    }

    private suspend fun receivedLocalData(localData: LocalData) {
        when(localData) {
            is Message -> {
                messages = buildList {
                    add(Chat.ReceivedMessage(localData.content, System.currentTimeMillis()))
                    addAll(messages)
                }
            }
            is LocalImage -> {
                messages = buildList {
                    add(Chat.ReceivedImage(localData.uri, localData.time))
                    addAll(messages)
                }
            }
        }
    }

    fun sendMessageUsingKtor(message: String) = viewModelScope.launch {
        messageRepo.sendMessage(message, isServer, attachmentReceiver) { sentChats ->
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
