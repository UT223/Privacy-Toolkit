package com.privacytoolkit

import com.privacytoolkit.data.repository.QRRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the QR code safety analysis logic.
 * These run on the JVM — no Android context needed because [QRRepository.analyse]
 * is a pure function with no IO or Android SDK calls.
 */
class QRAnalysisTest {

    // Use a fake DAO — analysis is pure so no DB interaction happens here
    private lateinit var repo: QRRepository

    @Before
    fun setUp() {
        // We only test the pure `analyse()` function; pass null for DAO since it isn't called
        repo = QRRepository(
            object : com.privacytoolkit.data.database.QRScanDao {
                override fun getHistory() = throw UnsupportedOperationException()
                override fun getLatest() = throw UnsupportedOperationException()
                override fun getUnsafeCount() = throw UnsupportedOperationException()
                override suspend fun insert(result: com.privacytoolkit.data.database.QRScanHistory) {}
                override suspend fun clearAll() {}
            }
        )
    }

    @Test
    fun `https url with clean domain is safe`() {
        val result = repo.analyse("https://www.google.com/search?q=android")
        assertTrue(result.isSafe)
        assertEquals("URL", result.contentType)
        assertTrue(result.warningFlags.isEmpty())
    }

    @Test
    fun `http url is flagged as insecure`() {
        val result = repo.analyse("http://example.com/page")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("HTTPS") })
    }

    @Test
    fun `ip address url is flagged`() {
        val result = repo.analyse("http://192.168.1.1/admin")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("IP address") })
    }

    @Test
    fun `url shortener is flagged`() {
        val result = repo.analyse("https://bit.ly/3AbCdEf")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("Short-link") })
    }

    @Test
    fun `phishing keyword in url is flagged`() {
        val result = repo.analyse("https://secure-login.bankupdate.com/verify?user=foo")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("keywords") })
    }

    @Test
    fun `suspicious tld is flagged`() {
        val result = repo.analyse("https://freemoney.tk/win")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("TLD") })
    }

    @Test
    fun `plain text is classified correctly`() {
        val result = repo.analyse("Hello, world!")
        assertEquals("Text", result.contentType)
        assertTrue(result.isSafe)
    }

    @Test
    fun `open wifi qr is flagged`() {
        val result = repo.analyse("WIFI:T:nopass;S:MyOpenNet;;")
        assertFalse(result.isSafe)
        assertTrue(result.warningFlags.any { it.contains("no password") })
    }

    @Test
    fun `wpa wifi qr is safe`() {
        val result = repo.analyse("WIFI:T:WPA;S:MyHome;P:secretpassword;;")
        assertTrue(result.isSafe)
    }
}
