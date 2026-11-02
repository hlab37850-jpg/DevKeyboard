package com.devboard.keyboard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Snippet(val id: String, val name: String, val body: String)

object SnippetStore {
    private lateinit var file: File
    private val items = mutableListOf<Snippet>()

    private val defaults: List<Snippet> by lazy { buildDefaults() }

    fun init(c: Context) {
        if (::file.isInitialized) return
        file = File(c.applicationContext.filesDir, "snippets.json")
        load()
        ensureDefaults()
    }

    private fun load() {
        items.clear()
        runCatching {
            if (!file.exists()) return
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Snippet(o.getString("id"), o.getString("name"), o.getString("body")))
            }
        }
    }

    private fun persist() {
        runCatching {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("id", it.id).put("name", it.name).put("body", it.body))
            }
            file.writeText(arr.toString())
        }
    }

    private fun ensureDefaults() {
        val existing = items.map { it.name }.toSet()
        var changed = false
        for (d in defaults) {
            if (!existing.contains(d.name)) {
                items.add(d)
                changed = true
            }
        }
        if (changed) persist()
    }

    private fun buildDefaults(): List<Snippet> {
        return listOf(
            Snippet(UUID.randomUUID().toString(), "heredoc: إنشاء ملف", "cat > %% <<'EOF'\n\nEOF\n"),
            Snippet(UUID.randomUUID().toString(), "mkdir -p", "mkdir -p %%\n"),
            Snippet(UUID.randomUUID().toString(), "Termux: تحديث وحزم", "pkg update -y && pkg upgrade -y\n"),
            Snippet(UUID.randomUUID().toString(), "Termux: git و gh", "pkg install -y git gh\n"),
            Snippet(UUID.randomUUID().toString(), "Termux: تخزين مشترك", "termux-setup-storage\n"),
            Snippet(UUID.randomUUID().toString(), "git: تهيئة", "git init\ngit branch -M main\n"),
            Snippet(UUID.randomUUID().toString(), "git: رفع تعديل", "git add -A\ngit commit -m \"%%\"\ngit push\n"),
            Snippet(UUID.randomUUID().toString(), "git: ربط مستودع", "git remote add origin %%\n"),
            Snippet(UUID.randomUUID().toString(), "git: حالة وسجل", "git status\ngit log --oneline -n 10\n"),
            Snippet(UUID.randomUUID().toString(), "GitHub: تسجيل دخول", "gh auth login\n"),
            Snippet(UUID.randomUUID().toString(), "GitHub: تشغيل Workflow", "gh workflow run %%\n"),
            Snippet(UUID.randomUUID().toString(), "GitHub: آخر النتائج", "gh run list --limit 5\n"),
            Snippet(UUID.randomUUID().toString(), "GitHub: تنزيل Artifact", "gh run download --name %%\n"),
            Snippet(UUID.randomUUID().toString(), "find: بحث في الملفات", "grep -rn \"%%\" .\n"),
            Snippet(UUID.randomUUID().toString(), "cd إلى المشروع", "cd ~/projects/%%\n"),
            Snippet(UUID.randomUUID().toString(), "تنزيل ملف", "curl -L -o %% الرابط_هنا\n"),
            Snippet(UUID.randomUUID().toString(), "chmod تنفيذ", "chmod +x %%\n"),
            Snippet(UUID.randomUUID().toString(), "Python: بيئة افتراضية", "python -m venv .venv\nsource .venv/bin/activate\n"),
            Snippet(UUID.randomUUID().toString(), "عمليات النظام", "top -n 1\n"),
            Snippet(UUID.randomUUID().toString(), "ملفات كبيرة", "du -sh * | sort -h | tail -n 10\n"),
            Snippet(UUID.randomUUID().toString(), "ضغط مجلد", "tar -czvf backup.tar.gz %%\n"),
            Snippet(UUID.randomUUID().toString(), "فك ضغط", "tar -xzvf %%\n"),
            Snippet(UUID.randomUUID().toString(), "SSH", "ssh user@host\n"),
            Snippet(UUID.randomUUID().toString(), "SCP", "scp %% user@host:~/\n"),
            Snippet(UUID.randomUUID().toString(), "قتل عملية", "pkill -f %%\n"),
            Snippet(UUID.randomUUID().toString(), "logcat", "logcat -d | tail -n 100\n")
        )
    }

    fun all(): List<Snippet> = items.toList()

    fun add(name: String, body: String) {
        items.add(0, Snippet(UUID.randomUUID().toString(), name, body))
        persist()
    }

    fun update(id: String, name: String, body: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) {
            items[i] = Snippet(id, name, body)
            persist()
        }
    }

    fun delete(id: String) {
        items.removeAll { it.id == id }
        persist()
    }

    fun restoreDefaults() {
        items.clear()
        items.addAll(defaults)
        persist()
    }
}
