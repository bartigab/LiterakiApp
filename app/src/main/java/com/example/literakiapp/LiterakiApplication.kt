package com.example.literakiapp

import android.app.Application
import android.content.Context
import com.example.literakiapp.data.AppContainer

class LiterakiApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LiterakiApplication).appContainer

