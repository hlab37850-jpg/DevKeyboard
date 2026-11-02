package com.devboard.keyboard.core

object Layouts {

    private val EN1 = "q w e r t y u i o p".split(" ")
    private val EN2 = "a s d f g h j k l".split(" ")
    private val EN3 = "z x c v b n m".split(" ")

    private val AR1 = "ض ص ث ق ف غ ع ه خ ح ج د".split(" ")
    private val AR2 = "ش س ي ب ل ا ت ن م ك ط".split(" ")
    private val AR3 = "ئ ء ؤ ر لا ى ة و ز ظ".split(" ")

    private val AR_SHIFT = mapOf(
        "ض" to "\u064E", "ص" to "\u064B", "ث" to "\u064F", "ق" to "\u064C",
        "ف" to "لإ", "غ" to "إ", "ع" to "\u2018", "ه" to "\u00F7",
        "خ" to "\u00D7", "ح" to "؛", "ج" to ">", "د" to "<",

        "ش" to "\u0650", "س" to "\u064D", "ي" to "]", "ب" to "[",
        "ل" to "لأ", "ا" to "أ", "ت" to "\u0640", "ن" to "،",
        "م" to "/", "ك" to ":", "ط" to "؟",

        "ئ" to "~", "ء" to "\u0652", "ؤ" to "}", "ر" to "{",
        "لا" to "لآ", "ى" to "آ", "ة" to "\u2019", "و" to ",",
        "ز" to ".", "ظ" to "؟"
    )

    private val AR_ALT = mapOf(
        "ح" to "ج", "خ" to "د", "ك" to "ط", "ز" to "ظ",
        "د" to "ذ", "ج" to "چ", "ط" to "ظ", "ظ" to "ط",
        "ا" to "إ", "و" to "ؤ", "ي" to "ئ", "ه" to "ة",
        "ل" to "لا", "ب" to "پ", "ف" to "ڤ", "ك" to "گ",
        "س" to "ث", "ق" to "ڨ"
    )

    private fun arKey(c: String) = KeyDef.ch(c, AR_SHIFT[c], AR_ALT[c])

    private fun pad(row: List<KeyDef>, side: Float): List<KeyDef> =
        listOf(KeyDef.space(side)) + row + listOf(KeyDef.space(side))

    fun letterRows(lang: String): List<List<KeyDef>> {
        return if (lang == "ar") {
            val r1 = AR1.map { arKey(it) }

            val r2 = ArrayList<KeyDef>()
            r2.add(KeyDef.space(0.5f))
            r2.addAll(AR2.map { arKey(it) })
            r2.add(KeyDef.space(0.5f))

            val r3 = ArrayList<KeyDef>()
            r3.add(KeyDef.act("⇧", "SHIFT", 1f))
            r3.addAll(AR3.map { arKey(it) })
            r3.add(KeyDef.act("⌫", "BACKSPACE", 1f))

            listOf(r1, r2, r3)
        } else {
            val r1 = pad(EN1.map { KeyDef.ch(it) }, 1f)

            val r2 = pad(EN2.map { KeyDef.ch(it) }, 1.5f)

            val r3Core = listOf(KeyDef.act("⇧", "SHIFT", 1.5f)) +
                EN3.map { KeyDef.ch(it) } +
                listOf(KeyDef.act("⌫", "BACKSPACE", 1.5f))

            val r3 = pad(r3Core, 1f)

            listOf(r1, r2, r3)
        }
    }

    fun numberRow(): List<KeyDef> {
        val digits = "1 2 3 4 5 6 7 8 9 0".split(" ")
        val arDigits = mapOf(
            "0" to "٠", "1" to "١", "2" to "٢", "3" to "٣", "4" to "٤",
            "5" to "٥", "6" to "٦", "7" to "٧", "8" to "٨", "9" to "٩"
        )
        return pad(digits.map { KeyDef.ch(it, null, arDigits[it]) }, 1f)
    }

