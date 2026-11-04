package com.agenda.auth

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat

/** Token opaco de 256 bits; nenhum segredo compartilhado/JWT é necessário no cliente. */
object TokenCodec {
    private val secureRandom = SecureRandom()
    private val tokenFormat = Regex("^syn_[A-Za-z0-9_-]{43}$")
    private val bearerHeader = Regex("^(?i:Bearer) +(.+)$")

    fun generate(): String = ByteArray(32).also(secureRandom::nextBytes).let { bytes ->
        "syn_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isValidFormat(token: String): Boolean = tokenFormat.matches(token)

    /** O esquema HTTP é case-insensitive; o valor do token continua case-sensitive. */
    fun fromAuthorizationHeader(authorization: String): String? =
        bearerHeader.matchEntire(authorization)?.groupValues?.get(1)?.takeIf(::isValidFormat)

    fun sha256(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
    )
}
