package com.agenda.reminder.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("reminders")
data class Reminder(
    @Id val id: String? = null,
    val title: String,
    val description: String? = null,
    val remindAt: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: Recurrence = Recurrence.ONCE,
    val recurUntil: Instant? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val source: Source = Source.MANUAL,
    val categoryIds: Set<String> = emptySet(),
    val createdAt: Instant = Instant.now(),
    // nulo indica registro anterior à autenticação: não expor a usuários novos.
    @Indexed val ownerId: String? = null,
)
