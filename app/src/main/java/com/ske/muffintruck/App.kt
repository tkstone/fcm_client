package com.ske.muffintruck

import androidx.multidex.MultiDexApplication
import com.ske.muffintruck.di.database
import com.ske.muffintruck.di.json
import com.ske.muffintruck.di.main
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


@Suppress("unused")
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(listOf(main, json, database))
        }
    }

}
