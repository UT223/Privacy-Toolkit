package com.privacytoolkit.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.privacytoolkit.R
import com.privacytoolkit.databinding.FragmentDashboardBinding
import com.privacytoolkit.viewmodel.AppViewModel
import com.privacytoolkit.viewmodel.NetworkViewModel
import com.privacytoolkit.viewmodel.QRViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard / Home screen.
 *
 * Aggregates summary data from all three ViewModels and displays:
 *  - Overall privacy score card
 *  - Quick-stat cards for Apps, Wi-Fi, and QR modules
 *  - Navigation shortcuts to each module
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val appVm: AppViewModel by activityViewModels()
    private val netVm: NetworkViewModel by activityViewModels()
    private val qrVm: QRViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()

        // Trigger Wi-Fi check automatically when dashboard is opened
        netVm.checkNetwork()
    }

    private fun setupObservers() {
        // Apps summary
        appVm.totalScanned.observe(viewLifecycleOwner) { total ->
            binding.tvAppsTotalValue.text = (total ?: 0).toString()
            updatePrivacyScore()
        }
        appVm.highRiskCount.observe(viewLifecycleOwner) { risky ->
            binding.tvAppsRiskyValue.text = (risky ?: 0).toString()
            updatePrivacyScore()
        }

        // Wi-Fi summary
        netVm.currentNetwork.observe(viewLifecycleOwner) { info ->
            if (info != null) {
                binding.tvWifiSsid.text = info.ssid
                binding.tvWifiStatus.text = info.safetyLabel
                binding.tvWifiStatus.setTextColor(ratingColor(info.safetyRating))
                binding.cardWifi.strokeColor = ratingColor(info.safetyRating)
            } else {
                binding.tvWifiSsid.text = "—"
                binding.tvWifiStatus.text = "Not checked"
            }
            updatePrivacyScore()
        }

        // QR summary
        qrVm.latestScan.observe(viewLifecycleOwner) { scan ->
            if (scan != null) {
                val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                binding.tvQrLastScan.text = fmt.format(Date(scan.scanTimestamp))
                binding.tvQrLastStatus.text = if (scan.isSafe) "Safe" else "Suspicious"
                binding.tvQrLastStatus.setTextColor(
                    if (scan.isSafe) safeColor() else dangerColor()
                )
            } else {
                binding.tvQrLastScan.text = "No scans yet"
                binding.tvQrLastStatus.text = "—"
            }
        }
        qrVm.unsafeCount.observe(viewLifecycleOwner) { count ->
            binding.tvQrUnsafeValue.text = (count ?: 0).toString()
        }
    }

    private fun setupClickListeners() {
        binding.cardApps.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_apps)
        }
        binding.cardWifi.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_wifi)
        }
        binding.cardQr.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_qr)
        }
        binding.btnScanApps.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_apps)
        }
    }

    /**
     * Computes a simple overall privacy score from 0–100 and updates the UI.
     *
     * Score deductions:
     *   -15  per high-risk app (max -60)
     *   -5   per medium-risk app (max -20)
     *   -30  if on an open Wi-Fi network
     *   -10  per unsafe QR scan (max -20)
     */
    private fun updatePrivacyScore() {
        var score = 100

        val total = appVm.totalScanned.value ?: 0
        val risky = appVm.highRiskCount.value ?: 0
        if (total > 0) {
            score -= (risky * 15).coerceAtMost(60)
        }

        val wifiRating = netVm.currentNetwork.value?.safetyRating ?: 0
        if (wifiRating == 2) score -= 30
        else if (wifiRating == 1) score -= 10

        val unsafeQr = qrVm.unsafeCount.value ?: 0
        score -= (unsafeQr * 10).coerceAtMost(20)

        score = score.coerceIn(0, 100)

        binding.tvPrivacyScore.text = score.toString()
        binding.progressScore.progress = score
        binding.tvScoreLabel.text = when {
            score >= 80 -> "Good"
            score >= 60 -> "Fair"
            score >= 40 -> "Needs Attention"
            else -> "High Risk"
        }
        binding.tvScoreLabel.setTextColor(
            when {
                score >= 80 -> safeColor()
                score >= 60 -> cautionColor()
                else -> dangerColor()
            }
        )
    }

    private fun ratingColor(rating: Int) = when (rating) {
        0 -> safeColor()
        1 -> cautionColor()
        else -> dangerColor()
    }

    private fun safeColor() = requireContext().getColor(R.color.safe_green)
    private fun cautionColor() = requireContext().getColor(R.color.caution_amber)
    private fun dangerColor() = requireContext().getColor(R.color.danger_red)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
