package com.cityarquest.data.repository

import android.content.Context
import com.cityarquest.data.LocalDataSource
import com.cityarquest.data.models.Quest
import com.cityarquest.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuestRepository(private val context: Context) {
    suspend fun fetchQuests(userLat: Double, userLon: Double, searchRadiusKm: Double): List<Quest> = withContext(Dispatchers.IO) {
        val apiKey = context.getString(R.string.google_api_key)
        LocalDataSource.generateQuests(context, userLat, userLon, searchRadiusKm, apiKey)
    }
}
