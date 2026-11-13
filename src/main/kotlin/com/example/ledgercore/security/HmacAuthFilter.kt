package com.example.ledgercore.security

import com.example.ledgercore.config.LedgerCoreProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Verifies X-Signature = hex(HMAC-SHA256(secret, "METHOD\nURI\nX-Timestamp\nsha256hex(body)")).
 * Requests older than [MAX_SKEW_MS] are rejected to bound replay windows.
 */
@Component
class HmacAuthFilter(
    private val properties: LedgerCoreProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val signature = request.getHeader(HEADER_SIGNATURE)
        val timestamp = request.getHeader(HEADER_TIMESTAMP)

        if (signature.isNullOrBlank() || timestamp.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature headers")
            return
        }

        val requestTime = timestamp.toLongOrNull()
        if (requestTime == null || abs(Instant.now().toEpochMilli() - requestTime) > MAX_SKEW_MS) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Stale or invalid timestamp")
            return
        }

        val wrapped = CachedBodyRequestWrapper(request)
        val expected = computeSignature(
            secret = properties.hmacSecret,
            method = request.method,
            uri = request.requestURI,
            timestamp = timestamp,
            body = wrapped.bodyBytes,
        )

        val provided = signature.toByteArrayOrNull()
        if (provided == null || !MessageDigest.isEqual(expected, provided)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature")
            return
        }

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            "partner",
            null,
            listOf(SimpleGrantedAuthority("ROLE_PARTNER")),
        )
        filterChain.doFilter(wrapped, response)
    }

    private fun computeSignature(secret: String, method: String, uri: String, timestamp: String, body: ByteArray): ByteArray {
        val bodyDigest = MessageDigest.getInstance("SHA-256").digest(body).toHex()
        val payload = "${method.uppercase()}\n$uri\n$timestamp\n$bodyDigest"
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.toByteArrayOrNull(): ByteArray? {
        if (length % 2 != 0 || !all { it in HEX_CHARS }) return null
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    companion object {
        const val HEADER_SIGNATURE = "X-Signature"
        const val HEADER_TIMESTAMP = "X-Timestamp"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val MAX_SKEW_MS = 5 * 60 * 1000L
        private val HEX_CHARS = "0123456789abcdefABCDEF".toSet()
    }
}
