package com.cityarquest.data.models

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Int,
    val latitude: Double,
    val longitude: Double,
    val isSuperQuest: Boolean,
    val imageResId: Int // Добавляем идентификатор изображения
)
