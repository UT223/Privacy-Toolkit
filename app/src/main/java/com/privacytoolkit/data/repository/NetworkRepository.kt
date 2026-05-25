package com.privacytoolkit.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.privacytoolkit.data.database.NetworkHistory
import com.privacytoolkit.data.database.NetworkHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class summarising the current Wi-Fi connection state for the UI.
 */
data class NetworkInfo(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val ssid: String,
    val securityType: String,
    val isOpen: Boolean,
    val safetyRating: Int,      // 0 = safe, 1 = caution, 2 = danger
    val safetyLabel: String,
    val safetyDetail: String,   // human-readable explanation
    val signalStrength: Int = 0,
    val frequency: Int = 0,
    val linkSpeed: Int = 0
)

/**
 * Repository handling Wi-Fi security inspection.
 *
 * Security classification:
 *   Open network (no password) → Danger  (safetyRating 2)
 *   WEP-encrypted              → Caution (safetyRating 1) — outdated/weak
 *   WPA/WPA2/WPA3              → Safe    (safetyRating 0)
 *   Unknown / no Wi-Fi        → Caution  (safetyRating 1)
 */
class NetworkRepository(
    private val context: Context,
    private val dao: NetworkHistoryDao
) {

    val history = dao.getHistory()
    val latest = dao.getLatest()

    /**
     * Reads the current Wi-Fi state, classifies security, saves to Room, and returns NetworkInfo.
     */
    @Suppress("DEPRECATION")
    suspend fun checkCurrentNetwork(): NetworkInfo = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Check if there is an active network at all
        val activeNetwork = cm.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isConnected = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isConnected) {
            return@withContext NetworkInfo(
                isConnected = false,
                isWifi = false,
                ssid = "Not connected",
                securityType = "None",
                isOpen = false,
                safetyRating = 1,
                safetyLabel = "No Connection",
                safetyDetail = "No active network connection detected."
            )
        }

        if (!isWifi) {
            return@withContext NetworkInfo(
                isConnected = true,
                isWifi = false,
                ssid = "Mobile Data",
                securityType = "Cellular",
                isOpen = false,
                safetyRating = 0,
                safetyLabel = "Safe",
                safetyDetail = "Connected via cellular data — generally secure."
            )
        }

        // Wi-Fi connection details (WifiInfo is deprecated in API 31+ but still accessible
        // via WifiManager for apps that hold ACCESS_WIFI_STATE)
        val wifiInfo = wm.connectionInfo
        val rawSsid = wifiInfo?.ssid ?: "<unknown>"
        val ssid = rawSsid.removePrefix("\"").removeSuffix("\"")

        // Determine security type from ScanResults
        // We scan available networks and find the matching SSID
        val scanResults = try { wm.scanResults } catch (e: Exception) { emptyList() }
        val matchingScan = scanResults.firstOrNull { it.SSID == ssid }

        val scanCapabilities = matchingScan?.capabilities ?: ""
        val (securityType, isOpen, safetyRating, safetyLabel, safetyDetail) =
            classifySecurity(scanCapabilities)

        val signalLevel = wifiInfo?.rssi ?: 0
        val frequency = wifiInfo?.frequency ?: 0
        val linkSpeed = wifiInfo?.linkSpeed ?: 0

        val info = NetworkInfo(
            isConnected = true,
            isWifi = true,
            ssid = if (ssid.isBlank() || ssid == "<unknown>") "Unknown Network" else ssid,
            securityType = securityType,
            isOpen = isOpen,
            safetyRating = safetyRating,
            safetyLabel = safetyLabel,
            safetyDetail = safetyDetail,
            signalStrength = signalLevel,
            frequency = frequency,
            linkSpeed = linkSpeed
        )

        // Persist to history
        dao.insert(
            NetworkHistory(
                ssid = info.ssid,
                securityType = securityType,
                isOpen = isOpen,
                safetyRating = safetyRating,
                safetyLabel = safetyLabel,
                signalStrength = signalLevel
            )
        )

        info
    }

    /**
     * Parses the WPA/WEP/Open tags in the WifiScanResult capabilities string
     * and returns a 5-tuple (type, isOpen, rating, label, detail).
     */
    private fun classifySecurity(capabilities: String): SecurityResult {
        return when {
            capabilities.contains("WPA3") ->
                SecurityResult("WPA3", false, 0, "Secure", "WPA3 encryption — the strongest available standard.")

            capabilities.contains("WPA2") ->
                SecurityResult("WPA2", false, 0, "Secure", "WPA2 encryption — strong and widely trusted.")

            capabilities.contains("WPA") ->
                SecurityResult("WPA", false, 0, "Secure", "WPA encryption — secure, though WPA2/3 is preferred.")

            capabilities.contains("WEP") ->
                SecurityResult(
                    "WEP", false, 1, "Caution",
                    "WEP is an outdated and easily cracked protocol. Avoid sensitive activities."
                )

            capabilities.isBlank() || !capabilities.contains("PSK") && !capabilities.contains("EAP") ->
                SecurityResult(
                    "Open", true, 2, "Danger",
                    "Open network with no encryption — all traffic can be intercepted!"
                )

            else ->
                SecurityResult("Unknown", false, 1, "Caution", "Security type could not be determined.")
        }
    }

    private data class SecurityResult(
        val type: String,
        val isOpen: Boolean,
        val rating: Int,
        val label: String,
        val detail: String
    )
}
