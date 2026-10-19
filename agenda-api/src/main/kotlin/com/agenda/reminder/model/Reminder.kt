package com.agenda.reminder.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("reminders")
data class Reminder(
    @Id val id: String? = null,
    val title: String,
    val description: String? = null,
    // nulo de propósito: "meu RG está na gaveta" não tem horário
    val remindAt: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: Recurrence = Recurrence.ONCE,
    val recurUntil: Instant? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val source: Source = Source.MANUAL,
    // RN1: pelo menos uma categoria (validamos no service)
    val categoryIds: Set<String> = emptySet(),
    val createdAt: Instant = Instant.now()
)