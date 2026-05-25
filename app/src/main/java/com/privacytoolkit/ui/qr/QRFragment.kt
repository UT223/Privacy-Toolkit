package com.privacytoolkit.ui.qr

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
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.privacytoolkit.R
import com.privacytoolkit.databinding.FragmentQrBinding
import com.privacytoolkit.viewmodel.QRViewModel

/**
 * Module 3 – QR Code Scanner.
 *
 * Integrates ZXing's continuous scanner preview, analyses scanned content
 * for security issues, and displays a history of past scans from Room.
 */
class QRFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QRViewModel by activityViewModels()
    private lateinit var historyAdapter: QRHistoryAdapter
    private var scanningPaused = false

    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScanner()
        else showNoCameraPermission()
    }

    // ZXing continuous barcode callback
    private val barcodeCallback = BarcodeCallback { result ->
        result?.text?.let { content ->
            if (!scanningPaused) {
                scanningPaused = true
                binding.barcodeView.pause()
                viewModel.analyseContent(content)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyAdapter = QRHistoryAdapter()
        binding.rvQrHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQrHistory.adapter = historyAdapter

        // Configure ZXing to read all QR code formats
        binding.barcodeView.decoderFactory = DefaultDecoderFactory(
            listOf(BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX)
        )
        binding.barcodeView.cameraSettings.isAutoFocusEnabled = true

        setupObservers()
        setupClickListeners()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        val hasCam = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCam) startScanner()
        else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startScanner() {
        binding.cameraPermissionLayout.visibility = View.GONE
        binding.scannerLayout.visibility = View.VISIBLE
        binding.barcodeView.decodeContinuous(barcodeCallback)
        binding.barcodeView.resume()
        scanningPaused = false
    }

    private fun showNoCameraPermission() {
        binding.cameraPermissionLayout.visibility = View.VISIBLE
        binding.scannerLayout.visibility = View.GONE
    }

    private fun setupObservers() {
        viewModel.currentResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                binding.resultCard.visibility = View.GONE
                return@observe
            }

            binding.resultCard.visibility = View.VISIBLE
            binding.tvQrContent.text = result.content.take(200) +
                    if (result.content.length > 200) "…" else ""
            binding.tvQrContentType.text = result.contentType
            binding.tvQrSafetyNote.text = result.safetyNote

            val color = if (result.isSafe)
                requireContext().getColor(R.color.safe_green)
            else
                requireContext().getColor(R.color.danger_red)

            binding.tvQrSafetyNote.setTextColor(color)
            binding.ivQrStatus.setImageResource(
                if (result.isSafe) R.drawable.ic_check_circle else R.drawable.ic_warning
            )

            // Warning flags
            if (result.warningFlags.isEmpty()) {
                binding.tvWarningFlags.visibility = View.GONE
            } else {
                binding.tvWarningFlags.visibility = View.VISIBLE
                binding.tvWarningFlags.text = result.warningFlags.joinToString("\n") { "• $it" }
            }
        }

        viewModel.history.observe(viewLifecycleOwner) { history ->
            historyAdapter.submitList(history)
        }
    }

    private fun setupClickListeners() {
        binding.btnScanAgain.setOnClickListener {
            viewModel.clearResult()
            binding.resultCard.visibility = View.GONE
            scanningPaused = false
            binding.barcodeView.resume()
        }

        binding.btnGrantCamera.setOnClickListener {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!scanningPaused &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            binding.barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
