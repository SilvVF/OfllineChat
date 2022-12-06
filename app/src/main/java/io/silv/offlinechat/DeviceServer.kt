package io.silv.offlinechat

import android.net.wifi.p2p.WifiP2pInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class DeviceServer(
    private val info: WifiP2pInfo
) {
    val dataChannel = MutableSharedFlow<String>()
    private val serverSocket = ServerSocket(23479)

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        serverSocket.use {
            val client = serverSocket.accept()
            val stringData = client.getInputStream()
                .readBytes()
                .decodeToString()
            dataChannel.emit(stringData)
        }
    }
}

class DeviceClient(
    private val info: WifiP2pInfo,
) {

    val len: Int = 0
    private val socket = Socket()

    suspend operator fun invoke() = withContext(Dispatchers.IO) {

        val buf = ByteArray(1024)
        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null)
            socket.connect(
                InetSocketAddress(
                    info.groupOwnerAddress.hostAddress as String,
                    23479
            ), 500)
            //send data to the server device
            val outputStream = socket.getOutputStream()
        } finally {
            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */
            socket.takeIf { it.isConnected }?.apply {
                close()
            }
        }
    }
}