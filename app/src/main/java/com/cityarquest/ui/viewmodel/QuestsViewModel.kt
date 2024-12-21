package com.cityarquest.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cityarquest.R
import com.cityarquest.data.LocalDataSource
import com.cityarquest.data.models.Quest
import kotlinx.coroutines.launch

class QuestsViewModel : ViewModel() {

    // Список всех загруженных квестов
    private val _quests = MutableLiveData<List<Quest>>(emptyList())
    val quests: LiveData<List<Quest>> get() = _quests

    // Один активный квест
    private val _activeQuest = MutableLiveData<Quest?>(null)
    val activeQuest: LiveData<Quest?> get() = _activeQuest

    // Общие очки пользователя (глобальный счёт)
    private val _totalPoints = MutableLiveData<Int>(0)
    val totalPoints: LiveData<Int> get() = _totalPoints

    /**
     * Загрузка квестов через LocalDataSource.
     */
    fun loadQuests(context: Context, userLat: Double, userLon: Double, radiusKm: Double) {
        viewModelScope.launch {
            val apiKey = context.getString(R.string.google_api_key)
            val questsList = LocalDataSource.generateQuests(
                context,
                userLat,
                userLon,
                radiusKm,
                apiKey
            )
            _quests.value = questsList
        }
    }

    /**
     * Обновить список квестов извне (например, из MapFragment).
     */
    fun updateQuests(quests: List<Quest>) {
        _quests.value = quests
    }

    /**
     * Начать квест — делаем квест активным. Если уже был активный, придётся завершить или предупредить пользователя.
     */
    fun startQuest(quest: Quest) {
        _activeQuest.value = quest
    }

    /**
     * Завершить текущий активный квест и начислить очки (если переданы).
     */
    fun completeActiveQuest(pointsEarned: Int = 0) {
        _activeQuest.value = null
        addPoints(pointsEarned)
    }

    /**
     * Увеличить общий счёт пользователя.
     */
    fun addPoints(points: Int) {
        _totalPoints.value = (_totalPoints.value ?: 0) + points
    }
}
