package io.silv.offlinechat.di

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.data.ImageFileRepo
import io.silv.offlinechat.data.KtorWebsocketClient
import io.silv.offlinechat.data.KtorWebsocketServer
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<WifiP2pManager> {
        androidApplication().getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }


    single<WifiP2pManager.Channel> {
        get<WifiP2pManager>()
            .initialize(
                /*srcContext =*/androidContext(),
                /*srcLooper =*/androidContext().mainLooper,
                /*listener =*/null)
    }

    single {
        WifiP2pReceiver(
            manager = get(),
            channel = get(),
        )
    }


    single {
        ImageReceiver(ImageFileRepo(androidContext()))
    }


    viewModel {

        val repoForMessages = ImageFileRepo(androidContext(), "message_images")

        MainActivityViewModel(
            receiver = get(),
            imageReceiver = get(),
            imageRepoForMessages = repoForMessages,
            ktorWebsocketServer = KtorWebsocketServer(repoForMessages),
            ktorWebsocketClient = KtorWebsocketClient(repoForMessages)
        )
    }
}

val activityModule = module {

}