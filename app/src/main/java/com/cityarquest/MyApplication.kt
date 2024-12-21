package com.cityarquest

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class MyApplication : Application() {
    lateinit var placesClient: PlacesClient

    override fun onCreate() {
        super.onCreate()
        // Инициализация Places SDK
        val apiKey = getString(R.string.google_api_key)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)
    }
}
