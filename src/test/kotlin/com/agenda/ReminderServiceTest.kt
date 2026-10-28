package com.agenda

import com.agenda.reminder.model.Reminder
import com.agenda.reminder.service.ReminderService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
class ReminderServiceTest @Autowired constructor(
    val service: ReminderService
) {
    @Test
    fun `lembrete sem categoria cai em Geral`() {
        val saved = service.create(Reminder(title = "Comprar pão"), "test-user")
        assertEquals(1, saved.categoryIds.size)
    }

    @Test
    fun `categoria inexistente lança erro`() {
        assertFailsWith<IllegalArgumentException> {
            service.create(Reminder(title = "x", categoryIds = setOf("id-que-nao-existe")), "test-user")
        }
    }
}