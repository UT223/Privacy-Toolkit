package com.privacytoolkit.data.database

import androidx.lifecycle.LiveData
import androidx.room.*

// ─── App Scan DAO ────────────────────────────────────────────────────────────

@Dao
interface AppScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<AppScanResult>)

    @Query("SELECT * FROM app_scan_results ORDER BY scanTimestamp DESC")
    fun getAllResults(): LiveData<List<AppScanResult>>

    @Query("SELECT * FROM app_scan_results WHERE riskScore >= 2 ORDER BY riskScore DESC")
    fun getHighRiskApps(): LiveData<List<AppScanResult>>

    @Query("SELECT COUNT(*) FROM app_scan_results WHERE riskScore >= 2")
    fun getHighRiskCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM app_scan_results")
    fun getTotalScanned(): LiveData<Int>

    @Query("DELETE FROM app_scan_results")
    suspend fun clearAll()
}

// ─── Network History DAO ──────────────────────────────────────────────────────

@Dao
interface NetworkHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: NetworkHistory)

    @Query("SELECT * FROM network_history ORDER BY scanTimestamp DESC LIMIT 50")
    fun getHistory(): LiveData<List<NetworkHistory>>

    @Query("SELECT * FROM network_history ORDER BY scanTimestamp DESC LIMIT 1")
    fun getLatest(): LiveData<NetworkHistory?>

    @Query("DELETE FROM network_history")
    suspend fun clearAll()
}

// ─── QR Scan History DAO ─────────────────────────────────────────────────────

@Dao
interface QRScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: QRScanHistory)

    @Query("SELECT * FROM qr_scan_history ORDER BY scanTimestamp DESC LIMIT 100")
    fun getHistory(): LiveData<List<QRScanHistory>>

    @Query("SELECT * FROM qr_scan_history ORDER BY scanTimestamp DESC LIMIT 1")
    fun getLatest(): LiveData<QRScanHistory?>

    @Query("SELECT COUNT(*) FROM qr_scan_history WHERE isSafe = 0")
    fun getUnsafeCount(): LiveData<Int>

    @Query("DELETE FROM qr_scan_history")
    suspend fun clearAll()
}
