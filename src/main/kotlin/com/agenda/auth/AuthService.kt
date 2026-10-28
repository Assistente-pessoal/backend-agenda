package com.agenda.auth

import com.agenda.user.dto.UserResponse
import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Locale
import java.nio.charset.StandardCharsets

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val sessions: SessionService,
) {
    // Trabalho de BCrypt equivalente também quando o e-mail não existe (mitiga enumeração por tempo).
    private val dummyHash = encoder.encode("synapse-placeholder-not-a-password")

    fun login(request: LoginRequest): LoginResponse {
        if (request.password.toByteArray(StandardCharsets.UTF_8).size > 72) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.")
        }
        val canonical = request.email.trim().lowercase(Locale.ROOT)
        val user = users.findByEmailNormalized(canonical)
            ?: users.findByEmailIgnoreCase(canonical) // compatibilidade com usuários legados
        val validPassword = encoder.matches(request.password, user?.password ?: dummyHash)
        if (user == null || !validPassword) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.")
        }
        val issued = sessions.issue(user)
        return LoginResponse(
            accessToken = issued.token,
            expiresAt = issued.expiresAt,
            user = user.toPublicResponse(),
        )
    }

    fun currentUser(id: String): UserResponse {
        val user = users.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.") }
        return user.toPublicResponse()
    }
}

private fun User.toPublicResponse(): UserResponse = UserResponse(
    id = requireNotNull(id), name = name, email = email
)
