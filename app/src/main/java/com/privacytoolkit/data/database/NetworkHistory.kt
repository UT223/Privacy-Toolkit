package com.privacytoolkit.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a recorded Wi-Fi network check.
 * Captures the network name, security type, and computed safety rating.
 */
@Entity(tableName = "network_history")
data class NetworkHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val securityType: String,       // "Open", "WEP", "WPA", "WPA2", "WPA3", "Unknown"
    val isOpen: Boolean,
    val safetyRating: Int,          // 0 = safe, 1 = caution, 2 = danger
    val safetyLabel: String,        // "Safe", "Caution", "Danger"
    val signalStrength: Int = 0,    // dBm
    val scanTimestamp: Long = System.currentTimeMillis()
)
