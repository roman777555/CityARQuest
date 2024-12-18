package com.cityarquest.data

import com.cityarquest.data.models.Quest

object LocalDataSource {
    private val quests = listOf(
        Quest(
            id = "q1",
            title = "Historic Monument Hunt",
            description = "Find and learn about the old monument near the main square.",
            latitude = 40.7128,
            longitude = -74.0060,
            difficulty = 2,
            isSuperQuest = false,
            imageResId = android.R.drawable.ic_menu_gallery // Временная заглушка
        ),
        Quest(
            id = "q2",
            title = "Coffee Lover's Trail",
            description = "Visit three local coffee shops and find a hidden discount.",
            latitude = 40.7130,
            longitude = -74.0055,
            difficulty = 1,
            isSuperQuest = false,
            imageResId = android.R.drawable.ic_menu_gallery
        ),
        Quest(
            id = "q3",
            title = "Street Art Explorer",
            description = "Discover a hidden piece of street art in a back alley.",
            latitude = 40.7140,
            longitude = -74.0070,
            difficulty = 3,
            isSuperQuest = true,
            imageResId = android.R.drawable.ic_menu_gallery
        )
    )

    fun getQuests(): List<Quest> = quests
}
