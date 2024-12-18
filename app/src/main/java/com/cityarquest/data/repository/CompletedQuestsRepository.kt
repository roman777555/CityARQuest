package com.cityarquest.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class CompletedQuestData(
    val id: String,
    val title: String,
    val difficulty: Int, // Добавьте это поле
    val pointsEarned: Int,
    val spentTimeSec: Long,
    val neighborhood: String
)

class CompletedQuestsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("completed_quests_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCompletedQuests(): List<CompletedQuestData> {
        val json = prefs.getString("completed_quests", "[]")
        val type = object : TypeToken<List<CompletedQuestData>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addCompletedQuestWithDetails(
        quest: com.cityarquest.data.models.Quest,
        points: Int,
        spentTimeSec: Long,
        neighborhood: String
    ) {
        val current = getCompletedQuests().toMutableList()
        if (!current.any { it.id == quest.id }) {
            val data = CompletedQuestData(
                id = quest.id,
                title = quest.title,
                difficulty = quest.difficulty, // Передаём значение difficulty
                pointsEarned = points,
                spentTimeSec = spentTimeSec,
                neighborhood = neighborhood
            )
            current.add(data)
            saveList(current)
        }
    }

    fun addCompletedQuestData(data: CompletedQuestData) {
        val current = getCompletedQuests().toMutableList()
        current.add(data)
        saveList(current)
    }

    private fun saveList(list: List<CompletedQuestData>) {
        val json = gson.toJson(list)
        prefs.edit().putString("completed_quests", json).apply()
    }
}
