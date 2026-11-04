package com.agenda

import com.agenda.category.Category
import com.agenda.category.CategoryRepository
import com.agenda.reminder.model.Reminder
import com.agenda.reminder.repository.ReminderCompletionRepository
import com.agenda.reminder.repository.ReminderRepository
import com.agenda.reminder.service.ReminderService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OwnershipRegressionTests {
    private val reminders = mock(ReminderRepository::class.java)
    private val categories = mock(CategoryRepository::class.java)
    private val completions = mock(ReminderCompletionRepository::class.java)
    private val service = ReminderService(reminders, categories, completions)

    @Test
    fun `criacao sempre usa proprietario autenticado`() {
        `when`(categories.findByNameAndIsDefaultTrue("Geral"))
            .thenReturn(Category(id = "global-category", name = "Geral", isDefault = true))
        `when`(reminders.save(any(Reminder::class.java))).thenAnswer { it.getArgument<Reminder>(0).copy(id = "reminder-1") }

        val saved = service.create(Reminder(title = "Lembrete", ownerId = "other-user"), "current-user")
        assertEquals("current-user", saved.ownerId)
        assertEquals(setOf("global-category"), saved.categoryIds)
    }

    @Test
    fun `categoria privada de outra conta nao pode ser usada na criacao`() {
        `when`(categories.findAllById(setOf("private-category")))
            .thenReturn(listOf(Category(id = "private-category", name = "Privada", userId = "other-user")))
        assertFailsWith<IllegalArgumentException> {
            service.create(Reminder(title = "Lembrete", categoryIds = setOf("private-category")), "current-user")
        }
        verifyNoInteractions(reminders, completions)
    }

    @Test
    fun `outra conta nao pode concluir lembrete pelo ID`() {
        assertFailsWith<NoSuchElementException> {
            service.complete("other-reminder", "current-user")
        }
        verify(reminders).findByIdAndOwnerId("other-reminder", "current-user")
        verifyNoMoreInteractions(reminders)
        verifyNoInteractions(categories, completions)
    }
}
