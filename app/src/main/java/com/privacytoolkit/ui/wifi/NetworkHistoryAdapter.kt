package com.privacytoolkit.ui.wifi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.privacytoolkit.R
import com.privacytoolkit.data.database.NetworkHistory
import com.privacytoolkit.databinding.ItemNetworkHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class NetworkHistoryAdapter :
    ListAdapter<NetworkHistory, NetworkHistoryAdapter.ViewHolder>(DIFF) {

    private val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    inner class ViewHolder(private val b: ItemNetworkHistoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(item: NetworkHistory) {
            b.tvHistorySsid.text = item.ssid
            b.tvHistoryType.text = item.securityType
            b.tvHistoryTime.text = fmt.format(Date(item.scanTimestamp))
            b.tvHistorySafety.text = item.safetyLabel
            val ctx = b.root.context
            b.tvHistorySafety.setTextColor(
                ctx.getColor(
                    when (item.safetyRating) {
                        0 -> R.color.safe_green
                        1 -> R.color.caution_amber
                        else -> R.color.danger_red
                    }
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemNetworkHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NetworkHistory>() {
            override fun areItemsTheSame(a: NetworkHistory, b: NetworkHistory) = a.id == b.id
            override fun areContentsTheSame(a: NetworkHistory, b: NetworkHistory) = a == b
        }
    }
}
