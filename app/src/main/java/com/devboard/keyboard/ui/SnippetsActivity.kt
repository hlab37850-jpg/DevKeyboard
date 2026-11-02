package com.devboard.keyboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devboard.keyboard.R
import com.devboard.keyboard.data.Snippet
import com.devboard.keyboard.data.SnippetStore
import com.devboard.keyboard.databinding.ActivitySnippetsBinding

class SnippetsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySnippetsBinding
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySnippetsBinding.inflate(layoutInflater)
        setContentView(b.root)
        adapter = Adapter()
        b.lvSnippets.adapter = adapter
        b.lvSnippets.setOnItemClickListener { _, _, pos, _ -> edit(adapter.data[pos]) }
        b.lvSnippets.setOnItemLongClickListener { _, _, pos, _ -> confirmDelete(adapter.data[pos]); true }
        b.fabAdd.setOnClickListener { edit(null) }
        refresh()
    }

    private fun refresh() {
        adapter.data = SnippetStore.all()
        adapter.notifyDataSetChanged()
        b.tvEmpty.visibility = if (adapter.data.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun edit(item: Snippet?) {
        val pad = (resources.displayMetrics.density * 16).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }
        val name = EditText(this).apply {
            hint = "اسم الأمر"
            setText(item?.name ?: "")
        }
        val body = EditText(this).apply {
            hint = "نص الأمر — استخدم %% لموضع المؤشر"
            setText(item?.body ?: "")
            minLines = 4
            setSingleLine(false)
        }
        box.addView(name)
        box.addView(body)

        AlertDialog.Builder(this)
            .setTitle(if (item == null) R.string.add else R.string.edit)
            .setView(box)
            .setPositiveButton(R.string.save) { _, _ ->
                val n = name.text.toString().trim()
                val t = body.text.toString()
                if (n.isEmpty() || t.isEmpty()) {
                    Toast.makeText(this, "الاسم والنص مطلوبان", Toast.LENGTH_SHORT).show()
                } else {
                    if (item == null) SnippetStore.add(n, t) else SnippetStore.update(item.id, n, t)
                    refresh()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(item: Snippet) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(item.name)
            .setPositiveButton(R.string.delete) { _, _ ->
                SnippetStore.delete(item.id); refresh()
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private inner class Adapter : BaseAdapter() {
        var data: List<Snippet> = emptyList()
        override fun getCount() = data.size
        override fun getItem(p: Int) = data[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val v = cv ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_snippet, parent, false)
            val it = data[p]
            v.findViewById<TextView>(R.id.tvSnipName).text = it.name
            v.findViewById<TextView>(R.id.tvSnipPreview).text = it.body.replace("\n", " ⏎ ")
            v.setBackgroundResource(android.R.drawable.list_selector_background)
            return v
        }
    }
}
