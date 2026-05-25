package com.privacytoolkit.data.repository

import com.privacytoolkit.data.database.QRScanDao
import com.privacytoolkit.data.database.QRScanHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of analysing a scanned QR code content.
 */
data class QRAnalysisResult(
    val content: String,
    val contentType: String,
    val isSafe: Boolean,
    val safetyNote: String,
    val warningFlags: List<String>   // list of specific concerns
)

/**
 * Repository for QR code content analysis and history management.
 *
 * Safety heuristics (all offline, no external lookups):
 *  - URLs not using HTTPS are flagged as insecure.
 *  - URLs with IP-address hosts or unusual TLDs are flagged suspicious.
 *  - Short-link domains (bit.ly, t.co, etc.) are flagged as potentially misleading.
 *  - Known malicious keyword patterns trigger a warning.
 *  - Extremely long URLs are suspicious (common in phishing).
 */
class QRRepository(private val dao: QRScanDao) {

    val history = dao.getHistory()
    val latest = dao.getLatest()
    val unsafeCount = dao.getUnsafeCount()

    // Domains commonly used as URL shorteners (obfuscate final destination)
    private val shortenerDomains = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "buff.ly",
        "short.link", "rb.gy", "is.gd", "cutt.ly", "tiny.cc", "bitly.com"
    )

    // TLDs commonly associated with phishing (not exhaustive, illustrative)
    private val suspiciousTlds = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".pw", ".xyz", ".top", ".club"
    )

    // Keyword fragments that are red flags in URLs / content
    private val suspiciousKeywords = listOf(
        "login", "signin", "verify", "secure", "account", "update", "confirm",
        "password", "credential", "paypal", "banking", "invoice", "prize", "winner"
    )

    /**
     * Analyses QR content, persists the result to Room, and returns an analysis object.
     */
    suspend fun analyseAndSave(rawContent: String): QRAnalysisResult =
        withContext(Dispatchers.IO) {
            val result = analyse(rawContent)

            dao.insert(
                QRScanHistory(
                    content = result.content,
                    contentType = result.contentType,
                    isSafe = result.isSafe,
                    safetyNote = result.safetyNote
                )
            )

            result
        }

    /**
     * Pure analysis function (no IO) — exposed for unit testing.
     */
    fun analyse(rawContent: String): QRAnalysisResult {
        val content = rawContent.trim()
        val warnings = mutableListOf<String>()

        val contentType = detectType(content)

        when (contentType) {
            "URL" -> {
                val lower = content.lowercase()

                // 1. Protocol check
                if (!lower.startsWith("https://")) {
                    warnings.add("Not using HTTPS — data may be visible to others")
                }

                // 2. IP address as host (suspicious)
                val hostRegex = Regex("https?://([^/]+)")
                val host = hostRegex.find(lower)?.groupValues?.get(1) ?: ""
                if (host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}.*"))) {
                    warnings.add("URL uses an IP address instead of a domain name")
                }

                // 3. URL shortener
                val cleanHost = host.removePrefix("www.")
                if (shortenerDomains.any { cleanHost.startsWith(it) }) {
                    warnings.add("Short-link URL — destination is hidden")
                }

                // 4. Suspicious TLD
                if (suspiciousTlds.any { host.endsWith(it) }) {
                    warnings.add("Domain uses a TLD commonly associated with spam")
                }

                // 5. Suspicious keywords in URL
                val keywordsFound = suspiciousKeywords.filter { lower.contains(it) }
                if (keywordsFound.isNotEmpty()) {
                    warnings.add("URL contains phishing-related keywords: ${keywordsFound.take(3).joinToString()}")
                }

                // 6. Unusually long URL
                if (content.length > 300) {
                    warnings.add("Unusually long URL (possible obfuscation)")
                }

                // 7. Multiple redirects hinted by double http
                if (lower.indexOf("http", 8) > 0) {
                    warnings.add("URL appears to contain a nested redirect")
                }
            }

            "Phone" -> {
                // No inherent risk — just inform
            }

            "WiFi" -> {
                val lower = content.lowercase()
                if (lower.contains("nopass") || !lower.contains("p:")) {
                    warnings.add("This Wi-Fi network has no password (open network)")
                }
                if (lower.contains("wep")) {
                    warnings.add("Wi-Fi uses WEP — an outdated, insecure protocol")
                }
            }

            "Email" -> {
                val keywordsFound = suspiciousKeywords.filter { content.lowercase().contains(it) }
                if (keywordsFound.isNotEmpty()) {
                    warnings.add("Content contains sensitive keywords: ${keywordsFound.take(3).joinToString()}")
                }
            }
        }

        val isSafe = warnings.isEmpty()
        val safetyNote = when {
            isSafe -> "No threats detected — content appears safe."
            warnings.size == 1 -> "1 concern found — review before proceeding."
            else -> "${warnings.size} concerns found — proceed with caution."
        }

        return QRAnalysisResult(
            content = content,
            contentType = contentType,
            isSafe = isSafe,
            safetyNote = safetyNote,
            warningFlags = warnings
        )
    }

    private fun detectType(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> "URL"
            lower.startsWith("mailto:") -> "Email"
            lower.startsWith("tel:") || lower.startsWith("smsto:") -> "Phone"
            lower.startsWith("wifi:") -> "WiFi"
            lower.startsWith("begin:vcard") -> "Contact"
            lower.startsWith("begin:vevent") -> "Calendar"
            else -> "Text"
        }
    }
}
