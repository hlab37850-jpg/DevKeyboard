package com.devboard.keyboard.ime

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.core.Ui
import com.devboard.keyboard.databinding.PanelTranslateBinding
import com.devboard.keyboard.translate.Langs
import com.devboard.keyboard.translate.TranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TranslatePanel(
    private val ctx: Context,
    parent: ViewGroup,
    private val host: PanelHost
) {
    private val b = PanelTranslateBinding.inflate(LayoutInflater.from(ctx), parent, false)
    val view: View get() = b.root

    private val source = StringBuilder()
    private var result: String = ""
    private var job: Job? = null
    private var lastSourceLen = 0

    init {
        paint()
        b.tvSource.movementMethod = ScrollingMovementMethod()
        b.tvResult.movementMethod = ScrollingMovementMethod()

        b.btnFrom.setOnClickListener { cycle(true, true) }
        b.btnFrom.setOnLongClickListener { cycle(true, false); true }
        b.btnTo.setOnClickListener { cycle(false, true) }
        b.btnTo.setOnLongClickListener { cycle(false, false); true }
        b.btnSwap.setOnClickListener {
            val s = Prefs.srcLang
            Prefs.srcLang = Prefs.dstLang
            Prefs.dstLang = s
            if (result.isNotEmpty()) {
                source.setLength(0); source.append(result)
                result = ""
                b.tvResult.text = ""
            }
            paint(); renderSource()
        }
        b.btnCloseTranslate.setOnClickListener { host.closePanel() }
        b.btnGrab.setOnClickListener {
            val t = host.readField()
            if (t.isEmpty()) host.notifyUser("لا يوجد نص في الحقل الحالي")
            else { source.setLength(0); source.append(t); renderSource(); status("تم جلب ${t.length} حرف") }
        }
        b.btnPasteSrc.setOnClickListener {
            val t = host.readClipboard()
            if (t.isEmpty()) host.notifyUser("الحافظة فارغة")
            else { source.append(t); renderSource() }
        }
        b.btnClearSrc.setOnClickListener {
            source.setLength(0); result = ""
            renderSource(); b.tvResult.text = ""; status("")
        }
        b.btnDoTranslate.setOnClickListener { run() }
        b.btnInsertTr.setOnClickListener {
            if (result.isEmpty()) host.notifyUser("لا توجد نتيجة")
            else host.insertText(result)
        }
        b.btnReplaceTr.setOnClickListener {
            if (result.isEmpty()) host.notifyUser("لا توجد نتيجة")
            else if (lastSourceLen <= 0) host.notifyUser("استخدم جلب النص أولاً ثم ترجم")
            else host.replaceLast(lastSourceLen, result)
        }
        b.btnCopyTr.setOnClickListener {
            if (result.isEmpty()) host.notifyUser("لا توجد نتيجة")
            else { host.copyText(result); host.notifyUser("تم نسخ الترجمة") }
        }
        renderSource()
        refreshLangLabels()
    }

    private fun paint() {
        b.root.setBackgroundColor(Ui.panelBg(ctx))
        listOf(b.tvSource, b.tvResult).forEach {
            it.background = Ui.boxBg(ctx)
            it.setTextColor(Ui.textPrimary(ctx))
            it.setHintTextColor(Ui.textMuted(ctx))
        }
        listOf(b.btnFrom, b.btnSwap, b.btnTo, b.btnCloseTranslate, b.btnGrab,
            b.btnPasteSrc, b.btnClearSrc, b.btnReplaceTr, b.btnCopyTr).forEach { style(it, false) }
        listOf(b.btnDoTranslate, b.btnInsertTr).forEach { style(it, true) }
        b.tvTrStatus.setTextColor(Ui.textMuted(ctx))
    }

    private fun style(tv: TextView, accent: Boolean) {
        tv.background = Ui.chipBg(ctx, accent)
        tv.setTextColor(if (accent) 0xFFFFFFFF.toInt() else Ui.textPrimary(ctx))
    }

    private fun cycle(isSource: Boolean, forward: Boolean) {
        if (isSource) Prefs.srcLang = Langs.next(Prefs.srcLang, forward)
        else Prefs.dstLang = Langs.next(Prefs.dstLang, forward)
        refreshLangLabels()
    }

    private fun refreshLangLabels() {
        b.btnFrom.text = Langs.title(Prefs.srcLang)
        b.btnTo.text = Langs.title(Prefs.dstLang)
    }

    private fun renderSource() {
        b.tvSource.text = if (source.isEmpty()) "" else source.toString() + "▏"
    }

    private fun status(s: String) { b.tvTrStatus.text = s }

    /** استقبال المفاتيح من اللوحة بدل حقل التطبيق */
    fun typeText(t: String) { source.append(t); renderSource() }

    fun backspace() {
        if (source.isNotEmpty()) { source.deleteCharAt(source.length - 1); renderSource() }
    }

    fun loadText(t: String) {
        source.setLength(0); source.append(t); renderSource()
    }

    fun isBusy() = job?.isActive == true

    fun cancel() { job?.cancel() }

    private fun run() {
        val text = source.toString().trim()
        if (text.isEmpty()) { host.notifyUser("اكتب نصاً أو استخدم جلب النص"); return }
        var from = Prefs.srcLang
        val to = Prefs.dstLang
        if (from == to) {
            from = Langs.detect(text)
            if (from == to) { host.notifyUser("اختر لغتين مختلفتين"); return }
        }
        lastSourceLen = text.length
        status(ctx.getString(com.devboard.keyboard.R.string.tr_working))
        b.btnDoTranslate.isEnabled = false
        job = host.uiScope().launch {
            val res = TranslationRepository.translate(text, from, to)
            b.btnDoTranslate.isEnabled = true
            if (res.ok) {
                result = res.text
                b.tvResult.text = res.text
                status("✔ " + res.note)
            } else {
                result = ""
                b.tvResult.text = ""
                status("✖ " + res.note)
            }
        }
    }
}
