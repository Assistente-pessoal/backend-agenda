package com.agenda

import com.agenda.auth.AuthService
import com.agenda.auth.IssuedSession
import com.agenda.auth.LoginRequest
import com.agenda.auth.SessionService
import com.agenda.auth.TokenCodec
import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoginRegressionTests {
    private val users = mock(UserRepository::class.java)
    private val sessions = mock(SessionService::class.java)
    private val encoder = BCryptPasswordEncoder()
    private val service = AuthService(users, encoder, sessions)

    @Test
    fun `login canonico valida senha e devolve contrato de sessao`() {
        val user = User(id = "user-1", name = "Pessoa", email = "pessoa@example.com", password = encoder.encode("test-password-123")!!)
        val issued = IssuedSession(TokenCodec.generate(), Instant.now().plusSeconds(3600))
        `when`(users.findByEmailNormalized("pessoa@example.com")).thenReturn(user)
        `when`(sessions.issue(user)).thenReturn(issued)

        val response = service.login(LoginRequest("Pessoa@Example.com", "test-password-123"))
        assertEquals("Bearer", response.tokenType)
        assertTrue(response.accessToken == issued.token)
        assertEquals(issued.expiresAt, response.expiresAt)
        assertEquals("user-1", response.user.id)
        assertEquals("pessoa@example.com", response.user.email)
    }

    @Test
    fun `usuario legado sem email normalizado continua podendo entrar`() {
        val user = User(id = "legacy-user", name = "Pessoa", email = "Pessoa@Example.com", password = encoder.encode("test-password-123")!!)
        `when`(users.findByEmailIgnoreCase("pessoa@example.com")).thenReturn(user)
        `when`(sessions.issue(user)).thenReturn(IssuedSession(TokenCodec.generate(), Instant.now().plusSeconds(3600)))

        assertEquals("legacy-user", service.login(LoginRequest("pessoa@example.com", "test-password-123")).user.id)
    }

    @Test
    fun `senha incorreta e usuario inexistente tem mesma resposta sem criar sessao`() {
        val user = User(id = "user-1", name = "Pessoa", email = "pessoa@example.com", password = encoder.encode("test-password-123")!!)
        `when`(users.findByEmailNormalized("pessoa@example.com")).thenReturn(user)

        val invalidPassword = assertFailsWith<ResponseStatusException> {
            service.login(LoginRequest("pessoa@example.com", "wrong-password-123"))
        }
        val nonexistentUser = assertFailsWith<ResponseStatusException> {
            service.login(LoginRequest("missing@example.com", "wrong-password-123"))
        }
        assertEquals(401, invalidPassword.statusCode.value())
        assertEquals(invalidPassword.statusCode, nonexistentUser.statusCode)
        assertEquals(invalidPassword.reason, nonexistentUser.reason)
        verifyNoInteractions(sessions)
    }

    @Test
    fun `login rejeita senha acima do limite BCrypt sem truncar`() {
        val error = assertFailsWith<ResponseStatusException> {
            service.login(LoginRequest("pessoa@example.com", "á".repeat(37)))
        }
        assertEquals(401, error.statusCode.value())
        verifyNoInteractions(users, sessions)
    }
}
