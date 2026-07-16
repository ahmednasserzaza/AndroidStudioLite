package com.worldcup.androidstudiolite

import android.app.Application
import com.worldcup.androidstudiolite.data.di.dataModules
import com.worldcup.androidstudiolite.di.appModule
import com.worldcup.androidstudiolite.di.sessionModule
import com.worldcup.androidstudiolite.di.useCaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AslApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AslApplication)
            modules(dataModules + listOf(useCaseModule, sessionModule, appModule))
        }
    }
}
