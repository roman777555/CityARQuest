package com.cityarquest.data.repository

import com.cityarquest.data.LocalDataSource
import com.cityarquest.data.models.Quest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuestRepository {
    suspend fun fetchQuests(): List<Quest> = withContext(Dispatchers.IO) {
        LocalDataSource.getQuests()
    }
}
