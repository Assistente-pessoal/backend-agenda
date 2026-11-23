package com.agenda.vox

import com.agenda.category.CategoryRepository
import com.agenda.reminder.model.Priority
import com.agenda.reminder.model.Reminder
import com.agenda.reminder.model.Source
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class VoxReminderMapper(
    private val categoryRepository: CategoryRepository,
    @Value("\${vox.timezone:America/Sao_Paulo}") timezone: String,
) {
    private val zone: ZoneId = ZoneId.of(timezone)

    fun toReminder(text: String, interpreted: VoxInterpretation): Reminder = Reminder(
        title = interpreted.title.trim(),
        description = text,
        remindAt = toInstant(interpreted.date, interpreted.time),
        priority = toPriority(interpreted.priority),
        source = Source.VOICE,
        categoryIds = categoryIdsFor(interpreted.domain),
    )

    private fun toInstant(date: String?, time: String?): Instant? {
        val localDate = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val localTime = time?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        return when {
            localDate != null -> localDate.atTime(localTime ?: DEFAULT_TIME).atZone(zone).toInstant()
            localTime != null -> {
                // Só horário: próxima ocorrência dele (hoje, se ainda não passou; senão amanhã).
                val now = ZonedDateTime.now(zone)
                var candidate = now.toLocalDate().atTime(localTime).atZone(zone)
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
                candidate.toInstant()
            }
            else -> null
        }
    }

    private fun toPriority(value: String): Priority = when (normalize(value)) {
        "alta", "high" -> Priority.HIGH
        "baixa", "low" -> Priority.LOW
        else -> Priority.MEDIUM
    }

    private fun categoryIdsFor(domain: String): Set<String> {
        val categoryName = when (normalize(domain)) {
            "saude" -> "Saúde"
            "estudos", "trabalho" -> "Compromissos"
            else -> return emptySet() // vazio: o ReminderService aplica "Geral"
        }
        val id = categoryRepository.findByNameAndIsDefaultTrue(categoryName)?.id
        return if (id != null) setOf(id) else emptySet()
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .trim()

    private companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    }
}