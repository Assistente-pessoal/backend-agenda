package com.agenda

import com.agenda.reminder.model.Reminder
import com.agenda.reminder.repository.ReminderRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.junit.jupiter.api.AfterAll
import java.util.UUID
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

	companion object {
		private val databaseName = "agenda_repository_test_${UUID.randomUUID().toString().replace("-", "")}"

		@JvmStatic
		@DynamicPropertySource
		fun mongoProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.mongodb.uri") { "mongodb://localhost:27017/$databaseName" }
		}

		@JvmStatic
		@AfterAll
		fun cleanDatabase(@Autowired template: MongoTemplate) {
			check(template.db.name == databaseName)
			template.db.drop()
		}
	}
}
