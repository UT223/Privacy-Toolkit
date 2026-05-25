package com.privacytoolkit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.privacytoolkit.data.database.AppScanResult
import com.privacytoolkit.data.database.PrivacyDatabase
import com.privacytoolkit.data.repository.AppRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the App Permission Analyzer module.
 * Exposes LiveData for the UI to observe and triggers background scans.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: AppRepository

    // LiveData from Room — auto-updates whenever the table changes
    val allResults: LiveData<List<AppScanResult>>
    val highRiskApps: LiveData<List<AppScanResult>>
    val highRiskCount: LiveData<Int>
    val totalScanned: LiveData<Int>

    // One-shot scan state
    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _scanError = MutableLiveData<String?>(null)
    val scanError: LiveData<String?> = _scanError

    init {
        val db = PrivacyDatabase.getInstance(application)
        repo = AppRepository(application, db.appScanDao())
        allResults = repo.allResults
        highRiskApps = repo.highRiskApps
        highRiskCount = repo.highRiskCount
        totalScanned = repo.totalScanned
    }

    /**
     * Triggers a fresh scan of all installed apps on a background coroutine.
     */
    fun scanApps() {
        if (_isScanning.value == true) return
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            try {
                repo.scanApps()
            } catch (e: Exception) {
                _scanError.value = "Scan failed: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }
}
