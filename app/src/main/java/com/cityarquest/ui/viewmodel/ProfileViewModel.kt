package com.cityarquest.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import com.cityarquest.data.models.Quest
import com.cityarquest.data.repository.CompletedQuestData
import com.cityarquest.data.repository.CompletedQuestsRepository
import com.cityarquest.data.repository.ProfileRepository

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepo = ProfileRepository(application)
    private val completedRepo = CompletedQuestsRepository(application)

    private val _completedQuests = MutableLiveData<List<CompletedQuestData>>(completedRepo.getCompletedQuests())
    val completedQuests: LiveData<List<CompletedQuestData>> get() = _completedQuests

    private val _userName = MutableLiveData<String>(profileRepo.userName)
    val userName: LiveData<String> get() = _userName

    private val _userPoints = MutableLiveData<Int>(profileRepo.userPoints)
    val userPoints: LiveData<Int> get() = _userPoints

    // Используем MediatorLiveData для генерации значков
    private val _badges = MediatorLiveData<List<String>>()
    val badges: LiveData<List<String>> get() = _badges

    init {
        _badges.addSource(_completedQuests) { updateBadges() }
        _badges.addSource(_userPoints) { updateBadges() }
    }

    private fun updateBadges() {
        val totalPoints = userPoints.value ?: 0
        val completedList = _completedQuests.value ?: emptyList()
        val badgesList = mutableListOf<String>()

        if (completedList.size > 3) badgesList.add("Adventurer")
        if (totalPoints > 100) badgesList.add("Master Explorer")

        _badges.value = badgesList
    }

    fun updateUserName(newName: String) {
        profileRepo.userName = newName
        _userName.value = newName
    }

    fun completeQuest(quest: Quest, points: Int, spentTimeSec: Long, neighborhood: String) {
        val data = CompletedQuestData(
            id = quest.id,
            title = quest.title,
            difficulty = quest.difficulty,
            pointsEarned = points,
            spentTimeSec = spentTimeSec,
            neighborhood = neighborhood
        )
        completedRepo.addCompletedQuestData(data)
        _completedQuests.value = completedRepo.getCompletedQuests()

        val newPoints = profileRepo.userPoints + points
        profileRepo.userPoints = newPoints
        _userPoints.value = newPoints
    }

    fun getAverageDifficulty(): Double {
        val list = _completedQuests.value ?: emptyList()
        if (list.isEmpty()) return 0.0
        return list.map { it.difficulty }.average()
    }
}
