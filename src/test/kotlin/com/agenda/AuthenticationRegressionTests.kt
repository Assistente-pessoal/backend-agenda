package com.agenda

import com.agenda.auth.AccessSession
import com.agenda.auth.AccessSessionRepository
import com.agenda.auth.AuthController
import com.agenda.auth.AuthService
import com.agenda.auth.BearerTokenFilter
import com.agenda.auth.SessionService
import com.agenda.auth.TokenCodec
import com.agenda.config.ApiExceptionHandler
import com.agenda.user.repository.UserRepository
import com.agenda.user.model.User
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticationRegressionTests {
    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `esquema bearer ignora caixa mas preserva token e revoga no logout`() {
        val token = TokenCodec.generate()
        val sessions = mock(SessionService::class.java)
        `when`(sessions.resolveUserId(token)).thenReturn("user-1")
        val filter = BearerTokenFilter(sessions)
        val controller = AuthController(mock(AuthService::class.java), sessions)

        for (scheme in listOf("Bearer ", "bearer ", "bEaReR   ")) {
            val request = MockHttpServletRequest("POST", "/auth/logout").apply {
                servletPath = "/auth/logout"
                addHeader(HttpHeaders.AUTHORIZATION, scheme + token)
            }
            val response = MockHttpServletResponse()
            var authenticated = false
            filter.doFilter(request, response, FilterChain { _, _ ->
                authenticated = SecurityContextHolder.getContext().authentication?.name == "user-1"
                assertEquals(204, controller.logout(scheme + token).statusCode.value())
            })
            assertTrue(authenticated)
            assertNull(SecurityContextHolder.getContext().authentication)
        }
        verify(sessions, org.mockito.Mockito.times(3)).revoke(token)
    }

    @Test
    fun `headers ambiguos e credenciais malformadas nao consultam sessoes`() {
        val token = TokenCodec.generate()
        val sessions = mock(SessionService::class.java)
        val filter = BearerTokenFilter(sessions)
        val invalidHeaders = listOf(
            listOf("Bearer $token", "Bearer $token"),
            listOf("Bearer $token, Bearer $token"),
            listOf("Basic $token"),
            listOf("Bearer ${token.uppercase()}"),
            listOf("Bearer $token "),
            listOf("Bearer\t$token"),
        )
        for (headers in invalidHeaders) {
            val request = MockHttpServletRequest("GET", "/reminders").apply {
                headers.forEach { addHeader(HttpHeaders.AUTHORIZATION, it) }
            }
            val response = MockHttpServletResponse()
            var reachedController = false
            filter.doFilter(request, response, FilterChain { _, _ -> reachedController = true })
            assertEquals(401, response.status)
            assertFalse(reachedController)
        }
        verifyNoInteractions(sessions)
    }

    @Test
    fun `falha no banco durante autenticacao devolve 503 sem detalhes internos`() {
        val token = TokenCodec.generate()
        val sessions = mock(SessionService::class.java)
        `when`(sessions.resolveUserId(token)).thenThrow(DataAccessResourceFailureException("private-database-details"))
        val request = MockHttpServletRequest("GET", "/reminders").apply {
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }
        val response = MockHttpServletResponse()
        var reachedController = false
        BearerTokenFilter(sessions).doFilter(request, response, FilterChain { _, _ -> reachedController = true })

        assertEquals(503, response.status)
        assertEquals("application/json", response.contentType?.substringBefore(';'))
        assertTrue(response.contentAsString.contains("sessão"))
        assertFalse(response.contentAsString.contains("private-database-details"))
        assertFalse(reachedController)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `falha no banco em operacoes de controller devolve 503 sem detalhes internos`() {
        val result = ApiExceptionHandler().handleDataAccess(DataAccessResourceFailureException("private-database-details"))
        assertEquals(503, result.statusCode.value())
        assertTrue(requireNotNull(result.body).message.contains("indisponível"))
        assertFalse(requireNotNull(result.body).message.contains("private-database-details"))
    }

    @Test
    fun `erro de programacao em acesso a dados nao e ocultado como indisponibilidade`() {
        val token = TokenCodec.generate()
        val sessions = mock(SessionService::class.java)
        `when`(sessions.resolveUserId(token)).thenThrow(InvalidDataAccessApiUsageException("invalid-query"))
        val request = MockHttpServletRequest("GET", "/reminders").apply {
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }
        assertFailsWith<InvalidDataAccessApiUsageException> {
            BearerTokenFilter(sessions).doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> })
        }
    }

    @Test
    fun `sessao emitida guarda apenas hash e deixa de autenticar apos logout`() {
        val sessions = mock(AccessSessionRepository::class.java)
        val users = mock(UserRepository::class.java)
        var persisted: AccessSession? = null
        `when`(sessions.save(org.mockito.ArgumentMatchers.any(AccessSession::class.java))).thenAnswer {
            it.getArgument<AccessSession>(0).copy(id = "session-1").also { session -> persisted = session }
        }
        `when`(sessions.findByTokenHash(org.mockito.ArgumentMatchers.anyString())).thenAnswer {
            persisted?.takeIf { session -> session.tokenHash == it.getArgument<String>(0) }
        }
        org.mockito.Mockito.doAnswer { persisted = null; null }.`when`(sessions).delete(org.mockito.ArgumentMatchers.any(AccessSession::class.java))
        `when`(users.existsById("user-1")).thenReturn(true)
        val service = SessionService(sessions, users, 24)
        val issued = service.issue(User(id = "user-1", name = "Teste", email = "teste@example.com", password = "unused-hash"))

        val stored = requireNotNull(persisted)
        assertTrue(stored.tokenHash == TokenCodec.sha256(issued.token))
        assertFalse(stored.tokenHash == issued.token)
        assertEquals("user-1", service.resolveUserId(issued.token))
        service.revoke(issued.token)
        assertNull(service.resolveUserId(issued.token))
    }
}
