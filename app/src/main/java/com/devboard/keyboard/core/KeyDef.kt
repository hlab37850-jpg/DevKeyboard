package com.devboard.keyboard.core

enum class KeyKind { CHAR, ACTION, ACCENT, SPACER }

data class KeyDef(
    val label: String,
    val code: String,
    val kind: KeyKind = KeyKind.CHAR,
    val weight: Float = 1f,
    val shiftLabel: String? = null,
    val shiftCode: String? = null,
    val alt: String? = null
) {
    companion object {
        fun ch(c: String, shift: String? = null, alt: String? = null) = KeyDef(
            label = c,
            code = c,
            kind = KeyKind.CHAR,
            shiftLabel = shift,
            shiftCode = shift ?: c.uppercase(),
            alt = alt
        )

        fun act(label: String, code: String, w: Float = 1f, accent: Boolean = false) =
            KeyDef(label, code, if (accent) KeyKind.ACCENT else KeyKind.ACTION, w)

        fun space(w: Float) = KeyDef("", "SPACER", KeyKind.SPACER, w)
    }
}
