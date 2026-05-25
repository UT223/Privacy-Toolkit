package com.privacytoolkit.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a QR code scan result.
 * Records the raw content, inferred type, and a safety assessment.
 */
@Entity(tableName = "qr_scan_history")
data class QRScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val contentType: String,        // "URL", "Text", "Email", "Phone", "WiFi", "Unknown"
    val isSafe: Boolean,
    val safetyNote: String,         // human-readable risk description
    val scanTimestamp: Long = System.currentTimeMillis()
)
