package io.silv.offlinechat

import android.app.Application
import io.silv.offlinechat.di.appModule
import io.silv.offlinechat.di.dataModule
import io.silv.offlinechat.di.wifiP2pModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class OfflineChatApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@OfflineChatApplication)
            // Load modules
            modules(appModule, wifiP2pModule, dataModule)
        }
    }
}