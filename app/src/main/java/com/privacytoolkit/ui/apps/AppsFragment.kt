package com.privacytoolkit.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.privacytoolkit.databinding.FragmentAppsBinding
import com.privacytoolkit.viewmodel.AppViewModel

/**
 * Module 1 – App Permission Analyzer.
 *
 * Lists all non-system installed apps sorted by risk level.
 * Triggers a fresh scan on first load and allows manual re-scan.
 */
class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: AppRiskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppRiskAdapter()
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = adapter

        setupObservers()

        binding.btnRescan.setOnClickListener { viewModel.scanApps() }

        // Auto-scan if no results yet
        if (viewModel.totalScanned.value == null || viewModel.totalScanned.value == 0) {
            viewModel.scanApps()
        }
    }

    private fun setupObservers() {
        viewModel.isScanning.observe(viewLifecycleOwner) { scanning ->
            binding.progressBar.visibility = if (scanning) View.VISIBLE else View.GONE
            binding.btnRescan.isEnabled = !scanning
            binding.tvScanStatus.text = if (scanning) "Scanning installed apps…" else ""
        }

        viewModel.allResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            val count = results?.size ?: 0
            val risky = results?.count { it.riskScore >= 2 } ?: 0
            binding.tvSummary.text = "$count apps scanned — $risky high/medium risk"
            binding.emptyView.visibility = if (count == 0 && viewModel.isScanning.value == false)
                View.VISIBLE else View.GONE
        }

        viewModel.scanError.observe(viewLifecycleOwner) { error ->
            binding.tvScanStatus.text = error ?: ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
