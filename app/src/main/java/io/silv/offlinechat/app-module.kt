package io.silv.offlinechat

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
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
            get(),
            get(),
        )
    }

    viewModel {
        MainActivityViewModel(
            get(), get(), get()
        )
    }
}

val activityModule = module {

}