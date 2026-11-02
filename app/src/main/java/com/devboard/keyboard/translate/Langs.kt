package com.devboard.keyboard.translate

data class Lang(val code: String, val title: String)

object Langs {
    val list = listOf(
        Lang("ar", "العربية"),
        Lang("en", "English"),
        Lang("fr", "Français"),
        Lang("es", "Español"),
        Lang("de", "Deutsch"),
        Lang("tr", "Türkçe"),
        Lang("ru", "Русский"),
        Lang("fa", "فارسی"),
        Lang("ur", "اردو"),
        Lang("hi", "हिन्दी"),
        Lang("id", "Indonesia"),
        Lang("pt", "Português"),
        Lang("it", "Italiano"),
        Lang("zh", "中文"),
        Lang("ja", "日本語")
    )

    fun title(code: String): String = list.firstOrNull { it.code == code }?.title ?: code

    fun next(code: String, forward: Boolean): String {
        val i = list.indexOfFirst { it.code == code }.let { if (it < 0) 0 else it }
        val n = if (forward) (i + 1) % list.size else (i - 1 + list.size) % list.size
        return list[n].code
    }

    /** كشف بسيط وشفاف: وجود حرف عربي واحد يعني أن المصدر عربي. */
    fun detect(text: String): String {
        val arabic = text.any { it in '\u0600'..'\u06FF' }
        return if (arabic) "ar" else "en"
    }
}

data class TrResult(val ok: Boolean, val text: String, val note: String)
