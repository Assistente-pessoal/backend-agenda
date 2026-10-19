package com.agenda.reminder.DTO

import com.agenda.reminder.model.Priority
import com.agenda.reminder.model.Recurrence
import com.agenda.reminder.model.Reminder
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateReminderRequest(
    @field:NotBlank
    val title: String,
    val description: String? = null,
    val remindAt: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val recurrence: Recurrence = Recurrence.ONCE,
    val categoryIds: Set<String> = emptySet()
) {
    fun toEntity() = Reminder(
        title = title,
        description = description,
        remindAt = remindAt,
        priority = priority,
        recurrence = recurrence,
        categoryIds = categoryIds
    )
}