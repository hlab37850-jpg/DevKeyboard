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

    fun init(c: Context) {
        if (::file.isInitialized) return
        file = File(c.applicationContext.filesDir, "snippets.json")
        load()
        if (items.isEmpty() && !file.exists()) seed()
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

    private fun seed() {
        val eof = "EOF"
        items.addAll(
            listOf(
                Snippet(
                    UUID.randomUUID().toString(), "heredoc: إنشاء ملف",
                    "cat > %% <<'$eof'\n\n$eof\n"
                ),
                Snippet(UUID.randomUUID().toString(), "mkdir -p", "mkdir -p %%\n"),
                Snippet(UUID.randomUUID().toString(), "Termux: تحديث وحزم", "pkg update -y && pkg upgrade -y\n"),
                Snippet(UUID.randomUUID().toString(), "Termux: git و gh", "pkg install -y git gh\n"),
                Snippet(UUID.randomUUID().toString(), "git: تهيئة", "git init\ngit branch -M main\n"),
                Snippet(UUID.randomUUID().toString(), "git: رفع تعديل", "git add -A\ngit commit -m \"%%\"\ngit push\n"),
                Snippet(UUID.randomUUID().toString(), "git: ربط مستودع", "git remote add origin %%\n"),
                Snippet(UUID.randomUUID().toString(), "git: حالة وسجل", "git status\ngit log --oneline -n 10\n"),
                Snippet(UUID.randomUUID().toString(), "find: بحث في الملفات", "grep -rn \"%%\" .\n"),
                Snippet(UUID.randomUUID().toString(), "cd إلى المشروع", "cd ~/projects/%%\n")
            )
        )
        persist()
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
        seed()
    }
}
