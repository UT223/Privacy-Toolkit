package com.privacytoolkit.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the result of scanning a single installed app.
 * Stores the app's name, package, detected risky permissions, and a computed risk score.
 */
@Entity(tableName = "app_scan_results")
data class AppScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val packageName: String,
    val riskyPermissions: String,   // comma-separated list
    val riskScore: Int,             // 0 = safe, 1 = low, 2 = medium, 3 = high
    val riskLabel: String,          // "Safe", "Low", "Medium", "High"
    val scanTimestamp: Long = System.currentTimeMillis()
)
