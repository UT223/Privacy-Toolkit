package com.privacytoolkit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.privacytoolkit.data.database.PrivacyDatabase
import com.privacytoolkit.data.database.QRScanHistory
import com.privacytoolkit.data.repository.QRAnalysisResult
import com.privacytoolkit.data.repository.QRRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the QR Code Scanner module.
 */
class QRViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: QRRepository

    val history: LiveData<List<QRScanHistory>>
    val latestScan: LiveData<QRScanHistory?>
    val unsafeCount: LiveData<Int>

    private val _currentResult = MutableLiveData<QRAnalysisResult?>()
    val currentResult: LiveData<QRAnalysisResult?> = _currentResult

    private val _isAnalysing = MutableLiveData(false)
    val isAnalysing: LiveData<Boolean> = _isAnalysing

    init {
        val db = PrivacyDatabase.getInstance(application)
        repo = QRRepository(db.qrScanDao())
        history = repo.history
        latestScan = repo.latest
        unsafeCount = repo.unsafeCount
    }

    /**
     * Analyses the scanned QR content and stores the result.
     */
    fun analyseContent(content: String) {
        viewModelScope.launch {
            _isAnalysing.value = true
            try {
                val result = repo.analyseAndSave(content)
                _currentResult.value = result
            } finally {
                _isAnalysing.value = false
            }
        }
    }

    /** Clear the current result (e.g. when navigating away or scanning again). */
    fun clearResult() {
        _currentResult.value = null
    }
}
