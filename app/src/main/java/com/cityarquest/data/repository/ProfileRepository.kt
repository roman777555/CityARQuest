package com.cityarquest.data.repository

import android.content.Context
import android.content.SharedPreferences

class ProfileRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "Player") ?: "Player"
        set(value) {
            prefs.edit().putString("user_name", value).apply()
        }

    var userPoints: Int
        get() = prefs.getInt("user_points", 0)
        set(value) {
            prefs.edit().putInt("user_points", value).apply()
        }
}
