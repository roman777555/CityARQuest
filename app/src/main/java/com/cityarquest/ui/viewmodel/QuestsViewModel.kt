package com.cityarquest.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cityarquest.data.models.Quest
import com.cityarquest.data.repository.QuestRepository
import kotlinx.coroutines.launch

class QuestsViewModel : ViewModel() {
    private val repository = QuestRepository()

    private val _quests = MutableLiveData<List<Quest>>()
    val quests: LiveData<List<Quest>> get() = _quests

    fun loadQuests() {
        if (_quests.value.isNullOrEmpty()) {
            viewModelScope.launch {
                try {
                    val result = repository.fetchQuests()
                    _quests.value = result
                } catch (e: Exception) {
                    _quests.value = emptyList()
                }
            }
        }
    }
}
