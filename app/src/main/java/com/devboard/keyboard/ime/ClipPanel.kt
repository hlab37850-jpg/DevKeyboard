package com.devboard.keyboard.ime

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.devboard.keyboard.R
import com.devboard.keyboard.core.Ui
import com.devboard.keyboard.data.ClipItem
import com.devboard.keyboard.data.ClipboardStore
import com.devboard.keyboard.databinding.PanelClipBinding

class ClipPanel(
    private val ctx: Context,
    parent: ViewGroup,
    private val host: PanelHost,
    private val onRefreshSystem: () -> Unit
) {
    private val b = PanelClipBinding.inflate(LayoutInflater.from(ctx), parent, false)
    val view: View get() = b.root
    private val adapter = Adapter()

    init {
        b.root.setBackgroundColor(Ui.panelBg(ctx))
        listOf(b.tvClipTitle, b.btnRefreshClip, b.btnClearClip, b.btnCloseClip).forEach {
            it.background = Ui.chipBg(ctx, false)
            it.setTextColor(Ui.textPrimary(ctx))
        }
        b.tvClipEmpty.setTextColor(Ui.textMuted(ctx))
        b.lvClip.adapter = adapter
        b.lvClip.setOnItemClickListener { _, _, p, _ ->
            host.insertText(adapter.data[p].text)
        }
        b.lvClip.setOnItemLongClickListener { _, _, p, _ ->
            val item = adapter.data[p]
            if (item.pinned) ClipboardStore.delete(item.text)
            else ClipboardStore.togglePin(item.text)
            host.notifyUser(if (item.pinned) "تم الحذف" else "تم التثبيت")
            refresh()
            true
        }
        b.btnCloseClip.setOnClickListener { host.closePanel() }
        b.btnRefreshClip.setOnClickListener { onRefreshSystem(); refresh() }
        b.btnClearClip.setOnClickListener { ClipboardStore.clearUnpinned(); refresh() }
        refresh()
    }

    fun refresh() {
        adapter.data = ClipboardStore.all()
        adapter.notifyDataSetChanged()
        b.tvClipEmpty.visibility = if (adapter.data.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class Adapter : BaseAdapter() {
        var data: List<ClipItem> = emptyList()
        override fun getCount() = data.size
        override fun getItem(p: Int) = data[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
            val v = cv ?: LayoutInflater.from(ctx).inflate(R.layout.item_clip, parent, false)
            val it = data[p]
            val tv = v.findViewById<TextView>(R.id.tvClipText)
            val pin = v.findViewById<TextView>(R.id.tvPin)
            tv.text = it.text.replace("\n", " ⏎ ")
            tv.setTextColor(Ui.textPrimary(ctx))
            pin.text = if (it.pinned) "★" else "☆"
            pin.setTextColor(if (it.pinned) 0xFFFFC107.toInt() else Ui.textMuted(ctx))
            v.background = Ui.boxBg(ctx)
            return v
        }
    }
}
