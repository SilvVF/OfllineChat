package io.silv.offlinechat.di

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import androidx.room.Room
import io.silv.offlinechat.MainActivityViewModel
import io.silv.offlinechat.data.ImageFileRepo
import io.silv.offlinechat.data.datastore.UserSettingsDataStoreRepo
import io.silv.offlinechat.data.datastore.UserSettingsDataStoreRepoImpl
import io.silv.offlinechat.data.datastore.userSettingsDataStore
import io.silv.offlinechat.data.ktor.KtorWebsocketClient
import io.silv.offlinechat.data.ktor.KtorWebsocketServer
import io.silv.offlinechat.data.room.DatabaseRepo
import io.silv.offlinechat.data.room.OfflineChatDatabase
import io.silv.offlinechat.ui.ImageReceiver
import io.silv.offlinechat.wifiP2p.WifiP2pReceiver
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            OfflineChatDatabase::class.java,
            "offline-database"
        ).build()
    }

    factory {
        ImageReceiver(
            get { parametersOf("attachments") }
        )
    }

    factory { parameters ->
        ImageFileRepo(androidContext(), parameters.get() )
    }

    factory {
        DatabaseRepo(get<OfflineChatDatabase>())
    }

    viewModel {
        MainActivityViewModel(
            receiver = get(),
            attachmentReceiver = get(),
            messageImageRepo = get { parametersOf("message-images") },
            ktorWebsocketClient = get(),
            ktorWebsocketServer = get(),
//            db = get()
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

    single<WifiManager> {
        androidApplication().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
            wifiManager = get()
        )
    }

}
