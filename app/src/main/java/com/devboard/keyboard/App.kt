package com.devboard.keyboard

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.data.ClipboardStore
import com.devboard.keyboard.data.SnippetStore
import com.devboard.keyboard.translate.TranslationRepository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        SnippetStore.init(this)
        ClipboardStore.init(this)
        TranslationRepository.init(this)
        applyTheme(Prefs.theme)
    }

    companion object {
        fun applyTheme(mode: String) {
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }
}
