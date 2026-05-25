package com.privacytoolkit.ui.wifi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.privacytoolkit.R
import com.privacytoolkit.databinding.FragmentWifiBinding
import com.privacytoolkit.viewmodel.NetworkViewModel

/**
 * Module 2 – Wi-Fi Security Checker.
 *
 * Displays the current network's security classification and a history of
 * previously checked networks from the local Room database.
 */
class WifiFragment : Fragment() {

    private var _binding: FragmentWifiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NetworkViewModel by activityViewModels()
    private lateinit var historyAdapter: NetworkHistoryAdapter

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.checkNetwork()
        else viewModel.checkNetwork() // still check even without location (less detail)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWifiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = NetworkHistoryAdapter()
        binding.rvNetworkHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNetworkHistory.adapter = historyAdapter

        setupObservers()
        binding.btnCheckNetwork.setOnClickListener { requestAndCheck() }

        requestAndCheck()
    }

    private fun requestAndCheck() {
        val hasPerm = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPerm) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            viewModel.checkNetwork()
        }
    }

    private fun setupObservers() {
        viewModel.isChecking.observe(viewLifecycleOwner) { checking ->
            binding.progressBarWifi.visibility = if (checking) View.VISIBLE else View.GONE
            binding.btnCheckNetwork.isEnabled = !checking
        }

        viewModel.currentNetwork.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe

            binding.tvSsid.text = info.ssid
            binding.tvSecurityType.text = info.securityType
            binding.tvSafetyLabel.text = info.safetyLabel
            binding.tvSafetyDetail.text = info.safetyDetail

            val color = ratingColor(info.safetyRating)
            binding.tvSafetyLabel.setTextColor(color)
            binding.cardNetworkStatus.strokeColor = color

            // Safety icon
            val iconRes = when (info.safetyRating) {
                0 -> R.drawable.ic_shield_ok
                1 -> R.drawable.ic_shield_warn
                else -> R.drawable.ic_shield_danger
            }
            binding.ivShieldIcon.setImageResource(iconRes)

            // Extra details
            if (info.isWifi && info.frequency > 0) {
                val band = if (info.frequency > 4000) "5 GHz" else "2.4 GHz"
                binding.tvNetworkDetails.text =
                    "Band: $band  •  Speed: ${info.linkSpeed} Mbps  •  Signal: ${info.signalStrength} dBm"
                binding.tvNetworkDetails.visibility = View.VISIBLE
            } else {
                binding.tvNetworkDetails.visibility = View.GONE
            }
        }

        viewModel.history.observe(viewLifecycleOwner) { history ->
            historyAdapter.submitList(history)
            binding.tvHistoryLabel.visibility =
                if (history.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.checkError.observe(viewLifecycleOwner) { error ->
            // Show error snackbar or status text
            if (!error.isNullOrBlank()) {
                binding.tvSafetyDetail.text = error
            }
        }
    }

    private fun ratingColor(rating: Int) = requireContext().getColor(
        when (rating) {
            0 -> R.color.safe_green
            1 -> R.color.caution_amber
            else -> R.color.danger_red
        }
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
