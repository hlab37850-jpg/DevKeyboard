package com.devboard.keyboard.ime

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.devboard.keyboard.R
import com.devboard.keyboard.core.Ui
import com.devboard.keyboard.data.Snippet
import com.devboard.keyboard.data.SnippetStore
import com.devboard.keyboard.databinding.PanelSnippetBinding
import com.devboard.keyboard.ui.SnippetsActivity

class SnippetPanel(
    private val ctx: Context,
    parent: ViewGroup,
    private val host: PanelHost
) {
    private val b = PanelSnippetBinding.inflate(LayoutInflater.from(ctx), parent, false)
    val view: View get() = b.root
    private val adapter = Adapter()

    init {
        b.root.setBackgroundColor(Ui.panelBg(ctx))
        listOf(b.tvSnipTitle, b.btnManageSnip, b.btnCloseSnip).forEach {
            it.background = Ui.chipBg(ctx, false)
            it.setTextColor(Ui.textPrimary(ctx))
        }
        b.tvSnipEmpty.setTextColor(Ui.textMuted(ctx))
        b.lvSnip.adapter = adapter
        b.lvSnip.setOnItemClickListener { _, _, p, _ ->
            val s = adapter.data[p]
            val marker = "%%"
            val idx = s.body.indexOf(marker)
            if (idx >= 0) {
                val clean = s.body.replace(marker, "")
                host.insertText(clean)
                val back = clean.length - idx
                host.notifyUser("تم الإدراج")
                moveCursorBack(back)
            } else {
                host.insertText(s.body)
            }
        }
        b.btnCloseSnip.setOnClickListener { host.closePanel() }
        b.btnManageSnip.setOnClickListener { host.openApp(SnippetsActivity::class.java) }
        refresh()
    }

    private var backPending = 0
    private fun moveCursorBack(n: Int) { backPending = n }
    fun consumeCursorBack(): Int { val n = backPending; backPending = 0; return n }

    fun refresh() {
        adapter.data = SnippetStore.all()
        adapter.notifyDataSetChanged()
        b.tvSnipEmpty.visibility = if (adapter.data.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class Adapter : BaseAdapter() {
        var data: List<Snippet> = emptyList()
        override fun getCount() = data.size
        override fun getItem(p: Int) = data[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val v = cv ?: LayoutInflater.from(ctx).inflate(R.layout.item_snippet, parent, false)
            val it = data[p]
            val n = v.findViewById<TextView>(R.id.tvSnipName)
            val pv = v.findViewById<TextView>(R.id.tvSnipPreview)
            n.text = it.name
            n.setTextColor(Ui.textPrimary(ctx))
            pv.text = it.body.replace("\n", " ⏎ ")
            pv.setTextColor(Ui.textMuted(ctx))
            v.background = Ui.boxBg(ctx)
            return v
        }
    }
}
