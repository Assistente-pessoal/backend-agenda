package com.agenda.reminder.DTO

import com.agenda.reminder.model.*
import java.time.Instant

data class ReminderResponse(
    val id: String?,
    val title: String,
    val description: String?,
    val remindAt: Instant?,
    val priority: Priority,
    val recurrence: Recurrence,
    val status: ReminderStatus,
    val categoryIds: Set<String>,
    val createdAt: Instant
) {
    companion object {
        fun from(r: Reminder) = ReminderResponse(
            r.id, r.title, r.description, r.remindAt, r.priority,
            r.recurrence, r.status, r.categoryIds, r.createdAt
        )
    }
}