package com.agenda

import com.agenda.user.dto.CreateUserRequest
import com.agenda.user.model.User
import com.agenda.user.repository.UserRepository
import com.agenda.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegistrationRegressionTests {
    private val users = mock(UserRepository::class.java)
    private val encoder = BCryptPasswordEncoder()
    private val service = UserService(users, encoder)

    @Test
    fun `nome curto depois de remover espacos nao e persistido`() {
        val error = assertFailsWith<ResponseStatusException> {
            service.create(CreateUserRequest(name = " a ", email = "test@example.com", password = "test-password-123"))
        }
        assertEquals(400, error.statusCode.value())
        verifyNoInteractions(users)
    }

    @Test
    fun `cadastro normaliza identidade e persiste hash BCrypt`() {
        `when`(users.save(any(User::class.java))).thenAnswer { it.getArgument<User>(0).copy(id = "user-1") }
        val request = CreateUserRequest(name = " Pessoa Teste ", email = "Pessoa@Example.com", password = "test-password-123")
        val response = service.create(request)
        val captured = ArgumentCaptor.forClass(User::class.java)
        verify(users).save(captured.capture())
        val stored = captured.value

        assertEquals("Pessoa Teste", stored.name)
        assertEquals("pessoa@example.com", stored.email)
        assertEquals(stored.email, stored.emailNormalized)
        assertTrue(encoder.matches(request.password, stored.password))
        assertTrue(request.password != stored.password)
        assertEquals("user-1", response.id)
        assertEquals(stored.email, response.email)
    }

    @Test
    fun `limite BCrypt considera bytes UTF8 e nao trunca senha`() {
        val error = assertFailsWith<ResponseStatusException> {
            service.create(CreateUserRequest(name = "Pessoa", email = "test@example.com", password = "á".repeat(37)))
        }
        assertEquals(400, error.statusCode.value())
        verifyNoInteractions(users)
    }

    @Test
    fun `cadastros concorrentes com email igual retornam conflito do indice unico`() {
        `when`(users.save(any(User::class.java))).thenThrow(DuplicateKeyException("private-index-details"))
        val error = assertFailsWith<ResponseStatusException> {
            service.create(CreateUserRequest(name = "Pessoa", email = "test@example.com", password = "test-password-123"))
        }
        assertEquals(409, error.statusCode.value())
        assertTrue(requireNotNull(error.reason).contains("E-mail já cadastrado"))
    }
}
