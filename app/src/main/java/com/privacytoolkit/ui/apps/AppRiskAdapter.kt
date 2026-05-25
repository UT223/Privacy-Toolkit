package com.privacytoolkit.ui.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.privacytoolkit.R
import com.privacytoolkit.data.database.AppScanResult
import com.privacytoolkit.databinding.ItemAppRiskBinding

/**
 * RecyclerView adapter that displays installed apps with their risk classification.
 *
 * Uses DiffUtil for efficient updates when the Room LiveData emits new data.
 */
class AppRiskAdapter : ListAdapter<AppScanResult, AppRiskAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemAppRiskBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: AppScanResult) {
            b.tvAppName.text = item.appName
            b.tvPackageName.text = item.packageName
            b.tvPermissions.text = if (item.riskyPermissions.isBlank()) "No sensitive permissions"
            else item.riskyPermissions

            val context = b.root.context
            val (bgColor, textColor) = when (item.riskScore) {
                3 -> Pair(R.color.danger_red_bg, R.color.danger_red)
                2 -> Pair(R.color.caution_amber_bg, R.color.caution_amber)
                1 -> Pair(R.color.safe_green_bg, R.color.safe_green)
                else -> Pair(R.color.neutral_bg, R.color.neutral_text)
            }
            b.chipRisk.setChipBackgroundColorResource(bgColor)
            b.chipRisk.setTextColor(context.getColor(textColor))
            b.chipRisk.text = item.riskLabel

            // Try to load the app icon
            try {
                val pm = context.packageManager
                val icon = pm.getApplicationIcon(item.packageName)
                b.ivAppIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                b.ivAppIcon.setImageResource(R.drawable.ic_app_default)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemAppRiskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppScanResult>() {
            override fun areItemsTheSame(a: AppScanResult, b: AppScanResult) =
                a.packageName == b.packageName
            override fun areContentsTheSame(a: AppScanResult, b: AppScanResult) = a == b
        }
    }
}
