package com.example.simplemessenger

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import com.example.simplemessenger.MainActivity

object ThemeManager {
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_COLORFUL = "colorful"
    
    fun applyTheme(activity: Activity, themeName: String) {
        when (themeName) {
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_COLORFUL -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                // Для цветной темы используем кастомную тему
                if (activity is MainActivity) {
                    activity.setTheme(R.style.Theme_SimpleMessenger_Colorful_WithActionBar)
                } else {
                    activity.setTheme(R.style.Theme_SimpleMessenger_Colorful)
                }
            }
            else -> { // THEME_LIGHT
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
    
    fun getCurrentTheme(context: Context): String {
        val prefs = context.getSharedPreferences("simplemessenger_prefs", Context.MODE_PRIVATE)
        return prefs.getString("app_theme", THEME_LIGHT) ?: THEME_LIGHT
    }
    
    fun saveTheme(context: Context, themeName: String) {
        val prefs = context.getSharedPreferences("simplemessenger_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme", themeName).apply()
    }
} 