package com.agenda

import com.agenda.reminder.model.Reminder
import com.agenda.reminder.repository.ReminderRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataMongoTest
class ReminderRepositoryTest @Autowired constructor(
	val repository: ReminderRepository
) {
	@Test
	fun `salva e busca um lembrete`() {
		val saved = repository.save(Reminder(title = "RG na gaveta de baixo"))

		val found = repository.findById(saved.id!!)

		assertTrue(found.isPresent)
		assertEquals("RG na gaveta de baixo", found.get().title)
	}
}