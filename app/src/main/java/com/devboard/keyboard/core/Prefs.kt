package com.devboard.keyboard.core

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (!::sp.isInitialized) {
            sp = c.applicationContext.getSharedPreferences("devkb_prefs", Context.MODE_PRIVATE)
        }
    }

    private fun put(k: String, v: Any?) {
        val e = sp.edit()
        when (v) {
            is String -> e.putString(k, v)
            is Int -> e.putInt(k, v)
            is Boolean -> e.putBoolean(k, v)
            is Long -> e.putLong(k, v)
        }
        e.apply()
    }

    var theme: String
        get() = sp.getString("theme", "system") ?: "system"
        set(v) = put("theme", v)

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = put("vibrate", v)

    var vibrateMs: Int
        get() = sp.getInt("vibrate_ms", 18)
        set(v) = put("vibrate_ms", v)

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = put("sound", v)

    var keyHeight: Int
        get() = sp.getInt("key_height", 50)
        set(v) = put("key_height", v)

    var textSize: Int
        get() = sp.getInt("text_size", 17)
        set(v) = put("text_size", v)

    var numberRow: Boolean
        get() = sp.getBoolean("number_row", true)
        set(v) = put("number_row", v)

    var devRow: Boolean
        get() = sp.getBoolean("dev_row", true)
        set(v) = put("dev_row", v)

    var clipboardEnabled: Boolean
        get() = sp.getBoolean("clip_enabled", true)
        set(v) = put("clip_enabled", v)

    var translationCache: Boolean
        get() = sp.getBoolean("tr_cache", true)
        set(v) = put("tr_cache", v)

    var lang: String
        get() = sp.getString("lang", "en") ?: "en"
        set(v) = put("lang", v)

    var autoCap: Boolean
        get() = sp.getBoolean("auto_cap", true)
        set(v) = put("auto_cap", v)

    var doubleSpacePeriod: Boolean
        get() = sp.getBoolean("dbl_space", true)
        set(v) = put("dbl_space", v)

    var provider: String
        get() = sp.getString("provider", "mymemory") ?: "mymemory"
        set(v) = put("provider", v)

    var libreUrl: String
        get() = sp.getString("libre_url", "https://libretranslate.com") ?: ""
        set(v) = put("libre_url", v)

    var libreKey: String
        get() = sp.getString("libre_key", "") ?: ""
        set(v) = put("libre_key", v)

    var email: String
        get() = sp.getString("mm_email", "") ?: ""
        set(v) = put("mm_email", v)

    var srcLang: String
        get() = sp.getString("src_lang", "en") ?: "en"
        set(v) = put("src_lang", v)

    var dstLang: String
        get() = sp.getString("dst_lang", "ar") ?: "ar"
        set(v) = put("dst_lang", v)

    fun resetAll() {
        sp.edit().clear().apply()
    }
}
