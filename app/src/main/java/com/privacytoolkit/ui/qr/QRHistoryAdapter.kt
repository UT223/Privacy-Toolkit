package com.privacytoolkit.ui.qr

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.privacytoolkit.R
import com.privacytoolkit.data.database.QRScanHistory
import com.privacytoolkit.databinding.ItemQrHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class QRHistoryAdapter : ListAdapter<QRScanHistory, QRHistoryAdapter.ViewHolder>(DIFF) {

    private val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    inner class ViewHolder(private val b: ItemQrHistoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(item: QRScanHistory) {
            b.tvQrHistoryContent.text = item.content.take(80) +
                    if (item.content.length > 80) "…" else ""
            b.tvQrHistoryType.text = item.contentType
            b.tvQrHistoryTime.text = fmt.format(Date(item.scanTimestamp))
            b.tvQrHistorySafety.text = if (item.isSafe) "Safe" else "Suspicious"
            val ctx = b.root.context
            b.tvQrHistorySafety.setTextColor(
                ctx.getColor(if (item.isSafe) R.color.safe_green else R.color.danger_red)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemQrHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<QRScanHistory>() {
            override fun areItemsTheSame(a: QRScanHistory, b: QRScanHistory) = a.id == b.id
            override fun areContentsTheSame(a: QRScanHistory, b: QRScanHistory) = a == b
        }
    }
}
