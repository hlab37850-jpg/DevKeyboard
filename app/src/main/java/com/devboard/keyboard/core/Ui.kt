package com.devboard.keyboard.core

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue

object Ui {

    fun dp(c: Context, v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.resources.displayMetrics).toInt()

    private fun systemDark(c: Context): Boolean =
        (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun dark(c: Context): Boolean = when (Prefs.theme) {
        "dark" -> true
        "light" -> false
        else -> systemDark(c)
    }

    fun boardBg(c: Context) = if (dark(c)) 0xFF15171C.toInt() else 0xFFE7EBF0.toInt()
    fun panelBg(c: Context) = if (dark(c)) 0xFF1D2027.toInt() else 0xFFF5F7FA.toInt()
    fun textPrimary(c: Context) = if (dark(c)) 0xFFEDF0F5.toInt() else 0xFF15181D.toInt()
    fun textMuted(c: Context) = if (dark(c)) 0xFF9AA3B2.toInt() else 0xFF5A6472.toInt()
    fun stroke(c: Context) = if (dark(c)) 0xFF323743.toInt() else 0xFFC9D1DB.toInt()

    private const val ACCENT = 0xFF1E88E5.toInt()
    private const val ACCENT_DARK = 0xFF1565C0.toInt()
    private const val ENTER = 0xFF2E7D32.toInt()

    private fun round(color: Int, radius: Float, borderColor: Int? = null): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.RECTANGLE
        g.cornerRadius = radius
        g.setColor(color)
        if (borderColor != null) g.setStroke(2, borderColor)
        return g
    }

    fun keyBg(c: Context, kind: KeyKind): Drawable {
        val d = dark(c)
        val r = dp(c, 7f).toFloat()
        val normal: Int
        val pressed: Int
        val txtBorder = if (d) null else 0xFFD3DAE3.toInt()
        when (kind) {
            KeyKind.CHAR -> {
                normal = if (d) 0xFF2B303A.toInt() else 0xFFFFFFFF.toInt()
                pressed = if (d) 0xFF3D4553.toInt() else 0xFFCFD8E3.toInt()
            }
            KeyKind.ACTION -> {
                normal = if (d) 0xFF20242C.toInt() else 0xFFD7DEE7.toInt()
                pressed = if (d) 0xFF363D49.toInt() else 0xFFB9C4D1.toInt()
            }
            KeyKind.ACCENT -> {
                normal = ACCENT
                pressed = ACCENT_DARK
            }
            KeyKind.SPACER -> {
                normal = Color.TRANSPARENT
                pressed = Color.TRANSPARENT
            }
        }
        val sl = StateListDrawable()
        sl.addState(intArrayOf(android.R.attr.state_pressed), round(pressed, r))
        sl.addState(intArrayOf(), round(normal, r, if (kind == KeyKind.CHAR) txtBorder else null))
        return sl
    }

    fun enterBg(c: Context): Drawable {
        val r = dp(c, 7f).toFloat()
        val sl = StateListDrawable()
        sl.addState(intArrayOf(android.R.attr.state_pressed), round(0xFF1B5E20.toInt(), r))
        sl.addState(intArrayOf(), round(ENTER, r))
        return sl
    }

    fun chipBg(c: Context, accent: Boolean = false): Drawable {
        val r = dp(c, 8f).toFloat()
        val normal = if (accent) ACCENT else if (dark(c)) 0xFF272C35.toInt() else 0xFFFFFFFF.toInt()
        val pressed = if (accent) ACCENT_DARK else if (dark(c)) 0xFF3A414D.toInt() else 0xFFD5DDE7.toInt()
        val sl = StateListDrawable()
        sl.addState(intArrayOf(android.R.attr.state_pressed), round(pressed, r))
        sl.addState(intArrayOf(), round(normal, r, if (accent) null else stroke(c)))
        return sl
    }

    fun boxBg(c: Context): Drawable =
        round(if (dark(c)) 0xFF12141A.toInt() else 0xFFFFFFFF.toInt(), dp(c, 8f).toFloat(), stroke(c))
}
