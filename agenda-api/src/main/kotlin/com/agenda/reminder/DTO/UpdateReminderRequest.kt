package com.agenda.reminder.DTO

import com.agenda.reminder.model.Priority
import com.agenda.reminder.model.Recurrence
import com.agenda.reminder.model.Reminder
import jakarta.validation.constraints.Pattern
import java.time.Instant

data class UpdateReminderRequest(
    // nulo = não enviado; se enviado, não pode ser só espaços
    @field:Pattern(regexp = ".*\\S.*", message = "title não pode ser vazio")
    val title: String? = null,
    val description: String? = null,
    val remindAt: Instant? = null,
    val priority: Priority? = null,
    val recurrence: Recurrence? = null,
    val recurUntil: Instant? = null,
    val categoryIds: Set<String>? = null
) {
    // para cada campo: se veio, usa o novo; se não veio (null), mantém o atual
    fun applyTo(current: Reminder) = current.copy(
        title = title ?: current.title,
        description = description ?: current.description,
        remindAt = remindAt ?: current.remindAt,
        priority = priority ?: current.priority,
        recurrence = recurrence ?: current.recurrence,
        recurUntil = recurUntil ?: current.recurUntil,
        categoryIds = categoryIds ?: current.categoryIds
    )
}