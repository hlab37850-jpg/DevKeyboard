package com.devboard.keyboard.ime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.devboard.keyboard.core.KeyDef
import com.devboard.keyboard.core.KeyKind
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.core.Ui
import kotlinx.coroutines.CoroutineScope

interface PanelHost {
    fun insertText(t: String)
    fun replaceLast(sourceLen: Int, t: String)
    fun copyText(t: String)
    fun readField(): String
    fun readClipboard(): String
    fun closePanel()
    fun notifyUser(msg: String)
    fun uiScope(): CoroutineScope
    fun openApp(cls: Class<*>)
}

class KeyRenderer(
    private val ctx: Context,
    private val container: LinearLayout,
    private val onClick: (KeyDef) -> Unit,
    private val onLong: (KeyDef) -> Boolean
) {
    private val handler = Handler(Looper.getMainLooper())
    private val cells = ArrayList<Pair<KeyDef, TextView>>()
    private val tasks = ArrayList<Runnable>()

    fun render(rows: List<List<KeyDef>>) {
        stopAll()
        container.removeAllViews()
        cells.clear()
        val screenH = ctx.resources.displayMetrics.heightPixels
        val budget = (screenH * 0.56f).toInt()
        val base = Ui.dp(ctx, Prefs.keyHeight.toFloat())
        val h = minOf(base, budget / rows.size.coerceAtLeast(1))
        val margin = Ui.dp(ctx, 2.5f)

        for (row in rows) {
            val ll = LinearLayout(ctx)
            ll.orientation = LinearLayout.HORIZONTAL
            ll.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h
            )
            for (k in row) {
                val tv = TextView(ctx)
                tv.gravity = Gravity.CENTER
                tv.textSize = Prefs.textSize.toFloat()
                tv.setTextColor(Ui.textPrimary(ctx))
                tv.text = k.label
                tv.maxLines = 1
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, k.weight)
                lp.setMargins(margin, margin, margin, margin)
                tv.layoutParams = lp

                if (k.kind == KeyKind.SPACER) {
                    tv.background = null
                } else {
                    tv.background = if (k.code == "ENTER") Ui.enterBg(ctx) else Ui.keyBg(ctx, k.kind)
                    if (k.code == "ENTER") tv.setTextColor(0xFFFFFFFF.toInt())
                    tv.setOnClickListener { onClick(k) }
                    tv.setOnLongClickListener { onLong(k) }
                    if (k.code == "BACKSPACE" || k.code.startsWith("ARROW_")) attachRepeat(tv, k)
                }
                cells.add(k to tv)
                ll.addView(tv)
            }
            container.addView(ll)
        }
    }

    fun refresh(shift: Int, ctrl: Boolean, alt: Boolean) {
        for ((k, tv) in cells) {
            when {
                k.kind == KeyKind.CHAR ->
                    tv.text = if (shift > 0) (k.shiftLabel ?: k.label.uppercase()) else k.label

                k.code == "SHIFT" -> {
                    tv.text = when (shift) { 2 -> "⇪"; 1 -> "⬆"; else -> "⇧" }
                    tv.background = Ui.keyBg(ctx, if (shift > 0) KeyKind.ACCENT else KeyKind.ACTION)
                    tv.setTextColor(if (shift > 0) 0xFFFFFFFF.toInt() else Ui.textPrimary(ctx))
                }

                k.code == "CTRL" || k.code == "ALT" -> {
                    val on = if (k.code == "CTRL") ctrl else alt
                    tv.background = Ui.keyBg(ctx, if (on) KeyKind.ACCENT else KeyKind.ACTION)
                    tv.setTextColor(if (on) 0xFFFFFFFF.toInt() else Ui.textPrimary(ctx))
                }
            }
        }
    }

    private fun attachRepeat(tv: TextView, k: KeyDef) {
        var repeating = false
        val task = object : Runnable {
            override fun run() {
                repeating = true
                onClick(k)
                handler.postDelayed(this, 55L)
            }
        }
        tasks.add(task)
        tv.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    repeating = false
                    v.isPressed = true
                    handler.postDelayed(task, 380L)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(task)
                    v.isPressed = false
                    if (!repeating && e.actionMasked == MotionEvent.ACTION_UP) onClick(k)
                    true
                }
                else -> false
            }
        }
    }

    fun stopAll() {
        tasks.forEach { handler.removeCallbacks(it) }
        tasks.clear()
    }
}
