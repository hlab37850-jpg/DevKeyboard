package com.devboard.keyboard.core

object Layouts {

    private val EN1 = "q w e r t y u i o p".split(" ")
    private val EN2 = "a s d f g h j k l".split(" ")
    private val EN3 = "z x c v b n m".split(" ")

    private val AR1 = "ض ص ث ق ف غ ع ه خ ح".split(" ")
    private val AR2 = "ش س ي ب ل ا ت ن م ك".split(" ")
    private val AR3 = "ئ ء ؤ ر لا ى ة و ز".split(" ")

    private val AR_SHIFT = mapOf(
        "ض" to "\u064E", "ص" to "\u064B", "ث" to "\u064F", "ق" to "\u064C",
        "ف" to "لإ", "غ" to "إ", "ع" to "\u2018", "ه" to "\u00F7",
        "خ" to "\u00D7", "ح" to "؛",
        "ش" to "\u0650", "س" to "\u064D", "ي" to "]", "ب" to "[",
        "ل" to "لأ", "ا" to "أ", "ت" to "\u0640", "ن" to "،",
        "م" to "/", "ك" to ":",
        "ئ" to "~", "ء" to "\u0652", "ؤ" to "}", "ر" to "{",
        "لا" to "لآ", "ى" to "آ", "ة" to "\u2019", "و" to ",", "ز" to "."
    )

    private val AR_ALT = mapOf(
        "ح" to "ج", "خ" to "د", "ك" to "ط", "ز" to "ظ",
        "ا" to "إ", "و" to "ؤ", "ي" to "ئ", "ه" to "ة",
        "ء" to "ذ", "ل" to "لا", "ب" to "پ", "ف" to "ڤ"
    )

    private fun arKey(c: String) = KeyDef.ch(c, AR_SHIFT[c], AR_ALT[c])

    fun letterRows(lang: String): List<List<KeyDef>> {
        val r1: List<KeyDef>
        val r2: List<KeyDef>
        val r3 = ArrayList<KeyDef>()
        if (lang == "ar") {
            r1 = AR1.map { arKey(it) }
            r2 = AR2.map { arKey(it) }
            r3.add(KeyDef.act("⇧", "SHIFT", 1.4f))
            AR3.forEach { r3.add(arKey(it)) }
            r3.add(KeyDef.act("⌫", "BACKSPACE", 1.4f))
        } else {
            r1 = EN1.map { KeyDef.ch(it) }
            r2 = EN2.map { KeyDef.ch(it) }
            r3.add(KeyDef.act("⇧", "SHIFT", 1.4f))
            EN3.forEach { r3.add(KeyDef.ch(it)) }
            r3.add(KeyDef.act("⌫", "BACKSPACE", 1.4f))
        }
        return listOf(r1, r2, r3)
    }

    fun numberRow(): List<KeyDef> =
        "1 2 3 4 5 6 7 8 9 0".split(" ").map { KeyDef.ch(it) }

    fun devRow(): List<KeyDef> = listOf(
        KeyDef.act("Esc", "ESC"),
        KeyDef.act("Tab", "TAB"),
        KeyDef.act("Ctrl", "CTRL"),
        KeyDef.act("Alt", "ALT"),
        KeyDef.act("←", "ARROW_LEFT"),
        KeyDef.act("→", "ARROW_RIGHT"),
        KeyDef.act("↑", "ARROW_UP"),
        KeyDef.act("↓", "ARROW_DOWN")
    )

    fun symbolRows(page: Int): List<List<KeyDef>> {
        return if (page == 1) {
            listOf(
                "1 2 3 4 5 6 7 8 9 0".split(" ").map { KeyDef.ch(it) },
                listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { KeyDef.ch(it) },
                listOf(KeyDef.act("=\\<", "SYM_PAGE2", 1.4f)) +
                    listOf("*", "\"", "'", ":", ";", "!", "?").map { KeyDef.ch(it) } +
                    listOf(KeyDef.act("⌫", "BACKSPACE", 1.4f))
            )
        } else {
            listOf(
                listOf("~", "`", "|", "\\", "/", "$", "%", "^", "{", "}").map { KeyDef.ch(it) },
                listOf("[", "]", "(", ")", "<", ">", "=", ",", ".", "-").map { KeyDef.ch(it) },
                listOf(
                    KeyDef.act("^C", "CTRL_C"), KeyDef.act("^D", "CTRL_D"),
                    KeyDef.act("^Z", "CTRL_Z"), KeyDef.act("^L", "CTRL_L"),
                    KeyDef.act("^A", "CTRL_A"), KeyDef.act("^E", "CTRL_E"),
                    KeyDef.act("^U", "CTRL_U"), KeyDef.act("^K", "CTRL_K"),
                    KeyDef.act("⌫", "BACKSPACE", 1.2f)
                )
            )
        }
    }

    fun bottomRow(mode: String, lang: String): List<KeyDef> {
        val left = if (mode == "letters")
            KeyDef.act("?123", "SYM_PAGE1", 1.5f)
        else
            KeyDef.act("ABC", "ABC", 1.5f)

        val second = when (mode) {
            "letters" -> KeyDef.act(if (lang == "ar") "EN" else "عر", "LANG", 1.2f)
            "sym1" -> KeyDef.act("2/2", "SYM_PAGE2", 1.2f)
            else -> KeyDef.act("1/2", "SYM_PAGE1", 1.2f)
        }
        return listOf(
            left,
            second,
            KeyDef.ch(",", null, "'"),
            KeyDef.act("space", "SPACE", 4f),
            KeyDef.ch(".", null, "…"),
            KeyDef.act("⏎", "ENTER", 1.6f, accent = true)
        )
    }

    fun build(lang: String, mode: String, numbers: Boolean, dev: Boolean): List<List<KeyDef>> {
        val rows = ArrayList<List<KeyDef>>()
        if (dev) rows.add(devRow())
        if (numbers && mode == "letters") rows.add(numberRow())
        rows.addAll(if (mode == "letters") letterRows(lang) else symbolRows(if (mode == "sym1") 1 else 2))
        rows.add(bottomRow(mode, lang))
        return rows
    }
}
