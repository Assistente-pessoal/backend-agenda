package com.agenda.auth

import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class SessionService(
    private val sessions: AccessSessionRepository,
    private val users: UserRepository,
    @Value("\${auth.session-hours:24}") private val sessionHours: Long,
) {
    init {
        require(sessionHours in 1..168) { "AUTH_SESSION_HOURS deve estar entre 1 e 168." }
    }

    fun issue(user: User): IssuedSession {
        val userId = requireNotNull(user.id) { "Usuário não persistido" }
        val token = TokenCodec.generate()
        val expiresAt = Instant.now().plus(sessionHours, ChronoUnit.HOURS)
        sessions.save(AccessSession(tokenHash = TokenCodec.sha256(token), userId = userId, expiresAt = expiresAt))
        return IssuedSession(token = token, expiresAt = expiresAt)
    }

    fun resolveUserId(rawToken: String): String? {
        if (!TokenCodec.isValidFormat(rawToken)) return null
        val session = sessions.findByTokenHash(TokenCodec.sha256(rawToken)) ?: return null
        if (!session.expiresAt.isAfter(Instant.now())) return null
        if (!users.existsById(session.userId)) return null
        return session.userId
    }

    fun revoke(rawToken: String) {
        if (!TokenCodec.isValidFormat(rawToken)) return
        val session = sessions.findByTokenHash(TokenCodec.sha256(rawToken)) ?: return
        sessions.delete(session)
    }
}

data class IssuedSession(val token: String, val expiresAt: Instant)
