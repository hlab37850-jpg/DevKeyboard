package com.devboard.keyboard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.devboard.keyboard.core.Prefs
import com.devboard.keyboard.databinding.ActivitySetupBinding
import com.devboard.keyboard.translate.TranslationRepository

class SetupActivity : AppCompatActivity() {

    private lateinit var b: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnEnable.setOnClickListener {
            runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        b.btnPick.setOnClickListener {
            val imm = getSystemService(InputMethodManager::class.java)
            imm?.showInputMethodPicker()
        }
        b.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        b.btnSnippets.setOnClickListener {
            startActivity(Intent(this, SnippetsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun enabled(): Boolean {
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun selected(): Boolean {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return id != null && id.startsWith("$packageName/")
    }

    private fun refresh() {
        val e = enabled()
        val s = selected()
        b.tvStatusEnabled.text = (if (e) "✔ " else "✖ ") +
            getString(if (e) com.devboard.keyboard.R.string.state_enabled else com.devboard.keyboard.R.string.state_disabled)
        b.tvStatusSelected.text = (if (s) "✔ " else "✖ ") +
            getString(if (s) com.devboard.keyboard.R.string.state_selected else com.devboard.keyboard.R.string.state_not_selected)

        val prov = if (Prefs.provider == "libre") "LibreTranslate: " + Prefs.libreUrl else "MyMemory (مجاني بدون مفتاح)"
        b.tvProviderInfo.text = "مزوّد الترجمة الحالي: $prov\nترجمات مخزّنة محلياً: ${TranslationRepository.cacheSize()}"
    }
}
