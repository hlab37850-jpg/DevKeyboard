package com.devboard.keyboard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ClipItem(val text: String, val time: Long, val pinned: Boolean)

object ClipboardStore {
    private const val MAX = 30
    private lateinit var file: File
    private val items = mutableListOf<ClipItem>()

    fun init(c: Context) {
        if (::file.isInitialized) return
        file = File(c.applicationContext.filesDir, "clips.json")
        load()
    }

    private fun load() {
        items.clear()
        runCatching {
            if (!file.exists()) return
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(ClipItem(o.getString("t"), o.getLong("ts"), o.optBoolean("p", false)))
            }
        }
    }

    private fun persist() {
        runCatching {
            val arr = JSONArray()
            items.forEach {
                arr.put(JSONObject().put("t", it.text).put("ts", it.time).put("p", it.pinned))
            }
            file.writeText(arr.toString())
        }
    }

    fun all(): List<ClipItem> =
        items.sortedWith(compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.time })

    fun add(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 8000) return
        val existing = items.firstOrNull { it.text == t }
        if (existing != null) {
            items.remove(existing)
            items.add(0, existing.copy(time = System.currentTimeMillis()))
        } else {
            items.add(0, ClipItem(t, System.currentTimeMillis(), false))
        }
        trim()
        persist()
    }

    private fun trim() {
        while (items.size > MAX) {
            val victim = items.lastOrNull { !it.pinned } ?: items.last()
            items.remove(victim)
        }
    }

    fun togglePin(text: String) {
        val i = items.indexOfFirst { it.text == text }
        if (i >= 0) {
            items[i] = items[i].copy(pinned = !items[i].pinned)
            persist()
        }
    }

    fun delete(text: String) {
        items.removeAll { it.text == text }
        persist()
    }

    fun clearUnpinned() {
        items.removeAll { !it.pinned }
        persist()
    }

    fun clearAll() {
        items.clear()
        persist()
    }
}
