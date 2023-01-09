package io.silv.offlinechat.di

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.data.ImageFileRepo
import io.silv.offlinechat.data.datastore.UserSettingsDataStoreRepo
import io.silv.offlinechat.data.datastore.UserSettingsDataStoreRepoImpl
import io.silv.offlinechat.data.datastore.userSettingsDataStore
import io.silv.offlinechat.data.ktor.KtorWebsocketClient
import io.silv.offlinechat.data.ktor.KtorWebsocketServer
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {

    factory {
        ImageReceiver(
            get { parametersOf("attachments") }
        )
    }

    factory { parameters ->
        ImageFileRepo(androidContext(), parameters.get() )
    }

    viewModel {
        MainActivityViewModel(
            receiver = get(),
            attachmentReceiver = get(),
            messageImageRepo = get { parametersOf("message-images") },
            ktorWebsocketClient = get(),
            ktorWebsocketServer = get()
        )
    }
}

val dataModule = module {
    singleOf(::KtorWebsocketServer)
    singleOf(::KtorWebsocketClient)
    single<UserSettingsDataStoreRepo> {
        UserSettingsDataStoreRepoImpl(
            store = androidContext().userSettingsDataStore
        )
    }
}

val wifiP2pModule = module {

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

}
