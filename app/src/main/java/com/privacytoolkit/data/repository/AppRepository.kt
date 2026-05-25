package com.privacytoolkit.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.privacytoolkit.data.database.AppScanDao
import com.privacytoolkit.data.database.AppScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for scanning installed apps and classifying their permission risk.
 *
 * Risk scoring logic:
 *   Score 0 – Safe   : no sensitive permissions
 *   Score 1 – Low    : only low-impact permissions (e.g. INTERNET)
 *   Score 2 – Medium : one medium-risk permission (CAMERA, LOCATION, etc.)
 *   Score 3 – High   : microphone, contacts + internet, or 3+ medium permissions
 */
class AppRepository(
    private val context: Context,
    private val dao: AppScanDao
) {

    // Permissions considered HIGH risk
    private val highRiskPermissions = setOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_PHONE_STATE"
    )

    // Permissions considered MEDIUM risk
    private val mediumRiskPermissions = setOf(
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.USE_BIOMETRIC",
        "android.permission.USE_FINGERPRINT",
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION"
    )

    // Permissions considered LOW risk (but worth noting)
    private val lowRiskPermissions = setOf(
        "android.permission.INTERNET",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.BLUETOOTH",
        "android.permission.NFC"
    )

    // Human-readable short labels for risky permissions
    private val permissionLabels = mapOf(
        "android.permission.RECORD_AUDIO" to "Microphone",
        "android.permission.CAMERA" to "Camera",
        "android.permission.READ_CONTACTS" to "Contacts",
        "android.permission.ACCESS_FINE_LOCATION" to "Precise Location",
        "android.permission.ACCESS_COARSE_LOCATION" to "Approx Location",
        "android.permission.READ_CALL_LOG" to "Call Log",
        "android.permission.PROCESS_OUTGOING_CALLS" to "Outgoing Calls",
        "android.permission.READ_SMS" to "Read SMS",
        "android.permission.RECEIVE_SMS" to "Receive SMS",
        "android.permission.SEND_SMS" to "Send SMS",
        "android.permission.READ_PHONE_STATE" to "Phone State",
        "android.permission.READ_EXTERNAL_STORAGE" to "Read Storage",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Write Storage",
        "android.permission.USE_BIOMETRIC" to "Biometrics",
        "android.permission.BODY_SENSORS" to "Body Sensors",
        "android.permission.ACTIVITY_RECOGNITION" to "Activity",
        "android.permission.INTERNET" to "Internet",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Auto-Start",
        "android.permission.NFC" to "NFC"
    )

    val allResults = dao.getAllResults()
    val highRiskApps = dao.getHighRiskApps()
    val highRiskCount = dao.getHighRiskCount()
    val totalScanned = dao.getTotalScanned()

    /**
     * Scans all non-system third-party apps, scores each, and persists results to Room.
     * Returns the list of results for immediate display.
     */
    suspend fun scanApps(): List<AppScanResult> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.GET_PERMISSIONS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        } else {
            PackageManager.GET_PERMISSIONS
        }

        val packages = try {
            pm.getInstalledPackages(flags)
        } catch (e: Exception) {
            emptyList()
        }

        val results = mutableListOf<AppScanResult>()

        for (pkg in packages) {
            // Skip system apps — focus on user-installed apps
            val isSystem = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) continue

            val permissions = pkg.requestedPermissions ?: continue
            if (permissions.isEmpty()) continue

            val riskyFound = mutableListOf<String>()
            var highCount = 0
            var mediumCount = 0

            for (perm in permissions) {
                when (perm) {
                    in highRiskPermissions -> {
                        riskyFound.add(permissionLabels[perm] ?: perm.substringAfterLast('.'))
                        highCount++
                    }
                    in mediumRiskPermissions -> {
                        riskyFound.add(permissionLabels[perm] ?: perm.substringAfterLast('.'))
                        mediumCount++
                    }
                    in lowRiskPermissions -> {
                        // Only add INTERNET if combined with camera/mic (makes it risky)
                        if (perm == "android.permission.INTERNET" &&
                            permissions.any { it in highRiskPermissions || it in mediumRiskPermissions }) {
                            riskyFound.add("Internet")
                        }
                    }
                }
            }

            // Compute score
            val score = when {
                highCount >= 1 -> 3
                mediumCount >= 3 -> 3
                mediumCount >= 1 -> 2
                riskyFound.isNotEmpty() -> 1
                else -> 0
            }
            val label = when (score) {
                3 -> "High"
                2 -> "Medium"
                1 -> "Low"
                else -> "Safe"
            }

            val appName = try {
                pm.getApplicationLabel(pkg.applicationInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }

            results.add(
                AppScanResult(
                    appName = appName,
                    packageName = pkg.packageName,
                    riskyPermissions = riskyFound.distinct().joinToString(", "),
                    riskScore = score,
                    riskLabel = label
                )
            )
        }

        // Sort by risk descending, then name
        results.sortWith(compareByDescending<AppScanResult> { it.riskScore }.thenBy { it.appName })

        // Persist to Room (replace old data)
        dao.clearAll()
        dao.insertAll(results)

        results
    }
}