    fun devRow(): List<KeyDef> {
        val core = listOf(
            KeyDef.act("Esc", "ESC"),
            KeyDef.act("Tab", "TAB"),
            KeyDef.act("Ctrl", "CTRL"),
            KeyDef.act("Alt", "ALT"),
            KeyDef.act("←", "ARROW_LEFT"),
            KeyDef.act("→", "ARROW_RIGHT"),
            KeyDef.act("↑", "ARROW_UP"),
            KeyDef.act("↓", "ARROW_DOWN")
        )
        return pad(core, 2f)
    }

    fun symbolRows(page: Int): List<List<KeyDef>> {
        return if (page == 1) {
            val row1 = pad("1 2 3 4 5 6 7 8 9 0".split(" ").map { KeyDef.ch(it) }, 1f)

            val row2Core = listOf("@", "#", "\$", "_", "&", "-", "+", "(", ")", "/")
                .map { KeyDef.ch(it) }
            val row2 = pad(row2Core, 1f)

            val row3Core = listOf(KeyDef.act("2/2", "SYM_PAGE2", 1.4f)) +
                listOf("*", "\"", "'", ":", ";", "!", "?").map { KeyDef.ch(it) } +
                listOf(KeyDef.act("⌫", "BACKSPACE", 1.4f))
            val row3 = pad(row3Core, 1.1f)

            listOf(row1, row2, row3)
        } else {
            val row1Core = listOf("~", "`", "|", "\\", "/", "\$", "%", "^", "{", "}")
                .map { KeyDef.ch(it) }
            val row1 = pad(row1Core, 1f)

            val row2Core = listOf("[", "]", "(", ")", "<", ">", "=", ",", ".", "-")
                .map { KeyDef.ch(it) }
            val row2 = pad(row2Core, 1f)

            val row3Core = listOf(
                KeyDef.act("^C", "CTRL_C"),
                KeyDef.act("^D", "CTRL_D"),
                KeyDef.act("^Z", "CTRL_Z"),
                KeyDef.act("^L", "CTRL_L"),
                KeyDef.act("^A", "CTRL_A"),
                KeyDef.act("^E", "CTRL_E"),
                KeyDef.act("^U", "CTRL_U"),
                KeyDef.act("^K", "CTRL_K"),
                KeyDef.act("⌫", "BACKSPACE")
            )
            val row3 = pad(row3Core, 1.5f)

            val row4Core = listOf(
                KeyDef.act("↶", "UNDO"),
                KeyDef.act("↷", "REDO"),
                KeyDef.act("نسخ", "COPY"),
                KeyDef.act("قص", "CUT"),
                KeyDef.act("لصق", "PASTE"),
                KeyDef.act("كل", "SELECT_ALL"),
                KeyDef.act("Home", "MOVE_HOME"),
                KeyDef.act("End", "MOVE_END"),
                KeyDef.act("Pg↑", "PAGE_UP"),
                KeyDef.act("Pg↓", "PAGE_DOWN")
            )
            val row4 = pad(row4Core, 1f)

            listOf(row1, row2, row3, row4)
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

        val core = listOf(
            left,
            second,
            KeyDef.ch(",", null, "'"),
            KeyDef.act("space", "SPACE", 4f),
            KeyDef.ch(".", null, "…"),
            KeyDef.act("⏎", "ENTER", 1.6f, accent = true)
        )

        return pad(core, 0.85f)
    }

    fun build(lang: String, mode: String, numbers: Boolean, dev: Boolean): List<List<KeyDef>> {
        val rows = ArrayList<List<KeyDef>>()
        if (dev) rows.add(devRow())
        if (numbers && mode == "letters") rows.add(numberRow())
        rows.addAll(
            if (mode == "letters") letterRows(lang)
            else symbolRows(if (mode == "sym1") 1 else 2)
        )
        rows.add(bottomRow(mode, lang))
        return rows
    }
}
