package com.agenda

import com.agenda.auth.AccessSession
import com.agenda.auth.AccessSessionRepository
import com.agenda.auth.SessionService
import com.agenda.auth.TokenCodec
import com.agenda.reminder.DTO.UpdateReminderRequest
import com.agenda.reminder.model.ReminderStatus
import com.agenda.reminder.repository.ReminderCompletionRepository
import com.agenda.reminder.repository.ReminderRepository
import com.agenda.reminder.service.ReminderService
import com.agenda.category.CategoryRepository
import com.agenda.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthSecurityTests {
    @Test
    fun `tokens sao aleatorios e nunca iguais aos seus hashes`() {
        val first = TokenCodec.generate()
        val second = TokenCodec.generate()
        assertNotEquals(first, second)
        assertTrue(TokenCodec.isValidFormat(first))
        assertFalse(TokenCodec.isValidFormat("not-a-token"))
        assertEquals(64, TokenCodec.sha256(first).length)
        assertNotEquals(first, TokenCodec.sha256(first))
        assertEquals(TokenCodec.sha256(first), TokenCodec.sha256(first))
    }

    @Test
    fun `sessao expirada nao autentica mesmo antes da limpeza TTL do banco`() {
        val sessions = mock(AccessSessionRepository::class.java)
        val users = mock(UserRepository::class.java)
        val rawToken = TokenCodec.generate()
        val hash = TokenCodec.sha256(rawToken)
        `when`(sessions.findByTokenHash(hash)).thenReturn(
            AccessSession(tokenHash = hash, userId = "alice", expiresAt = Instant.now().minusSeconds(20))
        )
        assertNull(SessionService(sessions, users, 24).resolveUserId(rawToken))
        verifyNoInteractions(users)
    }

    @Test
    fun `outro usuario nao consegue atualizar lembrete pelo ID`() {
        val reminders = mock(ReminderRepository::class.java)
        val categories = mock(CategoryRepository::class.java)
        val completions = mock(ReminderCompletionRepository::class.java)
        val service = ReminderService(reminders, categories, completions)
        // Sem correspondencia entre ID E ownerId, a busca retorna null (404).
        assertFailsWith<NoSuchElementException> {
            service.update("id-alice", UpdateReminderRequest(title = "Alterado"), "bob")
        }
        verify(reminders).findByIdAndOwnerId("id-alice", "bob")
        verifyNoInteractions(categories, completions)
    }

    @Test
    fun `listagem e filtrada pelo usuario logado`() {
        val reminders = mock(ReminderRepository::class.java)
        val categories = mock(CategoryRepository::class.java)
        val completions = mock(ReminderCompletionRepository::class.java)
        val service = ReminderService(reminders, categories, completions)
        `when`(reminders.findByOwnerIdAndStatus("alice", ReminderStatus.ACTIVE)).thenReturn(emptyList())
        assertEquals(emptyList(), service.list(ReminderStatus.ACTIVE, "alice"))
        verify(reminders).findByOwnerIdAndStatus("alice", ReminderStatus.ACTIVE)
    }
}
