package com.devboard.keyboard.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devboard.keyboard.App
import com.devboard.keyboard.R
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.data.ClipboardStore
import com.devboard.keyboard.data.SnippetStore
import com.devboard.keyboard.databinding.ActivitySettingsBinding
import com.devboard.keyboard.translate.TranslationRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        val themes = resources.getStringArray(R.array.theme_values)
        b.spTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.theme_names))
        b.spTheme.setSelection(themes.indexOf(Prefs.theme).coerceAtLeast(0))

        val provs = resources.getStringArray(R.array.provider_values)
        b.spProvider.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.provider_names))
        b.spProvider.setSelection(provs.indexOf(Prefs.provider).coerceAtLeast(0))

        b.swVibrate.isChecked = Prefs.vibrate
        b.sbVibrate.progress = Prefs.vibrateMs
        b.swSound.isChecked = Prefs.sound
        b.sbKeyHeight.progress = Prefs.keyHeight - 40
        b.sbTextSize.progress = Prefs.textSize - 12
        b.swNumberRow.isChecked = Prefs.numberRow
        b.swDevRow.isChecked = Prefs.devRow
        b.swAutoCap.isChecked = Prefs.autoCap
        b.swDoubleSpace.isChecked = Prefs.doubleSpacePeriod
        b.swCache.isChecked = Prefs.translationCache
        b.swClipboard.isChecked = Prefs.clipboardEnabled
        b.etEmail.setText(Prefs.email)
        b.etLibreUrl.setText(Prefs.libreUrl)
        b.etLibreKey.setText(Prefs.libreKey)

        updateLabels()
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) = updateLabels()
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        b.sbKeyHeight.setOnSeekBarChangeListener(listener)
        b.sbTextSize.setOnSeekBarChangeListener(listener)

        b.btnClearCache.setOnClickListener {
            TranslationRepository.clearCache()
            updateLabels()
            toast("تم مسح ذاكرة الترجمة")
        }
        b.btnClearClip.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("مسح الحافظة")
                .setMessage("سيتم حذف كل العناصر بما فيها المثبتة.")
                .setPositiveButton(R.string.delete) { _, _ ->
                    ClipboardStore.clearAll(); toast("تم المسح")
                }
                .setNegativeButton(R.string.cancel, null).show()
        }
        b.btnResetSnips.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("استعادة الأوامر")
                .setMessage("سيتم استبدال قائمة الأوامر الحالية بالقائمة الافتراضية.")
                .setPositiveButton(R.string.save) { _, _ ->
                    SnippetStore.restoreDefaults(); toast("تمت الاستعادة")
                }
                .setNegativeButton(R.string.cancel, null).show()
        }

        b.btnSave.setOnClickListener { save(themes, provs) }
    }

    private fun updateLabels() {
        b.tvKeyHeight.text = "ارتفاع المفتاح: ${b.sbKeyHeight.progress + 40} dp"
        b.tvTextSize.text = "حجم خط المفاتيح: ${b.sbTextSize.progress + 12} sp"
        b.tvInfo.text = "ترجمات مخزّنة: ${TranslationRepository.cacheSize()} · عناصر الحافظة: ${ClipboardStore.all().size} · الأوامر: ${SnippetStore.all().size}"
    }

    private fun save(themes: Array<String>, provs: Array<String>) {
        Prefs.theme = themes[b.spTheme.selectedItemPosition]
        Prefs.provider = provs[b.spProvider.selectedItemPosition]
        Prefs.vibrate = b.swVibrate.isChecked
        Prefs.vibrateMs = b.sbVibrate.progress.coerceIn(1, 60)
        Prefs.sound = b.swSound.isChecked
        Prefs.keyHeight = b.sbKeyHeight.progress + 40
        Prefs.textSize = b.sbTextSize.progress + 12
        Prefs.numberRow = b.swNumberRow.isChecked
        Prefs.devRow = b.swDevRow.isChecked
        Prefs.autoCap = b.swAutoCap.isChecked
        Prefs.doubleSpacePeriod = b.swDoubleSpace.isChecked
        Prefs.translationCache = b.swCache.isChecked
        Prefs.clipboardEnabled = b.swClipboard.isChecked
        Prefs.email = b.etEmail.text?.toString()?.trim().orEmpty()
        Prefs.libreUrl = b.etLibreUrl.text?.toString()?.trim().orEmpty()
        Prefs.libreKey = b.etLibreKey.text?.toString()?.trim().orEmpty()
        App.applyTheme(Prefs.theme)
        updateLabels()
        toast(getString(R.string.saved))
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
