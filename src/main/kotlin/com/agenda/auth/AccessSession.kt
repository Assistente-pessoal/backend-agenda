package com.agenda.auth

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/** Somente o hash SHA-256 do token é persistido; o token bruto só é devolvido no login. */
@Document("auth_sessions")
data class AccessSession(
    @Id val id: String? = null,
    @Indexed(unique = true) val tokenHash: String,
    @Indexed val userId: String,
    val createdAt: Instant = Instant.now(),
    @Indexed(expireAfter = "0s") val expiresAt: Instant,
)
