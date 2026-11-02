package com.devboard.keyboard.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.TextView
import android.widget.Toast
import com.devboard.keyboard.core.KeyDef
import com.devboard.keyboard.core.KeyKind
import com.devboard.keyboard.core.Layouts
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.core.Ui
import com.devboard.keyboard.data.ClipboardStore
import com.devboard.keyboard.databinding.KeyboardRootBinding
import com.devboard.keyboard.ui.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class DevKeyboardService : InputMethodService(), PanelHost {

    private var b: KeyboardRootBinding? = null
    private lateinit var renderer: KeyRenderer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mode = "letters"
    private var shift = 0
    private var ctrl = false
    private var alt = false
    private var lastShiftTap = 0L
    private var lastSpaceTap = 0L
    private var secure = false

    private enum class Panel { NONE, TRANSLATE, CLIP, SNIPPET }
    private var panel = Panel.NONE
    private var translatePanel: TranslatePanel? = null
    private var clipPanel: ClipPanel? = null
    private var snippetPanel: SnippetPanel? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { pullClipboard() }
    private var clipManager: ClipboardManager? = null

    // ---------------------------------------------------------------- lifecycle

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        clipManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        runCatching { clipManager?.addPrimaryClipChangedListener(clipListener) }
    }

    override fun onCreateInputView(): View {
        val binding = KeyboardRootBinding.inflate(LayoutInflater.from(this))
        b = binding
        renderer = KeyRenderer(this, binding.keysContainer, ::onKey, ::onLongKey)
        binding.btnLang.setOnClickListener { toggleLang() }
        binding.btnTranslate.setOnClickListener { togglePanel(Panel.TRANSLATE) }
        binding.btnClip.setOnClickListener { togglePanel(Panel.CLIP) }
        binding.btnSnip.setOnClickListener { togglePanel(Panel.SNIPPET) }
        binding.btnSettings.setOnClickListener { openApp(SettingsActivity::class.java) }
        binding.btnHide.setOnClickListener { requestHideSelf(0) }
        paint()
        rebuild()
        return binding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        secure = isSecure(info)
        ctrl = false; alt = false
        if (!restarting) {
            shift = if (Prefs.autoCap && !secure && shouldCap(info)) 1 else 0
            mode = "letters"
        }
        if (secure) closePanel()
        pullClipboard()
        updateStatus(info)
        paint()
        rebuild()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        translatePanel?.cancel()
        closePanel()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        runCatching { clipManager?.removePrimaryClipChangedListener(clipListener) }
        if (::renderer.isInitialized) renderer.stopAll()
        scope.cancel()
        b = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        paint(); rebuild()
    }

    // ---------------------------------------------------------------- rendering

    private fun paint() {
        val v = b ?: return
        v.kbRoot.setBackgroundColor(Ui.boardBg(this))
        val chips = listOf(v.btnLang, v.btnTranslate, v.btnClip, v.btnSnip, v.btnSettings, v.btnHide)
        chips.forEach {
            it.background = Ui.chipBg(this, false)
            it.setTextColor(Ui.textPrimary(this))
        }
        v.tvStatus.setTextColor(Ui.textMuted(this))
        v.btnLang.text = if (Prefs.lang == "ar") "عر" else "EN"
    }

    private fun rebuild() {
        val v = b ?: return
        if (panel != Panel.NONE) {
            v.keysContainer.visibility = View.GONE
            return
        }
        v.keysContainer.visibility = View.VISIBLE
        val rows = Layouts.build(Prefs.lang, mode, Prefs.numberRow, Prefs.devRow)
        renderer.render(rows)
        renderer.refresh(shift, ctrl, alt)
    }

    private fun updateStatus(info: EditorInfo?) {
        val v = b ?: return
        if (secure) {
            v.tvStatus.visibility = View.VISIBLE
            v.tvStatus.text = getString(com.devboard.keyboard.R.string.secure_mode)
            v.btnTranslate.alpha = 0.35f
            v.btnClip.alpha = 0.35f
        } else {
            v.tvStatus.visibility = View.GONE
            v.btnTranslate.alpha = 1f
            v.btnClip.alpha = 1f
        }
    }

    private fun isSecure(info: EditorInfo?): Boolean {
        if (info == null) return false
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val varn = info.inputType and InputType.TYPE_MASK_VARIATION
        if (cls == InputType.TYPE_CLASS_TEXT) {
            if (varn == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                varn == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                varn == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            ) return true
        }
        if (cls == InputType.TYPE_CLASS_NUMBER &&
            varn == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) return true
        return false
    }

    private fun shouldCap(info: EditorInfo?): Boolean {
        if (info == null) return false
        val caps = info.inputType and InputType.TYPE_MASK_FLAGS
        return caps and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 ||
            caps and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0
    }

    // ---------------------------------------------------------------- key handling

    private fun onKey(k: KeyDef) {
        feedback()
        if (panel == Panel.TRANSLATE && k.kind == KeyKind.CHAR) {
            val t = if (shift > 0) (k.shiftCode ?: k.code.uppercase()) else k.code
            translatePanel?.typeText(t)
            if (shift == 1) { shift = 0; renderer.refresh(shift, ctrl, alt) }
            return
        }
        if (panel == Panel.TRANSLATE && k.code == "BACKSPACE") {
            translatePanel?.backspace(); return
        }
        if (panel == Panel.TRANSLATE && (k.code == "SPACE" || k.code == "ENTER")) {
            translatePanel?.typeText(if (k.code == "SPACE") " " else "\n"); return
        }

        if (k.kind == KeyKind.CHAR) {
            val t = if (shift > 0) (k.shiftCode ?: k.code.uppercase()) else k.code
            if (ctrl || alt) sendModified(t) else commit(t)
            if (shift == 1) shift = 0
            ctrl = false; alt = false
            renderer.refresh(shift, ctrl, alt)
            return
        }
        action(k.code)
    }

    private fun onLongKey(k: KeyDef): Boolean {
        if (k.alt != null) {
            feedback()
            if (panel == Panel.TRANSLATE) translatePanel?.typeText(k.alt!!)
            else commit(k.alt!!)
            if (shift == 1) { shift = 0; renderer.refresh(shift, ctrl, alt) }
            return true
        }
        if (k.code == "SHIFT") {
            shift = 2; renderer.refresh(shift, ctrl, alt); return true
        }
        if (k.code == "BACKSPACE" && panel != Panel.TRANSLATE) {
            deleteWord(); return true
        }
        if (k.code == "SPACE") {
            switchInputMethodByPicker(); return true
        }
        return false
    }

    private fun action(code: String) {
        when (code) {
            "SHIFT" -> {
                val now = SystemClock.uptimeMillis()
                shift = if (now - lastShiftTap < 320) 2 else if (shift == 0) 1 else 0
                lastShiftTap = now
            }
            "CTRL" -> ctrl = !ctrl
            "ALT" -> alt = !alt
            "BACKSPACE" -> backspace()
            "ENTER" -> enter()
            "SPACE" -> {
                val now = SystemClock.uptimeMillis()
                if (Prefs.doubleSpacePeriod && now - lastSpaceTap < 300 && Prefs.lang != "ar") {
                    val ic = currentInputConnection
                    val before = ic?.getTextBeforeCursor(2, 0)?.toString() ?: ""
                    if (before == " ") { ic?.deleteSurroundingText(1, 0); commit(". ") }
                    else commit(" ")
                } else commit(" ")
                lastSpaceTap = now
            }
            "TAB" -> sendKey(KeyEvent.KEYCODE_TAB)
            "ESC" -> sendKey(KeyEvent.KEYCODE_ESCAPE)
            "ARROW_LEFT" -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT, if (shift > 0) KeyEvent.META_SHIFT_ON else 0)
            "ARROW_RIGHT" -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, if (shift > 0) KeyEvent.META_SHIFT_ON else 0)
            "ARROW_UP" -> sendKey(KeyEvent.KEYCODE_DPAD_UP)
            "ARROW_DOWN" -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
            "CTRL_C" -> sendKey(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON)
            "CTRL_D" -> sendKey(KeyEvent.KEYCODE_D, KeyEvent.META_CTRL_ON)
            "CTRL_Z" -> sendKey(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON)
            "CTRL_L" -> sendKey(KeyEvent.KEYCODE_L, KeyEvent.META_CTRL_ON)
            "CTRL_A" -> sendKey(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
            "CTRL_E" -> sendKey(KeyEvent.KEYCODE_E, KeyEvent.META_CTRL_ON)
            "CTRL_U" -> sendKey(KeyEvent.KEYCODE_U, KeyEvent.META_CTRL_ON)
            "CTRL_K" -> sendKey(KeyEvent.KEYCODE_K, KeyEvent.META_CTRL_ON)
            "LANG" -> toggleLang()
            "SYM_PAGE1" -> mode = "sym1"
            "SYM_PAGE2" -> mode = "sym2"
            "ABC" -> mode = "letters"
        }
        if (::renderer.isInitialized) renderer.refresh(shift, ctrl, alt)
        if (code == "SYM_PAGE1" || code == "SYM_PAGE2" || code == "ABC" || code == "LANG") rebuild()
    }

    private fun toggleLang() {
        Prefs.lang = if (Prefs.lang == "ar") "en" else "ar"
        shift = 0
        b?.btnLang?.text = if (Prefs.lang == "ar") "عر" else "EN"
        rebuild()
    }

    private fun switchInputMethodByPicker() {
        runCatching {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showInputMethodPicker()
        }
    }

    // ---------------------------------------------------------------- input connection

    private fun commit(t: String) {
        currentInputConnection?.commitText(t, 1)
    }

    private fun sendKey(code: Int, meta: Int = 0) {
        val ic = currentInputConnection ?: return
        val t = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0, meta))
    }

    private fun sendModified(text: String) {
        if (text.length == 1) {
            val c = text[0].lowercaseChar()
            val base = when (c) {
                in 'a'..'z' -> KeyEvent.KEYCODE_A + (c - 'a')
                in '0'..'9' -> KeyEvent.KEYCODE_0 + (c - '0')
                else -> -1
            }
            if (base >= 0) {
                var meta = 0
                if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
                if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                sendKey(base, meta)
                return
            }
            if (ctrl) {
                val code = c.code
                if (code in 64..95) { commit((code - 64).toChar().toString()); return }
                if (code in 97..122) { commit((code - 96).toChar().toString()); return }
            }
        }
        commit(text)
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) { ic.commitText("", 1); return }
        sendKey(KeyEvent.KEYCODE_DEL)
    }

    private fun deleteWord() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(40, 0)?.toString()
        if (before.isNullOrEmpty()) { sendKey(KeyEvent.KEYCODE_DEL); return }
        var n = before.length
        while (n > 0 && before[n - 1] == ' ') n--
        while (n > 0 && before[n - 1] != ' ') n--
        val count = before.length - n
        if (count <= 0) sendKey(KeyEvent.KEYCODE_DEL) else ic.deleteSurroundingText(count, 0)
    }

    private fun enter() {
        val info = currentInputEditorInfo
        val multiline = info != null &&
            (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)
        if (multiline) commit("\n") else sendKey(KeyEvent.KEYCODE_ENTER)
    }

    // ---------------------------------------------------------------- panels

    private fun togglePanel(p: Panel) {
        if (secure && (p == Panel.TRANSLATE || p == Panel.CLIP)) {
            notifyUser(getString(com.devboard.keyboard.R.string.secure_mode)); return
        }
        if (panel == p) { closePanel(); return }
        val v = b ?: return
        closePanel()
        panel = p
        v.panelContainer.removeAllViews()
        v.panelContainer.visibility = View.VISIBLE
        v.keysContainer.visibility = View.GONE
        when (p) {
            Panel.TRANSLATE -> {
                val tp = TranslatePanel(this, v.panelContainer, this)
                translatePanel = tp
                v.panelContainer.addView(tp.view)
                val sel = runCatching { currentInputConnection?.getSelectedText(0)?.toString() }.getOrNull()
                if (!sel.isNullOrEmpty()) tp.loadText(sel)
            }
            Panel.CLIP -> {
                pullClipboard()
                val cp = ClipPanel(this, v.panelContainer, this) { pullClipboard() }
                clipPanel = cp
                v.panelContainer.addView(cp.view)
            }
            Panel.SNIPPET -> {
                val sp = SnippetPanel(this, v.panelContainer, this)
                snippetPanel = sp
                v.panelContainer.addView(sp.view)
            }
            else -> {}
        }
    }

    override fun closePanel() {
        val v = b
        translatePanel?.cancel()
        panel = Panel.NONE
        translatePanel = null; clipPanel = null; snippetPanel = null
        v?.panelContainer?.removeAllViews()
        v?.panelContainer?.visibility = View.GONE
        v?.keysContainer?.visibility = View.VISIBLE
        if (v != null && ::renderer.isInitialized) rebuild()
    }

    // ---------------------------------------------------------------- PanelHost

    override fun insertText(t: String) {
        commit(t)
        val back = snippetPanel?.consumeCursorBack() ?: 0
        if (back > 0) {
            val ic = currentInputConnection
            val cur = ic?.getTextBeforeCursor(0, 0)
            if (cur != null) {
                val et = ic.getExtractedText(ExtractedTextRequest(), 0)
                if (et != null) {
                    val pos = (et.startOffset + et.selectionEnd - back).coerceAtLeast(0)
                    ic.setSelection(pos, pos)
                }
            }
        }
    }

    override fun replaceLast(sourceLen: Int, t: String) {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) { ic.commitText(t, 1); return }
        val before = ic.getTextBeforeCursor(sourceLen, 0)?.toString() ?: ""
        ic.deleteSurroundingText(before.length, 0)
        ic.commitText(t, 1)
    }

    override fun copyText(t: String) {
        val cm = clipManager ?: return
        runCatching { cm.setPrimaryClip(ClipData.newPlainText("DevKeyboard", t)) }
        if (!secure && Prefs.clipboardEnabled) ClipboardStore.add(t)
    }

    override fun readField(): String {
        val ic = currentInputConnection ?: return ""
        val sel = runCatching { ic.getSelectedText(0)?.toString() }.getOrNull()
        if (!sel.isNullOrEmpty()) return sel
        val et = runCatching {
            ic.getExtractedText(ExtractedTextRequest().apply { hintMaxChars = 4000; flags = 0 }, 0)
        }.getOrNull()
        if (et != null && et.text != null) {
            val full = et.text.toString()
            return if (full.length <= 2000) full else full.take(2000)
        }
        return ic.getTextBeforeCursor(1000, 0)?.toString().orEmpty()
    }

    override fun readClipboard(): String {
        val cm = clipManager ?: return ""
        return runCatching {
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this).toString() else ""
        }.getOrDefault("")
    }

    private fun pullClipboard() {
        if (secure || !Prefs.clipboardEnabled) return
        val t = readClipboard()
        if (t.isNotBlank()) {
            ClipboardStore.add(t)
            clipPanel?.refresh()
        }
    }

    override fun notifyUser(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun uiScope(): CoroutineScope = scope

    override fun openApp(cls: Class<*>) {
        runCatching {
            startActivity(Intent(this, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            requestHideSelf(0)
        }
    }

    // ---------------------------------------------------------------- feedback

    private fun feedback() {
        if (Prefs.vibrate) {
            runCatching {
                val vib: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }
                val ms = Prefs.vibrateMs.toLong().coerceIn(1L, 60L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") vib?.vibrate(ms)
                }
            }
        }
        if (Prefs.sound) {
            runCatching {
                (getSystemService(AUDIO_SERVICE) as? AudioManager)
                    ?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
            }
        }
    }
}
