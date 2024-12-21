package com.cityarquest.data.models

data class Quest(
    val id: String,
    val title: String,
    val type: String,
    val description: String,
    val difficulty: Int,
    val latitude: Double,
    val longitude: Double,
    val isSuperQuest: Boolean,
    val points: Int,
    // Новые поля:
    val fullDescription: String?, // Может быть null
    val arHints: String?,         // Подсказки для AR
    val imageUrl: String?         // URL картинки здания
){
    val difficultyText: String
        get() = when (difficulty) {
            1 -> "Easy"
            2 -> "Medium"
            3 -> "Hard"
            else -> "Unknown"
        }
}
