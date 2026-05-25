package com.privacytoolkit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.privacytoolkit.data.database.NetworkHistory
import com.privacytoolkit.data.database.PrivacyDatabase
import com.privacytoolkit.data.repository.NetworkInfo
import com.privacytoolkit.data.repository.NetworkRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Wi-Fi Security Checker module.
 */
class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: NetworkRepository

    val history: LiveData<List<NetworkHistory>>
    val latestRecord: LiveData<NetworkHistory?>

    private val _currentNetwork = MutableLiveData<NetworkInfo?>()
    val currentNetwork: LiveData<NetworkInfo?> = _currentNetwork

    private val _isChecking = MutableLiveData(false)
    val isChecking: LiveData<Boolean> = _isChecking

    private val _checkError = MutableLiveData<String?>(null)
    val checkError: LiveData<String?> = _checkError

    init {
        val db = PrivacyDatabase.getInstance(application)
        repo = NetworkRepository(application, db.networkHistoryDao())
        history = repo.history
        latestRecord = repo.latest
    }

    /**
     * Runs a network security check on a background coroutine and posts the result.
     */
    fun checkNetwork() {
        if (_isChecking.value == true) return
        viewModelScope.launch {
            _isChecking.value = true
            _checkError.value = null
            try {
                val info = repo.checkCurrentNetwork()
                _currentNetwork.value = info
            } catch (e: Exception) {
                _checkError.value = "Check failed: ${e.localizedMessage}"
            } finally {
                _isChecking.value = false
            }
        }
    }
}
