package fr.speekha.httpmocker.demo

import android.app.Application
import fr.speekha.httpmocker.demo.di.injectionModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DemoApplication : Application() {
    override fun onCreate(){
        super.onCreate()
        // start Koin!
        startKoin {
            // declare used Android context
            androidContext(this@DemoApplication)
            // declare modules
            modules(injectionModule)
        }
    }
}