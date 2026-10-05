package com.agenda.reminder.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("reminder_completions")
data class ReminderCompletion(
    @Id val id: String? = null,
    val reminderId: String,
    // o Mongo apaga o documento 30 dias depois desta data
    @Indexed(expireAfter = "30d")
    val completedAt: Instant = Instant.now()
)